package com.ibm.bi.dml.test.integration.functions.mlcontext;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;

import org.apache.spark.SparkContext;
import org.apache.spark.api.java.JavaRDD;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.ibm.bi.dml.api.DMLException;
import com.ibm.bi.dml.api.DMLScript;
import com.ibm.bi.dml.api.MLContext;
import com.ibm.bi.dml.api.MLOutput;
import com.ibm.bi.dml.api.DMLScript.RUNTIME_PLATFORM;
import com.ibm.bi.dml.parser.ParseException;
import com.ibm.bi.dml.runtime.DMLRuntimeException;
import com.ibm.bi.dml.runtime.matrix.data.MatrixValue.CellIndex;
import com.ibm.bi.dml.runtime.util.MapReduceTool;
import com.ibm.bi.dml.test.integration.AutomatedTestBase;
import com.ibm.bi.dml.test.integration.TestConfiguration;
import com.ibm.bi.dml.test.utils.TestUtils;

@RunWith(value = Parameterized.class)
public class GNMFTest extends AutomatedTestBase 
{

	
	private final static String TEST_DIR = "applications/gnmf/";
	private final static String TEST_GNMF = "GNMF";
	
	int numRegisteredInputs;
	int numRegisteredOutputs;
	
	public GNMFTest(int in, int out) {
		numRegisteredInputs = in;
		numRegisteredOutputs = out;
	}
	
	@Parameters
	 public static Collection<Object[]> data() {
	   Object[][] data = new Object[][] { { 0, 0 }, { 3, 2 }, { 2, 2 }, { 2, 1 }, { 2, 0 }, { 3, 0 }};
	   return Arrays.asList(data);
	 }
	 
	@Override
	public void setUp() {
		addTestConfiguration(TEST_GNMF, new TestConfiguration(TEST_DIR, TEST_GNMF, new String[] { "w", "h" }));
	}
	
