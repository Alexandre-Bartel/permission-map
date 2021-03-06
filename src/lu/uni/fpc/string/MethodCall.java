// 
// (c) 2012 University of Luxembourg - Interdisciplinary Centre for 
// Security Reliability and Trust (SnT) - All rights reserved
//
// Author: Alexandre Bartel
//
// This library is free software: you can redistribute it and/or modify
// it under the terms of the GNU Lesser General Public License as published by
// the Free Software Foundation, either version 2.1 of the License, or
// (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public License
// along with this program.  If not, see <http://www.gnu.org/licenses/>. 
//

package lu.uni.fpc.string;

import soot.Local;
import soot.SootMethod;
import soot.Unit;


  
	 class MethodCall {
		SootMethod method;
		String paramName;
		String paramConstantString;
		Local paramLocal;
		Unit paramNameUnit;
		Unit methodStmt;
		//					boolean isStringArray;

		public MethodCall(SootMethod m, Unit u) {
			method = m;
			paramName = "";
			paramConstantString = null;
			paramLocal = null;
			paramNameUnit = null;
			methodStmt = u;
			//						isStringArray = false;
		}

		//					public isStringArray(boolean b) {
		//						isStringArray = b;
		//					}
		//
		//					public boolean isStringArray() {
		//						return isStringArray;
		//					}

		public String toString() {
			return method.toString();
		}

		public String toLongString() {
			String s = "";
			s += "[I] paramName: "+ paramName;
			s += "[I] paramLocal: "+ paramLocal;
			s += "[I] paramNameUnit: "+ paramNameUnit;
			s += "[I] paramConstantString: "+ paramConstantString;
			return s;
		}

	}
