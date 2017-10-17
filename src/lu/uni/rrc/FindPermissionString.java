//
// (c) 2014 TU Darmstadt
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

package lu.uni.rrc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import soot.Body;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.StringConstant;

public class FindPermissionString {

  List<SootMethod> containPermissionString = new ArrayList<SootMethod>();
  HashMap<String, List<SootMethod>> permissionToMethods = new HashMap<String, List<SootMethod>>();
  HashMap<SootMethod, List<Unit>> methodToUnits = new HashMap<SootMethod, List<Unit>>();
  HashMap<String, List<SootMethod>> unittypeToMethods = new HashMap<String, List<SootMethod>>();
  
  public void find() {
    for (SootClass sc: Scene.v().getClasses()) {
      String scName = sc.getName();
      if (!(scName.startsWith("android") || sc.getName().startsWith("com.android"))) {
        continue;
      } 

      System.out.println("processing class "+ sc);
      for (SootMethod sm: sc.getMethods()) {
        if (sm.isConcrete()) {
          Body b = null;
          try {
            b = sm.retrieveActiveBody();
          } catch (Exception e) {
            System.out.println("[E] exception when retrieving body for method : "+ sm +" skipping...");
            continue;
          }
          for (Unit u: b.getUnits()) {
            Stmt s = (Stmt)u;
            if (s instanceof AssignStmt) {
              AssignStmt ass = (AssignStmt)s;
              if (ass.getRightOp() instanceof StringConstant) {
                StringConstant c = (StringConstant)ass.getRightOp();
                String v = c.value;
                if (v.startsWith("android.permission.")) {
                  addPermission( v, sm, b, u);
                  printBody(b);
                }
              } else if (ass.getRightOp() instanceof InvokeExpr){
                InvokeExpr ie = (InvokeExpr)ass.getRightOp();
                String p = hasPermissionString(ie);
                if (p != null) {
                  addPermission( p, sm, b, u);
                  printBody(b);
                }
              }
            } else if (s.containsInvokeExpr()) {
              InvokeExpr ie = s.getInvokeExpr();
              String p = hasPermissionString(ie);
              if (p != null) {
                addPermission( p, sm, b, u);
                printBody(b);
              }
            }
          }
        }
      }
    }
    
    int methodsNbr = containPermissionString.size();
    int averageSize = 0;
    int maxSize = 0;
    int minSize = Integer.MAX_VALUE;
    System.out.println(methodsNbr +" methods use a permission string of the form \"android.permission.\"");
    for (SootMethod sm: containPermissionString) {
      int currSize = sm.retrieveActiveBody().getUnits().size();
      averageSize += currSize;
      if (currSize > maxSize)
        maxSize = currSize;
      if (minSize > currSize)
        minSize = currSize;
      System.out.println("   "+ currSize +": "+ sm);
    }
    averageSize = averageSize / methodsNbr;
    
    System.out.println();
    System.out.println("methods with permission string: "+ methodsNbr);
    System.out.println("minSize: "+ minSize);
    System.out.println("maxSize: "+ maxSize);
    System.out.println("avgSize: "+ averageSize);
    System.out.println();
    for (String unittype: unittypeToMethods.keySet()) {
      List<SootMethod> methods = unittypeToMethods.get(unittype);
      System.out.println(unittype +": "+ methods.size());
      for (SootMethod sm: methods) {
        System.out.println("  "+ sm);
      }
    }
    
  }

  private void printBody(Body b) {
//  System.out.println("found in assign stmt "+ sm);
    System.out.println();
    System.out.println(b);
    System.out.println();
  }

  private void addPermission(String p, SootMethod sm, Body b, Unit u) {
    //
    containPermissionString.add(sm);
    //
    if (permissionToMethods.containsKey(p)) {
      permissionToMethods.get(p).add(sm);
    } else {
      List<SootMethod> methods = new ArrayList<SootMethod>();
      methods.add(sm);
      permissionToMethods.put(p, methods);
    }
    //
    if (methodToUnits.containsKey(sm)) {
      methodToUnits.get(sm).add(u);
    } else {
      List<Unit> units = new ArrayList<Unit>();
      units.add(u);
      methodToUnits.put(sm, units);
    }
    //
    String unittype = null;
    Stmt s = (Stmt)u;
    if (s instanceof AssignStmt) {
      AssignStmt ass = (AssignStmt)s;
      unittype = ass.getLeftOp().getClass() +" = "+ ass.getRightOp().getClass();
    } else if (s.containsInvokeExpr()) {
      unittype = s.getClass().toString();
    } else {
      throw new RuntimeException("[E] unit should be assignement or contain invoke expr! : "+ u);
    }
    if (unittypeToMethods.containsKey(unittype)) {
      unittypeToMethods.get(unittype).add(sm);
    } else {
      List<SootMethod> methods = new ArrayList<SootMethod>();
      methods.add(sm);
      unittypeToMethods.put(unittype, methods);
    }
    //
  }

  private String hasPermissionString(InvokeExpr ie) {
    for (Value val: (List<Value>)ie.getArgs()) {
      if (val instanceof StringConstant) {
        StringConstant c = (StringConstant)val;
        String v = c.value;
        if (v.startsWith("android.permission.")) {
          return v;
        }
      } 
    }
    return null;
  }
  
}
