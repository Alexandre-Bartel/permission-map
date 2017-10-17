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
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lu.uni.fpc.EntryPointKey;
import lu.uni.fpc.Util;
import soot.Scene;
import soot.SootMethod;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;

public class ForwardSearch {
  
  private static Logger logger = LoggerFactory.getLogger(ForwardSearch.class);
  
  private static Set<SootMethod> alreadyComputed = new HashSet<SootMethod>();
  private static Map<SootMethod, Set<String>> methodToPermissionSet = null;
  private static Map<EntryPointKey, Set<String>> entryPointsToPermissionSet = new HashMap<EntryPointKey, Set<String>>();
  
  public static void searchPermissions(CallGraph cg, Map<SootMethod, Set<String>> methodToPermissionSet) {
    ForwardSearch.methodToPermissionSet = methodToPermissionSet;
//    Set<SootMethod> checkGEOnePermn = new HashSet<SootMethod>(); // methods which check at least one permission
//    for (SootMethod sm: methodToPermissionSet.keySet()) {
//      if (methodToPermissionSet.get(sm).size() == 0)
//        continue;
//      checkGEOnePermn.add(sm);
//      alreadyComputed.add(sm);
//      System.out.println("already computed: "+ sm);
//    }
    SootMethod main = Scene.v().getMainMethod();
    Stack<SootMethod> stack = new Stack<SootMethod>();
    stack.push(main);
    search(cg, main, stack, 0);
    computeEntryPointPermissions();
    
  }
  
  private static void computeEntryPointPermissions() {
    for (EntryPointKey k: entryPointsToPermissionSet.keySet()) {
      SootMethod ep = k.getEp();
      Set<String> permissions = methodToPermissionSet.get(ep);
      if (permissions == null)
        throw new RuntimeException("error: not permission set for method "+ ep);
      entryPointsToPermissionSet.put(k, permissions);
    }
  }
  
  public static Map<EntryPointKey, Set<String>> getEntryPointsToPermissionSet() {
    return entryPointsToPermissionSet;
  }

  private static SootMethod currentWrapper = null;
  private static SootMethod currentEntryPoint = null;
  private static void search(CallGraph cg, SootMethod l, Stack<SootMethod> stack, int depth) {
    logger.debug(" <"+ depth +"> propagate from "+ l);
    
    if (depth == 1)
      currentWrapper = l;
    
    if (depth == 2) {
      currentEntryPoint = l;
      if (currentEntryPoint.toString().contains("GenerationGG") ||
          //currentEntryPoint.toString().contains("init>") ||
          currentEntryPoint.toString().contains("ServicesInit:")) {
            
          } else {
            if (!currentEntryPoint.toString().contains("DumbClass:")) {    
      EntryPointKey epk = new EntryPointKey(currentWrapper, currentEntryPoint);
      entryPointsToPermissionSet.put(epk, null);
            }
          }
    }
    
    if (alreadyComputed.contains(l)) {
      addPermissionsToStack(stack, l);
      return;
    }
    
    
    Iterator<Edge> it = cg.edgesOutOf(l);
    while (it.hasNext()) {
      Edge e = it.next();
      SootMethod outMethod = e.tgt();
      
      if (outMethod.toString().equals(l.toString())){
        logger.warn("outMethod same as l: "+ l);
        continue;
      }
      if (stack.contains(outMethod))
        throw new RuntimeException("cycle in stack!\n" + outMethod +"\n"+ Util.printStack(stack));
      
      addPermissionsToStack(stack, outMethod);
      
      stack.push(outMethod);
      search(cg, outMethod, stack, depth+1);
      stack.pop();
    }
    
    alreadyComputed.add(l);
    if (!methodToPermissionSet.containsKey(l))
      methodToPermissionSet.put(l, new HashSet<String>());

  }
  
  
  private static void addPermissionsToStack(Stack<SootMethod> stack, SootMethod outMethod) {
    if (!methodToPermissionSet.containsKey(outMethod))
      return;
    Set<String> permissions = methodToPermissionSet.get(outMethod);
    if (permissions.size() == 0)
      return;
    for (int i = 0; i < stack.size(); i++) {
      SootMethod sm = stack.elementAt(i);
      if (methodToPermissionSet.containsKey(sm)) {
        methodToPermissionSet.get(sm).addAll(permissions);
      } else {
        methodToPermissionSet.put(sm, new HashSet<String>(permissions));
      }     
    }
  }
  
}