	@Test
	public void testGNMFWithRDMLAndJava() throws IOException, DMLException, ParseException {
		int m = 2000;
		int n = 1500;
		int k = 50;
		int maxiter = 2;
		
		TestConfiguration config = getTestConfiguration(TEST_GNMF);
		
		/* This is for running the junit test the old way, i.e., replace $$x$$ in DML script with its value */
		config.addVariable("m", m);
		config.addVariable("n", n);
		config.addVariable("k", k);
		config.addVariable("maxiter", maxiter);
		
		double Eps = Math.pow(10, -8);

		/* This is for running the junit test the new way, i.e., construct the arguments directly */
		String GNMF_HOME = SCRIPT_DIR + TEST_DIR;
		fullDMLScriptName = GNMF_HOME + TEST_GNMF + ".dml";
		programArgs = new String[]{			GNMF_HOME + INPUT_DIR + "v", 
											GNMF_HOME + INPUT_DIR + "w", 
                							GNMF_HOME + INPUT_DIR + "h", 
                							Integer.toString(m), Integer.toString(n), Integer.toString(k), Integer.toString(maxiter),
                							GNMF_HOME + OUTPUT_DIR + "w", 
                							GNMF_HOME + OUTPUT_DIR + "h"};
		
		
		
		fullRScriptName = GNMF_HOME + TEST_GNMF + ".R";
		rCmd = "Rscript" + " " + fullRScriptName + " " + 
		       GNMF_HOME + INPUT_DIR + " " + Integer.toString(maxiter) + " " + GNMF_HOME + EXPECTED_DIR;
		
		loadTestConfiguration(config);

		double[][] v = getRandomMatrix(m, n, 1, 5, 0.2, System.currentTimeMillis());
		double[][] w = getRandomMatrix(m, k, 0, 1, 1, System.currentTimeMillis());
		double[][] h = getRandomMatrix(k, n, 0, 1, 1, System.currentTimeMillis());

		writeInputMatrix("v", v, true);
		writeInputMatrix("w", w, true);
		writeInputMatrix("h", h, true);

		for (int i = 0; i < maxiter; i++) {
			double[][] tW = TestUtils.performTranspose(w);
			double[][] tWV = TestUtils.performMatrixMultiplication(tW, v);
			double[][] tWW = TestUtils.performMatrixMultiplication(tW, w);
			double[][] tWWH = TestUtils.performMatrixMultiplication(tWW, h);
			for (int j = 0; j < k; j++) {
				for (int l = 0; l < n; l++) {
					h[j][l] = h[j][l] * (tWV[j][l] / (tWWH[j][l] + Eps));
				}
			}

			double[][] tH = TestUtils.performTranspose(h);
			double[][] vTH = TestUtils.performMatrixMultiplication(v, tH);
			double[][] hTH = TestUtils.performMatrixMultiplication(h, tH);
			double[][] wHTH = TestUtils.performMatrixMultiplication(w, hTH);
			for (int j = 0; j < m; j++) {
				for (int l = 0; l < k; l++) {
					w[j][l] = w[j][l] * (vTH[j][l] / (wHTH[j][l] + Eps));
				}
			}
		}
		
		boolean oldConfig = DMLScript.USE_LOCAL_SPARK_CONFIG; 
		DMLScript.USE_LOCAL_SPARK_CONFIG = true;
		RUNTIME_PLATFORM oldRT = DMLScript.rtplatform;
		try {
			DMLScript.rtplatform = RUNTIME_PLATFORM.HYBRID_SPARK;
			
			MLContext mlCtx = getMLContextForTesting();
			SparkContext sc = mlCtx.getSparkContext();
			mlCtx.reset();
			
			// Read two matrices through RDD and one through HDFS
			if(numRegisteredInputs >= 1) {
				JavaRDD<String> vIn = sc.textFile(GNMF_HOME + INPUT_DIR + "v", 2).toJavaRDD();
				mlCtx.registerInput("V", vIn, "text", m, n);
			}
			
			if(numRegisteredInputs >= 2) {
				JavaRDD<String> wIn = sc.textFile(GNMF_HOME + INPUT_DIR + "w", 2).toJavaRDD();
				mlCtx.registerInput("W", wIn, "text", m, k);
			}
			
			if(numRegisteredInputs >= 3) {
				JavaRDD<String> hIn = sc.textFile(GNMF_HOME + INPUT_DIR + "h", 2).toJavaRDD();
				mlCtx.registerInput("H", hIn, "text", k, n);
			}
			
			// Output one matrix to HDFS and get one as RDD
			if(numRegisteredOutputs >= 1) {
				mlCtx.registerOutput("H");
			}
			
			if(numRegisteredOutputs >= 2) {
				mlCtx.registerOutput("W");
			}
			
			MLOutput out = mlCtx.execute(fullDMLScriptName, programArgs);
			
			if(numRegisteredOutputs >= 1) {
				JavaRDD<String> hOut = out.getStringRDD("H", "text");
				String fName = GNMF_HOME + OUTPUT_DIR + "h";
				try {
					MapReduceTool.deleteFileIfExistOnHDFS( fName );
				} catch (IOException e) {
					throw new DMLRuntimeException("Error: While deleting file on HDFS");
				}
				hOut.saveAsTextFile(fName);
			}
			
			if(numRegisteredOutputs >= 2) {
				JavaRDD<String> wOut = out.getStringRDD("W", "text");
				String fName = GNMF_HOME + OUTPUT_DIR + "w";
				try {
					MapReduceTool.deleteFileIfExistOnHDFS( fName );
				} catch (IOException e) {
					throw new DMLRuntimeException("Error: While deleting file on HDFS");
				}
				wOut.saveAsTextFile(fName);
			}
		}
		finally {
			DMLScript.rtplatform = oldRT;
			DMLScript.USE_LOCAL_SPARK_CONFIG = oldConfig;
		}
		
		runRScript(true);
		disableOutAndExpectedDeletion();

		HashMap<CellIndex, Double> hmWDML = readDMLMatrixFromHDFS("w");
		HashMap<CellIndex, Double> hmHDML = readDMLMatrixFromHDFS("h");
		HashMap<CellIndex, Double> hmWR = readRMatrixFromFS("w");
		HashMap<CellIndex, Double> hmHR = readRMatrixFromFS("h");
		//HashMap<CellIndex, Double> hmWJava = TestUtils.convert2DDoubleArrayToHashMap(w);
		//HashMap<CellIndex, Double> hmHJava = TestUtils.convert2DDoubleArrayToHashMap(h);

		TestUtils.compareMatrices(hmWDML, hmWR, 0.000001, "hmWDML", "hmWR");
		TestUtils.compareMatrices(hmHDML, hmHR, 0.000001, "hmHDML", "hmHR");
		//TestUtils.compareMatrices(hmWDML, hmWJava, 0.000001, "hmWDML", "hmWJava");
		//TestUtils.compareMatrices(hmWR, hmWJava, 0.000001, "hmRDML", "hmWJava");
	}
}