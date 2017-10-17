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
import java.util.Map;
import java.util.Set;

import lu.uni.rrc.Util;
import soot.Body;
import soot.PatchingChain;
import soot.Scene;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.SootMethodRef;
import soot.Unit;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.InvokeExpr;
import soot.jimple.Jimple;
import soot.jimple.Stmt;

public class RedirectServiceManager {
    public void redirectGetSystemServiceCalls(Body b, List<Unit> getSSStmt,
            List<String> sSNames, SootClass servicesInitClass) {
        if (getSSStmt.size() != sSNames.size()) {
            int callNbr = getSSStmt.size();
            int namesNbr = sSNames.size();
            throw new RuntimeException("[Error] System Services get "
                    + "calls units should all correspond to a service name! "
                    + "calls: " + callNbr + " names: " + namesNbr);
        }
        PatchingChain<Unit> units = b.getUnits();
        int i = 0;
        for (Unit u : getSSStmt) {
            // must be an assignment of the type: r =
            // c.getSystemService("serviceName")
            if (!(u instanceof AssignStmt)) {
                System.out.println("warning: must be assign statement! "
                        + "Current statement is: " + u);
                continue;
            }
            // current assignment statement
            AssignStmt ass = (AssignStmt) u;

            // create new assignment statement: replacement of call to
            // getSystemService("serviceID") by a call to
            // the static method "ServicesInit: Xyz getManager_serviceID()"
            if (sSNames.get(i) == null) {
                System.out.println("warning: service name is null!! FIXME PLEASE");
                continue;
            }
            String targetMethod = "getManager_" + sSNames.get(i);
            Unit newU = null;
            if (!servicesInitClass.declaresMethodByName(targetMethod)) {
                System.out.println("WARNING: servicesInit class does not contain "
                                + "method '" + targetMethod + "\n" + b);
                continue;
            } else {
                SootMethod method = servicesInitClass
                        .getMethodByName(targetMethod);
                List<Value> args = new ArrayList<Value>();
                Value rvalue = Jimple.v().newStaticInvokeExpr(method.makeRef(),
                        args);
                AssignStmt newAss = Jimple.v().newAssignStmt(ass.getLeftOp(),
                        rvalue);
                newU = (Unit) newAss;
            }
            System.out.println("swapping " + u + " with " + newU);
            units.swapWith(u, newU);

            i++;
        }

    }

    /**
     * To get a manager (not to be confused with getSystem)
     * 
     * @param b
     * @return
     */
    public List<Unit> hasCallToSystemServices(Body b) {
        List<Unit> calls = new ArrayList<Unit>();
        for (Unit u : b.getUnits()) {
            Stmt s = (Stmt) u;
            if (s.containsInvokeExpr()) {
                try {
                    InvokeExpr ie = s.getInvokeExpr();
                    String mName = ie.getMethodRef().name();
                    // System.out.println("m    : "+ ie.getMethodRef());
                    // System.out.println("mName: "+ mName);
                    if (mName.equals("getSystemService")) {
                        calls.add(u);
                    }
                } catch (Throwable t) {
                    continue;
                }
            }
        }
        return calls;
    }

    /**
     * To get a service (not to be confused with getSystemService)
     * 
     * @param b
     * @return
     */
    public List<Unit> hasCallToService(Body b) {
        List<Unit> calls = new ArrayList<Unit>();
        for (Unit u : b.getUnits()) {
            Stmt s = (Stmt) u;
            if (s.containsInvokeExpr()) {
                try {
                    InvokeExpr ie = s.getInvokeExpr();
                    String mName = ie.getMethodRef().name();
                    String cName = ie.getMethodRef().declaringClass().getName();
                    // System.out.println("m    : "+ ie.getMethodRef());
                    // System.out.println("mName: "+ mName);
                    if (mName.equals("getService")
                            && cName.equals("android.os.ServiceManager")) {
                        calls.add(u);
                    }
                } catch (Throwable t) {
                    continue;
                }
            }
        }
        return calls;
    }

    public void redirectInstanceOf(Body b) {
        Map<SootClass, Set<SootClass>> m = Util.concreteServicesToInterfaces;
        for (Unit u : b.getUnits()) {
            Stmt s = (Stmt) u;
            if (s.containsInvokeExpr()) {
                InvokeExpr ie = s.getInvokeExpr();
                SootMethodRef smr = ie.getMethodRef();
                if (smr.name().equals("asInterface")) {
                    //
                    // replace r1 = asInterface( r2 ) by
                    // r1 = r2
                    //
                    String intfName = smr.returnType().toString();
                    System.out.println("asInterface: " + s);
                    boolean gotIt = false;
                    for (SootClass k : m.keySet()) {
                        for (SootClass e : m.get(k)) {
                            if (e.getName().equals(intfName)) {
                                System.out.println(" [*] got: " + e);
                                gotIt = true;
                            }
                        }
                    }
                    if (!gotIt) {
                        System.out.println(" [*] Did NOT get for " + intfName);
                    } else {
                        String old = u.toString();
                        if (!(u instanceof AssignStmt)) {
                            System.out
                                    .println("WARNING: "
                                            +
                                    "expected assignment stmt for asInterface: "
                                            + b);
                            return;
                        }
                        AssignStmt ass = (AssignStmt) u;
                        InvokeExpr ie_r = (InvokeExpr) ass.getRightOp();
                        ass.setRightOp(ie_r.getArg(0));
                        System.out.println(" [*] replaced " + old + " by " + u);
                    }
                } else if (smr.name().equals("getService")) {
                    //
                    // replace ServiceManager.getService("service") by
                    // ServicesInit."reference to service"
                    //
                    if (smr.declaringClass().toString()
                            .equals("android.os.ServiceManager")) {
                        String serviceName = Util.getStringName(b, u, 0);
                        System.out.println("getSystem: " + serviceName);
                        boolean found = false;

                        for (String sname : HandleServices.getInitServicesMap().keySet()) {
                            if (serviceName.equals(sname)) {
                                found = true;
                                System.out.println("found (other service)! " + sname);
                            }
                        }
                        if (!found) {
                            System.out.println("WARNING: not found '"
                                    + serviceName);
                        } else {
                            String old = u.toString();
                            SootClass servicesInitClass = Scene
                                    .v()
                                    .getSootClass(
                                            GenerateServiceInit.servicesInitClassName);
                            SootField sf = servicesInitClass
                                    .getFieldByName(serviceName);
                            if (!(u instanceof AssignStmt)) {
                                System.out
                                        .println("WARNING: "
                                                +
                                        "expected assignment stmt for getService: "
                                                + b);
                                continue;
                            }
                            AssignStmt ass = (AssignStmt) u;
                            ass.getRightOpBox().setValue(
                                    Jimple.v().newStaticFieldRef(sf.makeRef()));
                            System.out.println(" [*] replaced " + old + " by "
                                    + u);
                        }
                    }
                }

            }
        }

    }



}
