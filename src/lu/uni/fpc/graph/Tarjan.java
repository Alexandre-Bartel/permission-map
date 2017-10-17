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

package lu.uni.fpc.graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Type;
import soot.VoidType;
import soot.jimple.Jimple;
import soot.jimple.Stmt;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;



public class Tarjan {
  
  private static Logger logger = LoggerFactory.getLogger(lu.uni.fpc.graph.Tarjan.class);

  private int index = 0;
  private List<SootMethod> stack = new ArrayList<SootMethod>();
  private List<List<SootMethod>> scc = new ArrayList<List<SootMethod>>();
  private Map<SootMethod, Integer> indexes = new HashMap<SootMethod, Integer>();
  private Map<SootMethod, Integer> lowlinks = new HashMap<SootMethod, Integer>();

  /* The funtion tarjan has to be called for every unvisited node of the graph */
  public List<List<SootMethod>> executeTarjan(CallGraph graph){
    scc.clear();
    index = 0;
    stack.clear();
    SootMethod mainMethod = Scene.v().getMethod("<MainClass: void main(java.lang.String[])>");
    tarjan(mainMethod, graph);
    return scc;
  }

  private int getindex(SootMethod m) {
    if (indexes.containsKey(m))
      return indexes.get(m).intValue();
    return -1;
  }
  private int getlowlink(SootMethod m) {
    if (lowlinks.containsKey(m))
      return lowlinks.get(m).intValue();
    return -1;
  }
  private List<List<SootMethod>> tarjan(SootMethod v, CallGraph cg){
    indexes.put(v, new Integer(index));//v.index = index;
    lowlinks.put(v, new Integer(index));//v.lowlink = index;
    index++;
    stack.add(0, v);
    Iterator<Edge> it = cg.edgesOutOf(v);
    while(it.hasNext()){
      Edge e = it.next();
      SootMethod n = e.tgt();
      if(getindex(n) == -1){
        tarjan(n, cg);
        lowlinks.put(v, Math.min(getlowlink(v), getlowlink(n)));
      }else if(stack.contains(n)){
        lowlinks.put(v, Math.min(getlowlink(v), getindex(n)));
      }
    }
    if(getlowlink(v) == getindex(v)){
      SootMethod n;
      ArrayList<SootMethod> component = new ArrayList<SootMethod>();
      do{
        n = stack.remove(0);
        component.add(n);
      }while(n != v);
      scc.add(component);
    }
    return scc;
  }

