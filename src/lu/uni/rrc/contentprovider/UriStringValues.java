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

package lu.uni.rrc.contentprovider;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import soot.Body;
import soot.Local;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.IdentityStmt;
import soot.jimple.InvokeExpr;
import soot.jimple.NullConstant;
import soot.jimple.Stmt;
import soot.jimple.StringConstant;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.scalar.SimpleLiveLocals;
import soot.toolkits.scalar.SimpleLocalUses;
import soot.toolkits.scalar.SmartLocalDefs;


public class UriStringValues  {

  public static Set<String> findStringForUriAt (Body b, Unit u) {
    
    Set<String> strSet = new HashSet<String>();
    
    // check that u contains a method invocation with an android.net.Uri parameter
    Stmt s = (Stmt)u;
    if (!s.containsInvokeExpr())
      throw new RuntimeException("statement does not contains invoke expr! "+ s);
    Value v = null;
    int i = -1;
    for (Type t: (List<Type>)s.getInvokeExpr().getMethodRef().parameterTypes()) {
      i++;
      if (t.toString().equals("android.net.Uri"))
        v = s.getInvokeExpr().getArg(i);
    }
    if (v == null)
      throw new RuntimeException("none of invoke expression parameters is of Uri type! "+ s);
    if (v instanceof StringConstant) {
      strSet.add(((StringConstant)v).value);
      return strSet;
    } else if (v instanceof NullConstant) {
      strSet.add("nullConstant");
      return strSet;
    } else if (!(v instanceof Local)) {
      throw new RuntimeException("error: v not instance of Local! "+ v +" "+ v.getClass());
    }
    Local l = (Local)v;
    
    
    // find definitions of l
    final ExceptionalUnitGraph g = new ExceptionalUnitGraph(b);
    final SmartLocalDefs localDefs = new SmartLocalDefs(g, new SimpleLiveLocals(g));
    final SimpleLocalUses localUses = new SimpleLocalUses(g, localDefs);
    
    List<Unit> defs = localDefs.getDefsOfAt(l, u);
    for (Unit d: defs) {
      System.out.println("   def: "+ d);
      Stmt stmt = (Stmt)u;
      if (stmt.containsFieldRef()) {
        System.out.println("warning: cannot handle field stmt: "+ stmt);
      } else if (stmt.containsInvokeExpr()) {
        InvokeExpr ie = stmt.getInvokeExpr();
        String methodSig = ie.getMethod().toString();
        String parseMethodSig = "<android.net.Uri: android.net.Uri parse(java.lang.String)>";
        String getUriForSecureMethodSig = "<android.provider.Settings$Secure: android.net.Uri getUriFor(java.lang.String)>";
        String getUriForSystemMethodSig = "<android.provider.Settings$System: android.net.Uri getUriFor(java.lang.String)>";
        if (methodSig.contains(parseMethodSig) || 
            methodSig.contains(getUriForSystemMethodSig) || 
            methodSig.contains(getUriForSecureMethodSig)) {
          Value a = ie.getArg(0);
          if (a instanceof StringConstant) {
            strSet.add(((StringConstant)a).value);
          } else if (a instanceof Local){
            List<Unit> defsA = localDefs.getDefsOfAt((Local) a, stmt);
            boolean hasStrCst = false;
            for (Unit unit: defsA) {
              if (unit instanceof AssignStmt) {
                AssignStmt ass = (AssignStmt)unit;
                if (ass.getRightOp() instanceof StringConstant) {
                  strSet.add(((StringConstant)ass.getRightOp()).value);
                  hasStrCst = true;
                }
              }
            }
            if (!hasStrCst) {
              System.out.println("warning: did not find str cst: "+ stmt);
            }
          } else {
            throw new RuntimeException("error: no String nor Local: "+ stmt);
          }
        } else {
          System.out.println("warning: do not handle method stmt: "+ stmt);
        }
      } else if (stmt instanceof AssignStmt) {
        AssignStmt ass = (AssignStmt)stmt;
        Value r = ass.getRightOp();
        if (r instanceof StringConstant) {
          StringConstant c = (StringConstant)r;
          strSet.add(c.value);
        }
      } else if (stmt instanceof IdentityStmt) {
        System.out.println("warning: cannot handle identity stmt: "+ stmt);
      }
    }
    
    return strSet;
  }


}
