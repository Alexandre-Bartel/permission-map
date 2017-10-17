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

package lu.uni.epw.gent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lu.uni.epw.Config;
import lu.uni.epw.Util;
import lu.uni.epw.Wrappers;
import soot.ArrayType;
import soot.PrimType;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Type;

public class ConcreteClass {

  public static void generate(Set<SootClass> set, HashMap<String, String> generatedCC) {
    for (SootClass c: set) {
    	if (!c.isPublic()) {
    		throw new RuntimeException("error: concrete class not public: '''"+ c +"''''");
    	}
      String s = generateConcreteClass(c);
      generatedCC.put(c.getName(), s);
    }
  }
  
  /**
   * Generate class generator for concrete class 'sc'
   * @param sc concrete class
   * @return String representation of the Java code
   */
  public static String generateConcreteClass (SootClass sc) {
//    if (sc.getName().contains("]")) {
//      System.out.println(sc);
//      new Exception().printStackTrace();
//      System.exit(-1);
//    }
	
//      if (!Config.v().isTargetClass(sc.toString())) {
//      	System.out.println("skipping excluded concrete class "+ sc);
//      	return "";
//      }
	  
    boolean isInnerclass = sc.getName().contains("$") ? true : false;
    String type = sc.getName().replaceAll("\\$", "\\.");
    String o = "o_o";
    String g = "";
    String methodName = " get_"+ sc.getName().replaceAll("\\.","").replaceAll("\\$", "__");
    g += "public static "+ type +" "+ methodName +"() {\n";
    g += "  "+ type +" "+ o +" = ("+ type +") null;\n";
    if (!isInnerclass && !soot.Modifier.isEnum(sc.getModifiers())) {
      List<SootMethod> constructors = Util.getConstructors (sc);
      for (SootMethod m: constructors) {
        
    	  // check if constructor is a framework method
    	  if (!(Config.v().isFrameworkMethod(m) || Config.v().isServicesInitMethod(m))) {
    		  System.out.println("not a framework constructor! Skipping "+ m);
    		  continue;
    	  }
    	  
        // check if constructor method is public
        if (!m.isPublic()) {
          System.out.println("[gen] concrete: skipping non-public constructor method: "+ m);
          continue;
        }
        
        // check if constructor can be generated with only public parameters
        boolean canGenerateConstructor = true;
        List<Type> tList = (List<Type>)m.getParameterTypes();
        for (Type t: tList) {
          if (t instanceof ArrayType) {
            Type elementType = ((ArrayType)t).baseType;
            if (elementType instanceof PrimType)
              continue;
          }
          if (t instanceof PrimType) {
            continue;
          }
          String tS = t.toString().replaceAll("\\[\\]","");
          SootClass c = Scene.v().getSootClass (tS);
          if (!Scene.v().containsClass(tS)) {
            System.out.println(" cannot generate constructor t: "+ tS +" NOT IN SCENE!"+ sc);
            canGenerateConstructor = false;
            break;
           }
          if (!c.isPublic()) {
            System.out.println(" cannot generate constructor: not public! "+ c);
            canGenerateConstructor = false;
            System.out.println("modifiers: "+ Integer.toHexString(c.getModifiers()) +"("+ Integer.toHexString(java.lang.reflect.Modifier.PUBLIC) +")");
            for(SootMethod method: c.getMethods()) {
            System.out.println("\tm: "+ method);
            }
            break;
          }
        }

        if (!canGenerateConstructor) {
          System.out.println("cannot generate constructor: "+ m);
          continue;
        } else {
          System.out.println("can generate constructor: "+ m);
        }
        
        String cr = "";
        boolean throwsException = (m.getExceptions().size() > 0 ? true : false);
        if (throwsException) {
          cr += "  try {\n";
        }
        
        // if method is static: no new!
        String cg = null;
        cg = Util.generateFromConstructorMethod(m, type, o);
        if (cg == null)
          continue;
        cr = cr + cg;
        
        if (throwsException) {
          cr += "  } catch (java.lang.Throwable t) {} \n";
        }
        g += cr;
      }
    }//inner
    g += "  return o_o;\n";
    g += "}\n";
    
    
    String g2 = "";
    Map<SootClass, Set<Integer>> array = Wrappers.getArrays();
    if (array.containsKey(sc)) {
    	for (Integer dim : array.get(sc)) {
    		int d = dim.intValue();
	    	g2 += "\n\n";
	    	String methodName2 = methodName;
	    	for (int i=0; i<d; i++) {
	    		methodName2 += "_ARRAY_";
	    	}
	    	String type2 = type;
	    	for (int i=0; i<d; i++) {
	    		type2 += "[]";
	    	}
	    	g2 += "public static "+ type2 +" "+ methodName2 +"() {\n return null; \n}\n";
    	}
    	
    }

    String str = g + g2;
    
    return str;
  }

}
