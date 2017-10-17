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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lu.uni.epw.Config;
import lu.uni.epw.Util;
import lu.uni.epw.Wrappers;
import soot.ArrayType;
import soot.FastHierarchy;
import soot.PrimType;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Type;

public class GatherTypes {

  public static void gatherTypes (Map<String, Set<SootClass>> ttg) {
    GatherTypes gt = new GatherTypes();
    
    boolean newClassesAdded = true;
    while (newClassesAdded) {
      newClassesAdded = false;
      int csize = ttg.get("concrete").size();
      int asize = ttg.get("abstract").size();
      int isize = ttg.get("interface").size();
    
      for (SootClass sc: new HashSet<SootClass>(ttg.get("concrete"))) {
        System.out.println("[gather] for concrete '"+ sc +"'");
        gt.gatherType(sc, ttg);
      }
      for (SootClass sc: new HashSet<SootClass>(ttg.get("abstract"))) {
        System.out.println("[gather] for abstract '"+ sc +"'");
        gt.gatherType(sc, ttg);
        // abstract and concrete direct and indirect subclasses
        gt.gatherSubFromAbstract(sc, ttg);
      }
      for (SootClass sc: new HashSet<SootClass>(ttg.get("interface"))) {
        System.out.println("[gather] for interface '"+ sc +"'");
        gt.gatherType(sc, ttg);
        // add abstract and concrete direct and indirect subclasses
        gt.gatherSubFromInterface(sc, ttg);
      }
      
      int csize2 = ttg.get("concrete").size();
      int asize2 = ttg.get("abstract").size();
      int isize2 = ttg.get("interface").size();      
      if (csize2 > csize || asize2 > asize || isize2 > isize) {
        System.out.println("[gather] new classes were added, ...");
        newClassesAdded = true;
      }
      
    }
  }

  public void gatherType (SootClass sc, Map<String, Set<SootClass>> typesToGenerate) {
    
    List<SootMethod> constructors = Util.getConstructors(sc);
    
    for (SootMethod sm: constructors) {
        if ((sm.isPublic() ) &&
            ((sm.getModifiers() & Util.ACC_SYNTHETIC) == 0) &&
            (sm.isConcrete() || ((sc.isAbstract() || sc.isInterface()) && sm.isAbstract())) ) {
          //mList.add (sm);
          List<Type> checkTypeList = new ArrayList<Type>();
          
          //checkTypeList.add (sm.getReturnType());
          checkTypeList.addAll((List<Type>)sm.getParameterTypes());
          for (Type t: checkTypeList) {
            // skip primitive types
            if (t instanceof PrimType) 
              continue;
            // check that type is in Soot's Scene
            SootClass typeClass = Scene.v().getSootClass(t.toString());
            if (!Config.v().isTargetClass(typeClass.toString())) {
            	System.out.println("skipping excluded class "+ typeClass);
            	continue;
            }
            
            if (typeClass.isPrivate() || typeClass.isProtected()) {
            	System.out.println("skipping protected or private class: "+ typeClass);
            	continue;
            }
            
            if (t instanceof ArrayType) {
                System.out.println("[gather] handle array type: "+ t);
                ArrayType at = (ArrayType)t;
                int dim = at.numDimensions;
                Type et = at.getElementType();
//                SootClass eTypeClass = Scene.v().getSootClass(t.toString());

                SootClass base = Scene.v().getSootClass(et.toString());
                for (int i = 1; i <= dim; i++) {
                	Wrappers.addToArray(base, i);
                }
                typeClass = base;

            } else if (typeClass == null || typeClass.isPhantom()) {
              System.err.println("error: class required for method parameter not  in scene! "
                      + t +"(type: '"+ t.getClass() +"')");
              typesToGenerate.get("notInScene").add(typeClass);
              continue;
            }
            
            
            // base type can change if array
            if (!typeClass.isPublic())
            	continue;
            
            if (typeClass.isInterface()) {
              Set<SootClass> s = typesToGenerate.get("interface");
              if (!s.contains(typeClass)) {
                s.add(typeClass);
                gatherType(typeClass, typesToGenerate);
                System.out.println("[gather] type interface "+ typeClass);
              }
            } else if (typeClass.isAbstract()) {
              Set<SootClass> s = typesToGenerate.get("abstract");
              if (!s.contains(typeClass)) {
                s.add(typeClass);
                gatherType(typeClass, typesToGenerate);
                System.out.println("[gather] type abstract "+ typeClass);
              }
            } else if (typeClass.isConcrete()) {
              Set<SootClass> s = typesToGenerate.get("concrete");
              if (!s.contains(typeClass)) {
                s.add(typeClass);
                gatherType(typeClass, typesToGenerate);
                System.out.println("[gather] type concrete "+ typeClass);
              }
            } else {
              throw new RuntimeException("error: class not interface nor abstract nor concrete! "+ typeClass);
            }
          }
        }
      }  
  }
  
  private void gatherSubFromInterface(SootClass sc, Map<String, Set<SootClass>> ttg) {
    FastHierarchy h = Scene.v().getOrMakeFastHierarchy();
    Set<SootClass> implementersSet = (Set<SootClass>)h.getAllImplementersOfInterface (sc);
    // get subclasses
    Collection<SootClass> subclassesCollec = new HashSet<SootClass>();
    for (SootClass sc2: implementersSet) {
      Set<SootClass> s = Util.getAllSubclassesOf (sc2);
      subclassesCollec.addAll (s);
    }
    addClasses(subclassesCollec, ttg);
  }

  private void gatherSubFromAbstract(SootClass sc, Map<String, Set<SootClass>> ttg) {
    Set<SootClass> s = Util.getAllSubclassesOf (sc);
    addClasses(s, ttg);
  }
  
  private void addClasses(Collection<SootClass> classes, Map<String, Set<SootClass>> ttg) {
    for (SootClass sc: classes) {
      // skip inner class
      if (sc.getName().contains("$"))
        continue;
      if (!Config.v().isTargetClass(sc.toString())) {
      	System.out.println("skipping excluded class "+ sc);
      	continue;
      }
        
      // add class
      if (sc.isPublic()) {
    	  Wrappers.addClassToTTG(sc);
      }
    }
  }
  
}
