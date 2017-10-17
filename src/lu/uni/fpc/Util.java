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

package lu.uni.fpc;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

import soot.FastHierarchy;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.util.Chain;

public class Util {
  /**
   * 
   * @param concreteService
   * @return
   */
  static public Set<SootClass> getAllInterafaces(SootClass concreteService) {
    Set<SootClass> intfs = new HashSet<SootClass>();
    Chain<SootClass> implementedIntfs = concreteService.getInterfaces();
    for (SootClass sc: implementedIntfs) {
      intfs.add(sc);
    }
    SootClass supc = concreteService.getSuperclass();
    if (!supc.getName().equals("java.lang.Object")) {
      intfs.addAll(getAllInterafaces(supc));
    }
    return intfs;
  }
  
  public static String time2Str(long t) {
    double td = t/1000.;
    if (td < 60) {
      return ""+ td +" s";
    }
    td = td/60.;
    if (td < 60)
      return ""+ td +" min";
    
    td = td/60.;
    if (td < 24)
      return ""+ td +" hour";
    
    td = td/24.;
    return ""+ td +" day";
  }
  
  private static HashMap<SootClass, Set<SootClass>> getAllBelowCached = new HashMap<SootClass, Set<SootClass>>();
  public static Set<SootClass> getAllBelow(SootClass sc) {
    if (getAllBelowCached.containsKey(sc)) {
      return getAllBelowCached.get(sc);
    }
    Set<SootClass> set = new HashSet<SootClass>();
    FastHierarchy fh = Scene.v().getOrMakeFastHierarchy();
    if (sc.isInterface()) {
      set.add(sc);
      Collection<SootClass> below = fh.getAllImplementersOfInterface(sc);
      set.addAll(below);
      for (SootClass e: below) {
        set.addAll (getAllBelow(e));
      }
    } else {
      set.add(sc);
      Collection<SootClass> below = fh.getSubclassesOf(sc);
      set.addAll(below);
      for (SootClass e: below) {
        set.addAll (getAllBelow(e));
      }
    }
    getAllBelowCached.put(sc, set);
    return set;
  }

  public static boolean c1isBelowc2(SootClass mClass, SootClass handlerClass) {
    Set<SootClass> set = getAllBelow (handlerClass);
    for (SootClass sc: set)
      if (sc.getName().equals(mClass.getName()))
        return true;
    return false;
  }

  public static boolean methodisOneOfClass(SootMethod sm, SootClass sc) {
    for (SootMethod m: sc.getMethods()) {
      if (sm.getName().equals(m.getName())) {
        return true;
      }
    }
    return false;
  }

  public static String printStack(Stack<SootMethod> stack) {
    String s = "----\n";
    int i = 0;
    for (SootMethod m: stack)
      s += " > "+i++ +" "+ m +"\n";
   s += "----\n";
   return s;
  }


}
