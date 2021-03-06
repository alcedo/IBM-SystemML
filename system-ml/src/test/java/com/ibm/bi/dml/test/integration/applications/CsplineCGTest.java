/**
 * (C) Copyright IBM Corp. 2010, 2015
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package com.ibm.bi.dml.test.integration.applications;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import com.ibm.bi.dml.runtime.matrix.data.MatrixValue.CellIndex;
import com.ibm.bi.dml.test.integration.AutomatedTestBase;
import com.ibm.bi.dml.test.integration.TestConfiguration;
import com.ibm.bi.dml.test.utils.TestUtils;

@RunWith(value = Parameterized.class)
public class CsplineCGTest extends AutomatedTestBase {
    private final static String TEST_DIR = "applications/cspline/";
    private final static String TEST_CSPLINE = "CsplineCG";
    private int numRecords, numDim;
    public CsplineCGTest(int rows, int cols) {
        numRecords = rows;
        numDim = 1; // we have cubic spline which is always one dimensional
    }
    @Parameters
    public static Collection<Object[]> data() {
        Object[][] data = new Object[][] {
                {10, 1},
                {100, 1},
                {1000, 1},
        };
        return Arrays.asList(data);
    }
    @Override
    public void setUp() {
        setUpBase();
        addTestConfiguration(TEST_CSPLINE, new TestConfiguration(TEST_DIR, "CsplineCG",
                new String[] {}));
    }
    @Test
    public void testCspline()
    {
        int rows = numRecords;
        int cols = numDim;
        TestConfiguration config = getTestConfiguration(TEST_CSPLINE);
        String CSPLINE_HOME = SCRIPT_DIR + TEST_DIR;
        fullDMLScriptName = CSPLINE_HOME + TEST_CSPLINE + ".dml";
        int numIter = rows; // since CG will converse in worse case n
        programArgs = new String[]{"-nvargs",
                "X=" + CSPLINE_HOME + INPUT_DIR + "X",
                "Y=" + CSPLINE_HOME + INPUT_DIR + "Y",
                "K=" + CSPLINE_HOME + OUTPUT_DIR + "K",
                "O=" + CSPLINE_HOME + OUTPUT_DIR + "pred_y",
                "maxi="+numIter,
                "inp_x="+4.5 };
        fullRScriptName = CSPLINE_HOME + TEST_CSPLINE + ".R";
        rCmd = "Rscript" + " " +
                fullRScriptName + " " +
                CSPLINE_HOME + INPUT_DIR + "X.mtx" + " " +
                CSPLINE_HOME + INPUT_DIR + "Y.mtx" + " " +
                4.5 + " " +
                CSPLINE_HOME + EXPECTED_DIR + "pred_y";
        loadTestConfiguration(config);
        double[][] X = new double[rows][cols];

        // X axis is given in the increasing order
        for (int rid = 0; rid < rows; rid++) {
            for (int cid = 0; cid < cols; cid++) {
                X[rid][cid] = rid+1;
            }
        }
        double[][] Y = getRandomMatrix(rows, cols, 0, 5, 1.0, -1);

        writeInputMatrixWithMTD("X", X, true);
        writeInputMatrixWithMTD("Y", Y, true);

        runTest(true, false, null, -1);

        runRScript(true);
        disableOutAndExpectedDeletion();

        HashMap<CellIndex, Double> pred_y_R = readRMatrixFromFS("pred_y");
        HashMap<CellIndex, Double> pred_y_DML= readDMLMatrixFromHDFS("pred_y");

        boolean success =
                TestUtils.compareMatrices(pred_y_R, pred_y_DML, Math.pow(10, -5), "k_R", "k_DML");
        System.out.println(success+"");
    }
}
