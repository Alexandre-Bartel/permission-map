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

package lu.uni.fpc.identity;

import java.util.ArrayList;
import java.util.List;

import soot.Body;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.UnitGraph;

public class ClearRestoreCallingIdentity {

  public static List<Edge> removeBetweenClearAndRestoreIdentity(SootMethod m, CallGraph cg) {
    Body b = m.retrieveActiveBody();
    UnitGraph ug = new BriefUnitGraph(b);
    
    List<Unit> unitInBetween = new ArrayList<Unit>();
    List<Unit> methodCalls = new ArrayList<Unit>();
    List<Edge> methodCallsToKeep = new ArrayList<Edge>();
    
    // get clearIdentity node
    for (Unit u: b.getUnits()) {
      Stmt s = (Stmt)u;
      if (s.containsInvokeExpr()) {
        methodCalls.add(u);
        InvokeExpr ie = s.getInvokeExpr();
        if (ie.getMethodRef().name().contains("clearCallingIdentity")) {
          System.out.println("[IDInversion] body: "+ b);
          getUnitInBetween(ug, unitInBetween, u);
        }
      }
    }
    for (Unit u: unitInBetween) {
      System.out.println("[IDInversion] unit to remove:"+ u);
    }
    // keep only method outside of clear/restore
    for (Unit u: methodCalls) {
      Stmt s = (Stmt)u;
      InvokeExpr ie = s.getInvokeExpr();
      if (ie.getMethodRef().name().contains("clearCallingIdentity")) {
        continue;
      } else if (ie.getMethodRef().name().contains("restoreCallingIdentity")) {
        continue;
      } else if (unitInBetween.contains(u)) {
        System.out.println("[IDInversion] delete out method '"+ ie.getMethodRef() +"'");
        continue;
      } else {
        //retrieveAllEdgesFromUnits
        System.out.println("[IDInversion] keep out method '"+ ie.getMethod() +"'");
        //Set<Edge> edgesOutOfUnitSet = retrieveAllEdgesOutOfUnit(m, u, ie, cg);
        methodCallsToKeep.add(new Edge(m, (Stmt) u, ie.getMethod()));//All(edgesOutOfUnitSet);
      }
    }
    return methodCallsToKeep;
  }

//  private static Set<Edge> retrieveAllEdgesOutOfUnit(SootMethod m, Unit u, InvokeExpr ie, CallGraph cg) {
//    Set<Edge> set = new HashSet<Edge>();
//    Iterator<Edge> it = cg.edgesOutOf(ie.getMethod());
//    System.out.println("[IDInversion] edges out of method '"+ ie.getMethod() +"'");
//    while (it.hasNext()) {
//      Edge e = it.next();      
//      System.out.println("[IDInversion] tgt: "+ e.tgt());
//      Edge newE = new Edge(m, (Stmt) u, e.tgt());
//      set.add(newE);
//    }
//    return set;
//  }

  private static void getUnitInBetween(UnitGraph ug, List<Unit>inBetween, Unit u) {

    for (Unit succ: ug.getSuccsOf(u)) {
      Stmt s = (Stmt)succ;
      if (inBetween.contains(succ)) {
        continue;
      }
      if (s.containsInvokeExpr()) {
        InvokeExpr ie = s.getInvokeExpr();
        if (ie.getMethodRef().name().contains("restoreCallingIdentity")) {
          return;
        } 
      }
      inBetween.add(succ);
      getUnitInBetween(ug, inBetween, succ);
    }
  }
}
