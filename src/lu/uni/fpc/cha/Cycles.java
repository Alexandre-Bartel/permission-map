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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import soot.SootMethod;

// Handle cycles
public class Cycles {
  
  Map<CycleKey, Set<SootMethod>> cycle2Methods = new HashMap<CycleKey, Set<SootMethod>>();
  Set<SootMethod> cache = new HashSet<SootMethod>();
  
  private String printMethodAndStack(SootMethod sm, Stack<SootMethod> stack) {
      String s = "\n method: "+ sm +"\n";
      for (SootMethod m: stack)
        s += m.toString() +"\n";
     return s;
  }
  
  public void printStats() {
    System.out.println("[cycle] cycles #: "+ cycle2Methods.keySet().size());
  }
  
  public void addCycle(Stack<SootMethod> stack, SootMethod sm) {
    // check that there is a cycle with m
    if (stack.search(sm) == -1)
      throw new RuntimeException("error: method not in stack. No cycle!"+ printMethodAndStack(sm, stack));
    
    // check that there are not more than one
    if (stack.lastIndexOf(sm) != stack.indexOf(sm))
      throw new RuntimeException("error: a method cannot be more than twice in the stack! "+ printMethodAndStack(sm, stack));
    
    // two cycles with the same method is not possible since we memorize already computed methods
    //if (cycle2Methods.containsKey(sm))
    //  throw new RuntimeException("error: there already exist a cycle with the same method! "+ printMethodAndStack(sm, stack));
    
    // At this point the stack looks like this:
    // 0  1  2        n-1
    // M1|M2|M3|.....|Mn
    // sm = M3
    // sm is at depth n in the stack, but is not in the stack.
    // Methods M3, M4, ... Mn have to be saved to be later 
    // updated with the correct permission set of M3.
    Set<SootMethod> methods = new HashSet<SootMethod>();
    int startIndex = stack.indexOf(sm);
    for (int i=startIndex+1; i<stack.size(); i++)
      methods.add(stack.elementAt(i));
    
    CycleKey ck = new CycleKey(sm, stack.size()+1); // +1 since sm is not in the stack
    if (cycle2Methods.keySet().contains(ck)){
      cycle2Methods.get(ck).addAll(methods);
    }
    else{
      cycle2Methods.put(ck, methods);
    }
    cache.addAll(methods);
    System.out.println("[cycle] add at depth '"+ stack.size() +"' for method '"+ sm +"'");

  }
  
  /**
   * 
   * @param stack
   * @param sm
   */
  public void addStackToCycle(Stack<SootMethod> stack, SootMethod sm) {
    Set<SootMethod> methods = new HashSet<SootMethod>();
    int startIndex = stack.indexOf(sm);
    for (int i=startIndex+1; i<stack.size(); i++)
      methods.add(stack.elementAt(i));
    
    CycleKey ck = new CycleKey(sm, stack.size()+1); // +1 since sm is not in the stack
    if (!cycle2Methods.keySet().contains(ck)) {
      String s = "\n";
      for (int i=0; i<stack.size(); i++)
        s += i +"> "+ stack.elementAt(i) +"\n";
      throw new RuntimeException("error: cyclekey not in stack! "+ ck + s);
    }
    cycle2Methods.get(ck).addAll(methods);
    cache.addAll(methods);
    System.out.println("[cycle] add at depth '"+ stack.size() +"' for method '"+ sm +"'");
  }
  
//  // Check that sm in not within a cycle. If it 
//  // is, add sm to the set of the cycle so that
//  // it is later update with the correct permission
//  // set
//  public void ifMethodInCycleAddWholeStack(SootMethod sm, Stack<SootMethod> stack) {
//    for (SootMethod k: cycle2Methods.keySet()) {
//      if (sm.toString().equals(k.toString())) {
//        cycle2Methods.get(k).add(sm);
//        cycle2Methods.get(k).addAll(stack);
//      }
//      for (SootMethod m: cycle2Methods.get(k)) {
//        if (sm.toString().equals(m.toString())) {
//          cycle2Methods.get(k).add(sm);
//          cycle2Methods.get(k).addAll(stack);
//        }
//      }
//    }
//  }
  
  
//  // This method is called once the graph 
//  // has been explored. It corrects methods
//  // which call methods from cycles.
//  public void updateCycles(Map<String, Set<String>> methodToPermissionSet) {
//    for (SootMethod k: cycle2Methods.keySet()) {
//      Set<String> pSet = methodToPermissionSet.get(k.toString());
//      for (SootMethod m: cycle2Methods.get(k))
//        methodToPermissionSet.get(m.toString()).addAll(pSet);
//    }
//  }
  
  public void updateCycle (SootMethod sm, Map<String, Set<String>> methodToPermissionSet, int depth) {
    CycleKey ck = new CycleKey(sm, depth);
    // if the key is not is the map, there is no cycle to update
    if (!(cycle2Methods.keySet().contains(ck)))
      return;
    // The key is in the map.
    // Check that the depth of the current method is > than
    // the key's depth
    CycleKey ckInMap = null;
    for (CycleKey k: cycle2Methods.keySet()) {
      if (k.equals(ck)) {
        ckInMap = k;
      }
    }
    if (ckInMap.depth() <= ck.depth())
      return;
    System.out.println("[cycle] update cycle depth '"+ depth +"' for method '"+ sm +"'");
    // get the real set of permissions
    Set<String> pSet = methodToPermissionSet.get(sm.toString());
    // add this set of permission(s) to all methods in the cycle
    for (SootMethod m: cycle2Methods.get(ckInMap))
      methodToPermissionSet.get(m.toString()).addAll(pSet);  
    // the cycle must be removed at this point
    cycle2Methods.remove(ckInMap);
    updateCache();
  }
  
  public void checkThatAllCyclesHaveBeenUpdated() {
    if (cycle2Methods.keySet().size() == 0)
      return;
    for (CycleKey ck: cycle2Methods.keySet()) {
      System.out.println("[cycle] remaining cycle starting at '"+ ck +"'");
      for (SootMethod mInCycle: cycle2Methods.get(ck))
        System.out.println("[cycle]  in cycle: '"+ mInCycle +"'");
    }
    System.out.flush();
    throw new RuntimeException("error: not all cycles were updated.");   
  }

  public boolean addCycleIfNecessary(Stack<SootMethod> stack, SootMethod m) {
    // check if m is in the stack
    if (stack.contains(m)) {
      addCycle(stack, m);
      return true;
    }

    // check if m is in cache
    if (!cache.contains(m))
      return false;
    
    // check if m is in registered cycles
    for (CycleKey k: cycle2Methods.keySet())
      for (SootMethod tgt: cycle2Methods.get(k))
        if (m.toString().equals(tgt.toString())) {
          addStackToCycle(stack, k.bottomMethod()); // add the stack to k, *not* m
          break;
        }
    
    return false;
  }
  
  private void updateCache() {
    cache = new HashSet<SootMethod>();
    for (CycleKey k: cycle2Methods.keySet())
      cache.addAll(cycle2Methods.get(k));
  }
  
}
