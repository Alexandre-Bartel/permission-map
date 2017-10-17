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

package lu.uni.rrc.services;

import java.util.ArrayList;
import java.util.List;

import soot.Body;
import soot.PatchingChain;
import soot.SootClass;
import soot.SootField;
import soot.Unit;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.InvokeExpr;
import soot.jimple.Jimple;
import soot.jimple.NullConstant;
import soot.jimple.Stmt;

public class RedirectService {
  public void redirectGetSystemServiceCalls(Body b, List<Unit> getSSStmt, List<String> sSNames, SootClass servicesInitClass) {
    if (getSSStmt.size() != sSNames.size()) {
      int callNbr = getSSStmt.size();
      int namesNbr = sSNames.size();
      throw new RuntimeException("[Error] System Services get calls units should all correspond to a service name! calls: "+ callNbr +" names: "+ namesNbr);
    }
    
    PatchingChain<Unit> units = b.getUnits();
    int i = 0;
    for (Unit u: getSSStmt) {
      // must be an assignment of the type: r = c.getSystemService("serviceName")
      if (!(u instanceof AssignStmt)) {
        throw new RuntimeException("[Error] must be assign statement! Current statement is: "+ u);
      }
      // current assignment statement
      AssignStmt ass = (AssignStmt)u;
      
      // create new assignment statement: replacement of call to getSystemService by a reference to
      // the static field ref to the service (which is created in a custom class called ServicesInit.java)
      //RefType rt = RefType.v("ServicesInit");
      Unit newU = null;
      if (!servicesInitClass.declaresFieldByName(sSNames.get(i))) {
        System.out.println("[Warning] servicesInit class does not contain field '"+ sSNames.get(i) +"' replacing statement by nop.");
        newU = Jimple.v().newAssignStmt(ass.getLeftOp(), NullConstant.v());//Jimple.v().newNopStmt();
      } else {
        SootField sf = servicesInitClass.getFieldByName(sSNames.get(i)); //new SootField(sSNames.get(i), rt, Modifier.STATIC);
        Value rvalue = Jimple.v().newStaticFieldRef(sf.makeRef());
        AssignStmt newAss = Jimple.v().newAssignStmt(ass.getLeftOp(), rvalue);
        newU = (Unit) newAss;
      }
      System.out.println("swapping "+ u +" with "+ newU);
      units.swapWith(u, newU);
 
      i++;
    }
    
  }
  
  /**
   * To get a service (not to be confused with getSystemService)
   * @param b
   * @return
   */
  public List<Unit> hasCallToGetSystem(Body b) {
    List<Unit> calls = new ArrayList<Unit>();
    for (Unit u: b.getUnits()) {
      Stmt s = (Stmt)u;
      if (s.containsInvokeExpr()) {
        try {
        InvokeExpr ie = s.getInvokeExpr();
        String mName = ie.getMethodRef().name();
        //System.out.println("m    : "+ ie.getMethodRef());
        //System.out.println("mName: "+ mName);
                    if (mName.equals("getService") && ie.getArgs().size() > 0) {
          calls.add(u);
        }
        } catch (Throwable t) {
          continue;
        }
      }
    }
    return calls;
  }


}
