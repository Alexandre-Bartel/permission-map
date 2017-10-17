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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lu.uni.epw.Config;
import lu.uni.epw.Util;
import lu.uni.epw.Wrappers;
import soot.FastHierarchy;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;

public class AbstractClass {
  
  public static void generate(Set<SootClass> set, Map<String, String> generatedAC) {
    for (SootClass c: set) {
    	if (!c.isPublic()) {
    		throw new RuntimeException("error: abstract class not public: "+ c);
    	}
      String s = generateAbstractClass(c);
      generatedAC.put(c.getName(), s);
    }
  }
  
  /**
   * Generate a class generator for abstract class 'sc'.
   * @param sc abstract class
   * @return a string representing the class generator
   */
  public static String generateAbstractClass (SootClass sc) {
	  
		
//      if (!Config.v().isTargetClass(sc.toString())) {
//      	System.out.println("skipping excluded abstract class "+ sc);
//      	return "";
//      }
	  
	  if (!sc.isPublic()) {
		  throw new RuntimeException("error: abstract class not public: "+ sc);
	  }
	  
	  
    if (sc.getName().contains("]")) {
      System.out.println(sc);
      new Exception().printStackTrace();
      System.exit(-1);
    }
    String type = sc.getName().replaceAll("\\$", "\\.");
    String o = "o_o";
    String g = "";
    String methodName = " get_"+ sc.getName().replaceAll("\\.","").replaceAll("\\$", "__").replaceAll("\\[\\]", "_ARRAY_");
    g += "public static "+ type +" "+ methodName +"() {\n";
    g += "  "+ type +" "+ o +" = ("+ type +") null;\n";
    List<SootMethod> constructors = Util.getConstructors (sc);
    FastHierarchy h = Scene.v().getOrMakeFastHierarchy();
    Set<SootClass> subclassesCollec = Util.getAllSubclassesOf (sc);
    int concreteNbr = 0;
    for (SootClass sc2: (Collection<SootClass>) subclassesCollec) {
      // skip type containing digits
      if (sc2.toString().matches(".*[0-9].*")){
        continue;
      }
      // skip inner classes
      if (sc2.toString().contains("$")) {
        continue;
      }
      // only public class
      if (!sc2.isPublic()) {
        continue;
      }
      // end skip
      if (sc2.isConcrete()) {
        // check if array
        int dim = 0;
        String elementType = type.toString();
        while (elementType.endsWith("[]")) {
          elementType = elementType.replaceAll("\\[\\]$", "");
          dim++;
        }
        
        concreteNbr += 1;
        //typeNeeded.add (sc2);
        //if (dim > 0) {
        //  g += "  "+ o +" = ("+ type +")"+ "get_"+ elementType.replaceAll("\\.","").replaceAll("\\$", "__")+ "_ARRAY"+ dim +"();\n";    
        //} else {
          g += "  "+ o +" = ("+ type +")"+ "get_"+ sc2.getName().replaceAll("\\.","").replaceAll("\\$", "__").replaceAll("\\[\\]", "_ARRAY_") +"();\n";
        //}
      }
    }
    if (concreteNbr == 0) {
      //createFakeAbstract (sc);
      //g+= "  "+ o +" = ("+ type +")"+ typesFilename +".get_fakeabstract_"+ sc.getName().replaceAll("\\.","").replaceAll("\\$", "") +"();\n";
    }
    g += "  return "+ o +";\n";
    g += "}\n";
    
    // Handling Arrays
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

    return g + g2;
  }


}
