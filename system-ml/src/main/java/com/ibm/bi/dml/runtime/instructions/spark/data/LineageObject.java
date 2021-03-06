/**
 * (C) Copyright IBM Corp. 2010, 2015
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */

package com.ibm.bi.dml.runtime.instructions.spark.data;

import java.util.ArrayList;
import java.util.List;

import com.ibm.bi.dml.runtime.controlprogram.caching.MatrixObject;

public abstract class LineageObject 
{

	//basic lineage information
	protected int _numRef = -1;
	protected List<LineageObject> _childs = null;
	protected String _varName = null;
	
	//N:1 back reference to matrix object
	protected MatrixObject _mo = null;
	
	protected LineageObject()
	{
		_numRef = 0;
		_childs = new ArrayList<LineageObject>();
	}
	
	public String getVarName() {
		return _varName;
	}
	
	public int getNumReferences()
	{
		return _numRef;
	}
	
	public void setBackReference(MatrixObject mo)
	{
		_mo = mo;
	}
	
	public boolean hasBackReference()
	{
		return (_mo != null);
	}
	
	public void incrementNumReferences()
	{
		_numRef++;
	}
	
	public void decrementNumReferences()
	{
		_numRef--;
	}
	
	public List<LineageObject> getLineageChilds()
	{
		return _childs;
	}
	
	public void addLineageChild(LineageObject lob)
	{
		lob.incrementNumReferences();
		_childs.add( lob );
	}
}
