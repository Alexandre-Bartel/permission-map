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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import soot.ArrayType;
import soot.FastHierarchy;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Type;
import soot.util.Chain;

public class Util {
  
  public static final int ACC_SYNTHETIC = 0x1000;
  
  /**
   * 
   * @param sc
   * @return
   */
  public static Set<SootClass> getAllSubclassesOf (SootClass sc) {
      Set<SootClass> subClassesSet = new HashSet<SootClass>();
      FastHierarchy fh = Scene.v().getOrMakeFastHierarchy();
      Collection<SootClass> subClasses = fh.getSubclassesOf(sc); 
      for (SootClass c: subClasses) {
        subClassesSet.add(c);
        subClassesSet.addAll(Util.getAllSubclassesOf(c));
      }
      return subClassesSet;
   }

  /**
   * 
   * @param c
   * @return List of all public methods of SootClass c
   */
  public static List<SootMethod> getAccessiblePublicMethods(SootClass c) {
    List<SootMethod> smList = new ArrayList<SootMethod>();

    smList.addAll( c.getMethods() );

    if (c.toString().equals("java.lang.Object")){ return smList; }
    if (c.toString().endsWith("[]")) { return smList; }

    SootClass superclass = c.getSuperclass();
    if (superclass != null && !superclass.toString().equals("java.lang.Object")) {
      smList.addAll( getAccessiblePublicMethods( superclass ));
    }

    Chain<SootClass> interfacesChain = c.getInterfaces();
    for (SootClass si: interfacesChain) {
      smList.addAll( getAccessiblePublicMethods( si ));
    }

    return smList;
  }
  
  /**
   * 
   * @param c
   * @return
   */
  public static List<SootMethod> getConstructors(SootClass c) {
    List<SootMethod> constructors = new ArrayList<SootMethod>();
    
    // special case if class is an Android service manager class
    if (Managers.isManager(c)) {
      constructors = Managers.getManagerConstructor(c);
      return constructors;
    }
    
    for (SootMethod sm: getAccessiblePublicMethods(c)) {
    // get constructors
      if (sm.isConstructor() && sm.getDeclaringClass().equals(c)) {
        constructors.add(sm);
        continue;
      }
    // get static methods returning 'c' type
      if (sm.isStatic() && sm.getReturnType().toString().equals(c.getType().toString())) {
        constructors.add(sm);
        continue;
      }
    // TODO: get methods from other classes returning 'c' type or a subtype of 'c'
    }
    
    return constructors;
  }
  
//  /**
//   * 
//   * @param m
//   * @param type
//   * @param o
//   * @return
//   */
//  public static String generateFromStaticConstructorMethod(SootMethod m, String type, String o) {
//    String cr = null;
//    cr = "  "+ o +" = "+ m.getDeclaringClass().toString() +"."+ m.getName() +"();\n";
//    return cr;
//  }
  
  /**
   * 
   * @param m
   * @param type
   * @param o
   * @return
   */
  public static String generateFromConstructorMethod(SootMethod m, String type, String o) {
    String cr = "";
    if (m.isStatic()) {
      cr = "  "+ o +" = "+ m.getDeclaringClass().toString() +"."+ m.getName() +"(";
    } else {
      cr += "  "+ o +" = new "+ type +"(";
    }
    List<Type> typeList = (List<Type>)m.getParameterTypes();
    boolean skip = false;
    // skip type containing digits
    for (int i=0; i< typeList.size(); i++) {
      if (typeList.get(i).toString().matches(".*[0-9].*")){
        skip = true;
        break;
      }
    }
    if (skip) {return null;}
    // end skip
    for (int i=0; i< typeList.size(); i++) {
      Type t = typeList.get(i);
      String tS = t.toString().replaceAll("\\$", "\\.");
      if (Util.isParameterPrimType(tS)) { // prim type
        cr += "("+ tS +")"+ Util.generateBasicTypeValue(tS); 
      } else { // other type
    		
          if (!Config.v().isTargetClass(t.toString())) {
          	System.out.println("skipping excluded parameter class "+ t);
          	cr += "("+ tS +") null ";
          } else {
        	  SootClass baseClass = null;
        	  if (t instanceof ArrayType) {
        		  ArrayType at = (ArrayType)t;
        		  baseClass = Scene.v().getSootClass(at.baseType.toString());  
        	  } else {
        		  baseClass = Scene.v().getSootClass(t.toString());
        	  }
        	  SootClass sc2 = Scene.v().getSootClass (t.toString());
        	  System.out.println("sootclass for type '"+ t +"' :"+ sc2);
        	  String mCall = "";
        	  if (Wrappers.isHandledClass(baseClass)) {
        		  mCall = " get_"+ sc2.toString().replaceAll("\\.","").replaceAll("\\$", "__").replaceAll("\\[\\]", "_ARRAY_") +"()";
        	  } else {
        		  mCall = " null";
        	  }
        	  cr += "("+ tS +") " + mCall; //"null";
          }
      }
      if (i+1 < typeList.size()) {
        cr += ",";
      }
    }
    cr += ");\n";
    return cr;
  }
  
  /**
   * Generate default values for primitive types.
   * @param t
   * @return
   */
  public static String generateBasicTypeValue(String t) {
    String value = "null";
    if (Util.isPrimType(t)) {
      if (t.equals("boolean")) {
        value = "false";
      } else {
        value = "0";
      }
    } else if (t.startsWith("java.lang.String")) {
      value = "\"\"";

    } else if (t.contains("[]")) {
      value = "null";
    }
    return value;
  }
  
//  public static String generateBasicPrimTypeValue(Type t) {
//    if (!(t instanceof PrimType))
//      throw new RuntimeException("error: type not instance of PrimType! "+ t);
//    if (t instanceof BooleanType) {
//      return "false";
//    } else {
//      return "0";
//    }
//  }
  
  
  /**
   * 
   * @param t
   * @return true if t is primitive type
   */
  public static boolean isPrimType(Type t) {
    return isPrimType (t.toString());
  }
  
  /**
   * 
   * @param t
   * @return true if t is primitive type
   */
  public static boolean isParameterPrimType (String t) {
    if (t.startsWith("void") ||
        t.startsWith("char") ||
        t.startsWith("byte") ||
        t.startsWith("short") ||
        t.startsWith("int") ||
        t.startsWith("float") ||
        t.startsWith("long") ||
        t.startsWith("double") ||
        t.startsWith("boolean")) {
      return true;
    } else {
      return false;
    }
  }
  
  /**
   * 
   * @param t
   * @return true if t is primitive type
   */
  public static boolean isPrimType (String t) {
    if (t.equals("void") ||
        t.equals("char") ||
        t.equals("byte") ||
        t.equals("short") ||
        t.equals("int") ||
        t.equals("float") ||
        t.equals("long") ||
        t.equals("double") ||
        t.equals("boolean")) {
      return true;
    } else {
      return false;
    }
  }

  public static String addSpacesBefore(String g) {
    return g.replaceAll("^", "  ").replaceAll("\\\n", "\\\n  ");
  }


  public boolean isPublicClass(SootClass sc) {
	  if (!sc.isInnerClass()) {
		  return sc.isPublic();
	  } else {
		  return sc.isPublic() && isPublicClass(sc.getOuterClass());
	  }
  }

  
}