  static int dumbMethodId = 0;
  /**
   * Replace every strongly connected component by a single SootMethod
   * @param cg
   * @param sccList
   * @param methodToPermissionSet 
   */
  public void removeSCC(CallGraph cg, List<List<SootMethod>> sccList, Set<SootMethod> wMethods, Set<SootMethod> epMethods, Map<SootMethod, Set<String>> methodToPermissionSet) {
    // add a new class to the Scene
    // this class contains all methods which replace SCCs
    SootClass dumbSC = new SootClass("DumbClass");
    Scene.v().addClass(dumbSC);
    
    // go through each Strongly Connected Component
    int i = 0;
    for (List<SootMethod> scc: sccList) { 
      
      // check that to edge links back to main method
      // TODO: delete this check
      int count = 0;
      Iterator<Edge> itmain = cg.edgesInto(Scene.v().getMainMethod());
      while(itmain.hasNext()) {
        Edge e = itmain.next();
        System.out.println("edge to main: "+ e);
        count++;
      }
      if (count > 0) {
        throw new RuntimeException("error edge to main method!");
      }
      
      System.out.println("handling scc "+ i++);
      
      // do not handle SCC with only one node
      // TODO: on node could cycle on itself. This case is handled later but
      // really should be handled here
      if (scc.size() <= 1)
        continue;
      
      for (SootMethod sm: scc)
        System.out.println("    "+ sm);
      
      // create new method node to replace SCC
      SootMethod dumbSM = new SootMethod("DumbMethod"+ dumbMethodId++, new ArrayList<Type>(), VoidType.v());
      dumbSC.addMethod(dumbSM);
      
      for (SootMethod sm: scc) {
        
        if (wMethods.contains(sm)) {
          System.out.println("weird: wrapper in scc: "+ sm);
        }
        
        Iterator<Edge> it = cg.edgesInto(sm);
        System.out.println("while1");
        while (it.hasNext()) {
          Edge e = it.next();
          SootMethod inMethod = e.src();
          if (epMethods.contains(sm)) {// we want to keep entry point methods
            continue;
          }
          if (wMethods.contains(inMethod)) {
            System.out.println("warning: wrapper method detected but not entry point method!");
            System.out.println("wrapper: "+ inMethod);
            System.out.println("ep     : "+ sm);
          }
          if (scc.contains(inMethod))
            continue;
          Stmt fakeStmt = createFakeStmt(dumbSM);
          Edge newEdge = new Edge(inMethod, fakeStmt, dumbSM);
          cg.addEdge(newEdge);
          System.out.println("add edge in: "+ newEdge);
          
          //         cg.removeEdge(e);
          //         System.out.println("remove edge 1: "+ e);
        }
        it = cg.edgesOutOf(sm);
        System.out.println("while2");
        while (it.hasNext()) {
          Edge e = it.next();
          SootMethod outMethod = e.tgt();
          if (epMethods.contains(sm)) { // we keep entry point methods
            Stmt fakeStmt = createFakeStmt(outMethod);
            Edge newEdge = new Edge(sm, fakeStmt, dumbSM);
            cg.addEdge(newEdge);
            System.out.println("add edge out to protect entry point: "+ newEdge);
            continue;
          }
          if (epMethods.contains(outMethod))
          if (scc.contains(outMethod))
            continue;
          Stmt fakeStmt = createFakeStmt(outMethod);
          Edge newEdge = new Edge(dumbSM, fakeStmt, outMethod);
          cg.addEdge(newEdge);
          System.out.println("add edge out: "+ newEdge);
          
          //         cg.removeEdge(e);
          //         System.out.println("remove edge 2: "+ e);
        }
      }

      
      // permission from nodes of the SCC must be in the new method
      // permission list
      for (SootMethod sm: scc) {
        if (methodToPermissionSet.containsKey(sm)) {
          Set<String> permissions = methodToPermissionSet.get(sm);
          if (permissions.size() == 0)
            continue;
          if (methodToPermissionSet.containsKey(dumbSM)) {
            methodToPermissionSet.get(dumbSM).addAll(permissions);
          } else {
            methodToPermissionSet.put(dumbSM, permissions);
          }
        }
      }
      
      // delete all methods except entry point methods
      System.out.println("deleting edges");
      for (SootMethod sm: scc) {
        Iterator<Edge> it = cg.edgesInto(sm);
        while (it.hasNext()) {
          Edge e = it.next();
          if (epMethods.contains(sm)) { // we keep entry point methods
            if (!scc.contains(e.src()))
              continue;
          }
          cg.removeEdge(e);
          System.out.println("removing edge in: "+ e);
        }
        it = cg.edgesOutOf(sm);
        while (it.hasNext()) {
          Edge e = it.next();
          if (epMethods.contains(sm)) { // we keep entry point methods
            if (!scc.contains(e.tgt()))
              continue;
          }
          cg.removeEdge(e);
          System.out.println("removing edge out: "+ e);
        }
      }


    }
  }


  private Stmt createFakeStmt (SootMethod sm) {
    Stmt s = null;
    SootClass sc = sm.getDeclaringClass();
    if (sm.isStatic()) {
      s = Jimple.v().newInvokeStmt(Jimple.v().newStaticInvokeExpr(sm.makeRef()));
    } else {
      if (sc.isInterface()) {
        s = Jimple.v().newInvokeStmt(Jimple.v().newInterfaceInvokeExpr(Jimple.v().newLocal("a", VoidType.v()), sm.makeRef()));
      } else {
        s = Jimple.v().newInvokeStmt(Jimple.v().newVirtualInvokeExpr(Jimple.v().newLocal("a", VoidType.v()), sm.makeRef()));
      }
    }         
    return s;
  }

}