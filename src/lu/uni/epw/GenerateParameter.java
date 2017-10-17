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

package lu.uni.epw;

import java.util.List;

import soot.ArrayType;
import soot.Hierarchy;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Type;

public class GenerateParameter {

	SootMethod sm = null;
	Hierarchy h = null;

	boolean debug = true; /* false */

	public GenerateParameter(SootMethod sm) {
		this.sm = sm;
	}


	public String generateDumb() {
		String r = "";
		if (sm == null) { return r; }
		List<Type> tList = sm.getParameterTypes();
		int i = 0;
		String value = "";
		for (Type t: tList) {
			if (i > 0) {
				r += ", ";
			}
			
			if (t.toString().contains("$")) {
				r += "/* inner class? */("+ t.toString().replaceAll("\\$","\\.") +")";
				//Debug.printDbg("["+ sm +"] generateDumb: type: "+ t);
				//r += "/* parameters contain innerclass '"+ sm +"' */\n";
			} else {
				if (Util.isPrimType(t)) {
					r += "("+ t +")";
				} else {
					SootClass typeSC = Scene.v().getSootClass(t.toString().replaceAll("\\[\\]",""));
					Debug.printDbg(typeSC +" isInterface or is Abstract? "+ ((typeSC.isAbstract()||typeSC.isInterface())?"1":"0"));
					if (t.toString().contains("android.widget.ListAdapter")) {
						r += "("+ "android.widget.ArrayAdapter" +")";
					} else {
						r += "("+ t +")";
					}
				}
			}
			if (Util.isPrimType(t)) {
				value = "0";
				if (t.toString().startsWith("boolean")) {
				  value = "false";
				}
			} else {
				value = "null";
			}
			if (t.toString().contains("[]")) {
				value = "null";
			}
			r = r.replaceAll("\\$", "\\."); // inner class
			r += value;
			i += 1;
		}
		return r;
	}

	/**
	 * 
	 * @return
	 */
  public String generateFromGG() {
    String r = "";
    if (sm == null) { return r; }
    List<Type> tList = sm.getParameterTypes();
    int i = 0;
    String value = "";
    for (Type t: tList) {
      if (i > 0) {
        r += ", ";
      }
      
      SootClass pc = Scene.v().getSootClass(t.toString());
      if (pc.isPrivate() || pc.isProtected()) {
    	  r += "/* protected or private class? */ null ";
    	  continue;
      }
      
      if (t.toString().contains("$")) {
    	  System.out.println(" inner class:: "+ t +" private:"+ pc.isPrivate() +" protected:"+ pc.isProtected());
        r += "/* inner class? */("+ t.toString().replaceAll("\\$","\\.") +")";
        //Debug.printDbg("["+ sm +"] generateDumb: type: "+ t);
        //r += "/* parameters contain innerclass '"+ sm +"' */\n";
      } else {
        if (Util.isPrimType(t)) {
          r += "("+ t +")";
        } else {
          r += "("+ t +")";
        }
      }
      
      if (t.toString().equals("boolean")) {
        value = "false";
      } else if (Util.isPrimType(t)) {
        value = "0";
      }else if (!Config.v().isTargetClass(t.toString())) {
        System.out.println("excluded parameter type: "+ t);
        value = "null";
      } else if (t.toString().endsWith("[]")) {
        value = makeArray (t);
      } else {
    	  SootClass baseClass = Scene.v().getSootClass(t.toString());
    	  if (Wrappers.isHandledClass(baseClass)) {
	        String generatorClassName = Wrappers.generatorClassName;
	        value = generatorClassName;
	        String scGeneratorName = t.toString().replaceAll("\\.", "").replaceAll("\\$", "__");
	        value +="."+ "get_" + scGeneratorName +"()";
    	  } else {
    		  value = "null";
    	  }
      }
      r = r.replaceAll("\\$", "\\."); // inner class
      r += value;
      i += 1;
    }
    return r;
  }

  private String makeArray(Type t) {
    ArrayType at = null;
    int dim = 0;
    String type = t.toString();
    if (t instanceof ArrayType) {
      at = (ArrayType)t;
    } else {
      throw new RuntimeException("error: not an ArrayType! "+ t);
    }
    while (type.endsWith("[]")) {
      type = type.replaceAll("\\[\\]$", "");
      dim++;
    }
    Type baseType = at.baseType;
    if (Util.isPrimType(baseType))
      return "null";
    // at this point 'type' is the array element type
    String r = null;
    SootClass baseClass = Scene.v().getSootClass(baseType.toString());
    if (Wrappers.isHandledClass(baseClass)) {
    	r = Wrappers.generatorClassName +".get_"+ t.toString().replaceAll("\\.", "").replaceAll("\\$", "__").replaceAll("\\[\\]", "_ARRAY_") +"()";
    } else {
    	r = "null";
    }
    return r;
  }


}
