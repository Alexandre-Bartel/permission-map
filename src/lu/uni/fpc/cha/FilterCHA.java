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

package lu.uni.fpc.cha;

import java.io.File;
import java.util.Stack;

import soot.Scene;
import soot.SootClass;
import soot.SootMethod;

public class FilterCHA {
	
	static FilterCHA instance = null;
	
	public static FilterCHA v() {
		if (instance == null)
			instance = new FilterCHA();
		return instance;
	}
	
	private FilterCHA() {}

  public boolean mustSkip(Stack<SootMethod> stack, SootMethod currentMethod) {
    
    String mName = currentMethod.toString().replaceAll("<", "").replaceAll(">", "");
    String className = mName.split(":")[0];
    SootClass mClass = Scene.v().getSootClass(mName.split(":")[0]);
    
    if (stack.size() <= 2)
      return false;
    
    // only keep android.* and com.android.*
    if (!(mName.startsWith("android.") || mName.startsWith("com.android."))) {
      //System.out.println("[filter-android] "+ mName);
      return true;
    }
    
    if (ConfigCHA.v().skipOnTransact())
    	if (mName.contains("onTransact("))
    		return true;
    
    if (ConfigCHA.v().skipStubProxy())
      if (className.endsWith("$Proxy") || className.endsWith("$Stub$Proxy"))
        return true;
    
//    if (false) {// activate pscout filter even though we are searching the graph forweards and they are searching the graph backwands
//      if (pscoutFilters(currentMethod))
//        return true;
//    }
    
    return false;
    
  }
  
  public boolean pscoutFilters(SootMethod currentMethod) {
    
    // 
    String mName = currentMethod.toString().replaceAll("<", "").replaceAll(">", "");
    String className = mName.split(":")[0];
    SootClass mClass = Scene.v().getSootClass(mName.split(":")[0]);
    
    if (mName.contains("onTransact"))
      return true;
//    if (className.startsWith("com.android."))
//      return true;
    if (className.startsWith("android.os.A"))
      return true;
    if (className.startsWith("android.os.B"))
      return true;
    if (className.startsWith("android.os.C"))
      return true;
    if (className.startsWith("android.os.D"))
      return true;
    if (className.startsWith("android.os.E"))
      return true;
    if (className.startsWith("android.os.F"))
      return true;
    if (className.startsWith("android.os.G"))
      return true;
    if (className.startsWith("android.os.H"))
      return true;
    if (className.startsWith("android.os.I"))
      return true;
    if (className.startsWith("android.os.J"))
      return true;
    if (className.startsWith("android.os.K"))
      return true;
    if (className.startsWith("android.os.L"))
      return true;
    if (className.startsWith("android.os.M"))
      return true;
    if (className.startsWith("android.os.N"))
      return true;
    if (className.startsWith("android.os.O"))
      return true;
    if (className.startsWith("android.os.P"))
      return true;
    
    return false;
  }

public void addDefaultClassesToKeep() {
	// TODO Auto-generated method stub
	
}

public void addClassesToAnalyze(File f) {
	// TODO Auto-generated method stub
	
}

public void addDefaultClassesToSkip() {
	// TODO Auto-generated method stub
	
}

public void addClassesToSkip(File fskip) {
	// TODO Auto-generated method stub
	
}
  
  

}
