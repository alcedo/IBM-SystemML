package com.ibm.bi.dml.runtime.instructions.spark;

import com.ibm.bi.dml.parser.Expression.DataType;
import com.ibm.bi.dml.parser.Expression.ValueType;
import com.ibm.bi.dml.runtime.DMLRuntimeException;
import com.ibm.bi.dml.runtime.DMLUnsupportedOperationException;
import com.ibm.bi.dml.runtime.functionobjects.Builtin;
import com.ibm.bi.dml.runtime.functionobjects.ValueFunction;
import com.ibm.bi.dml.runtime.instructions.Instruction;
import com.ibm.bi.dml.runtime.instructions.cp.CPOperand;
import com.ibm.bi.dml.runtime.instructions.cp.ScalarScalarBuiltinCPInstruction;
import com.ibm.bi.dml.runtime.matrix.operators.BinaryOperator;
import com.ibm.bi.dml.runtime.matrix.operators.Operator;
import com.ibm.bi.dml.runtime.matrix.operators.RightScalarOperator;

public abstract class BuiltinBinarySPInstruction extends BinarySPInstruction 
{
	@SuppressWarnings("unused")
	private static final String _COPYRIGHT = "Licensed Materials - Property of IBM\n(C) Copyright IBM Corp. 2010, 2015\n" +
                                             "US Government Users Restricted Rights - Use, duplication  disclosure restricted by GSA ADP Schedule Contract with IBM Corp.";
	
	private int arity;
	
	public BuiltinBinarySPInstruction(Operator op, CPOperand in1, CPOperand in2, CPOperand out, int _arity, String opcode, String istr )
	{
		super(op, in1, in2, out, opcode, istr);
		_sptype = SPINSTRUCTION_TYPE.BuiltinBinary;
		arity = _arity;
	}

	public int getArity() {
		return arity;
	}
	
	public static Instruction parseInstruction ( String str ) 
		throws DMLRuntimeException, DMLUnsupportedOperationException {
		CPOperand in1 = new CPOperand("", ValueType.UNKNOWN, DataType.UNKNOWN);
		CPOperand in2 = new CPOperand("", ValueType.UNKNOWN, DataType.UNKNOWN);
		CPOperand out = new CPOperand("", ValueType.UNKNOWN, DataType.UNKNOWN);
		String opcode = parseBinaryInstruction(str, in1, in2, out);
		
		ValueFunction func = Builtin.getBuiltinFnObject(opcode);
		
		// Determine appropriate Function Object based on opcode
			
		if ( in1.getDataType() == DataType.SCALAR && in2.getDataType() == DataType.SCALAR ) {
			return new ScalarScalarBuiltinCPInstruction(new BinaryOperator(func), in1, in2, out, opcode, str);
		} else if (in1.getDataType() != in2.getDataType()) {
			return new MatrixScalarBuiltinSPInstruction(new RightScalarOperator(func, 0), in1, in2, out, opcode, str);					
		} else { // if ( in1.getDataType() == DataType.MATRIX && in2.getDataType() == DataType.MATRIX ) {
			return new MatrixMatrixBuiltinSPInstruction(new BinaryOperator(func), in1, in2, out, opcode, str);	
		} 
	}
}
