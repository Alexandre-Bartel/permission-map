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
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import soot.Body;
import soot.Local;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.javaToJimple.LocalGenerator;
import soot.jimple.ArrayRef;
import soot.jimple.AssignStmt;
import soot.jimple.CastExpr;
import soot.jimple.DefinitionStmt;
import soot.jimple.FieldRef;
import soot.jimple.IdentityStmt;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.Jimple;
import soot.jimple.NewExpr;
import soot.jimple.Stmt;
import soot.jimple.ThisRef;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.SimpleLiveLocals;
import soot.toolkits.scalar.SimpleLocalUses;
import soot.toolkits.scalar.SmartLocalDefs;

public class HandleThreads {
	
	private static Logger logger = LoggerFactory.getLogger(HandleThreads.class);

    Set<SootMethod> mStart = null;
    Set<SootMethod> mRun = null;

    public static String threadRunSig = "<java.lang.Thread: void run()>";
    public static String threadStartSig = "<java.lang.Thread: void start()>";

    public static String threadInitSSig = "<java.lang.Thread: void <init>(";

    public HandleThreads(Set<SootMethod> mStart, Set<SootMethod> mRun) {
        this.mStart = mStart;
        this.mRun = mRun;
    }

    public void doWork() {
        for (SootMethod sm : mStart) {
            Body b = sm.retrieveActiveBody();

            // System.out.println("start: " + sm + "\n" + b);

            List<Unit> replaceList = new ArrayList<Unit>();

            for (Unit u : b.getUnits()) {
                Stmt s = (Stmt) u;
                if (s.containsInvokeExpr()) {
                    InvokeExpr ie = s.getInvokeExpr();
                    String ieSig = ie.getMethodRef().getSignature();
                    if (threadStartSig.equals(ieSig)) {
                        replaceList.add(u);
                    }
                }
            }

            for (Unit unit : replaceList) {
                replaceStart(b, unit);
            }

        }

        for (SootMethod sm : mRun) {
            // System.out.println("run: " + sm + "\n" +
            // sm.retrieveActiveBody());
        }

    }

    private void replaceStart(Body b, Unit unit) {
        System.out.println("replace start: " + unit);
        // Suppose we have r1.<Thread start()>
        // The first step is to find parameter p in
        // r1.<Thread <init>(p)>

        Stmt stmt = (Stmt) unit;
        InvokeExpr iexpr = stmt.getInvokeExpr();
        InstanceInvokeExpr iiexpr = (InstanceInvokeExpr) iexpr;
        Value base = iiexpr.getBase();

        Type bType = getTypeOfBase(b, (Local) base, unit);

        if (bType == null) {
            System.out.println("warning: null thread type, just removing start().");
        }

        Util.removeUnit(b, unit);
        return;

        // if (bType.toString().equals("java.lang.Thread")) {
        // System.out.println("find runnable type:");
        // // findRunnableType();
        // }
        //
        //
        //
        // // p is of type runnable, so the second step
        // // is to find the real type of p

    }

    private void findRunnableType(Body b, Unit newThread) {
        AssignStmt ass = (AssignStmt) newThread;
        Local base = (Local) ass.getLeftOp();

        Unit tu = null;
        Value runnableArg = null;
        for (Unit u : b.getUnits()) {
            Stmt s = (Stmt) u;
            if (s.containsInvokeExpr()) {
                InvokeExpr ie = s.getInvokeExpr();
                String ieSig = ie.getMethodRef().getSignature();
                if (ieSig.startsWith(threadInitSSig)) {
                    InstanceInvokeExpr iie = (InstanceInvokeExpr) ie;
                    if (iie.getBase() == base) {
                        System.out.println("same base for init...");
                        int i = 0;
                        for (Type pt : iie.getMethod().getParameterTypes()) {
                            System.out.println("type: " + pt);
                            if (pt.toString().equals("java.lang.Runnable")) {
                                tu = u;
                                runnableArg = iie.getArg(i);
                                break;
                            }
                            i++;
                        }
                    }
                }
            }
        }

        // we do this outside the loop so we can add units
        if (tu != null && runnableArg != null && runnableArg instanceof Local) {
            findRunnableDef(b, (Local) runnableArg, tu);
        }
    }

    private void findRunnableDef(Body b, Local l, Unit unit) {
        final UnitGraph g = new ExceptionalUnitGraph(b);
        final SmartLocalDefs localDefs = new SmartLocalDefs(g, new SimpleLiveLocals(g));
        final SimpleLocalUses localUses = new SimpleLocalUses(g, localDefs);

        List<Unit> defs = localDefs.getDefsOfAt((Local) l, unit);
        Unit def = defs.get(0);
        if (def instanceof IdentityStmt) {
            IdentityStmt is = (IdentityStmt) def;
            Value r = is.getRightOp();
            if (r instanceof ThisRef) {
                Type t = r.getType();
                System.out.println("this ref type: " + t);
                Local base_1 = (Local) is.getLeftOp();
                SootMethod method_1 = Scene.v().getMethod("<" + t + ": void run()>");
                Unit newU = Jimple.v().newInvokeStmt(
                        Jimple.v().newVirtualInvokeExpr(base_1, method_1.makeRef()));
                System.out.println("inserting " + newU);
                insertAfter(b, newU, def);
            } else {
                System.out.println("warning: not handling runnable from parameter!" + def);
            }
        } else if (def instanceof AssignStmt) {
            AssignStmt ass = (AssignStmt) def;
            Value r = ass.getRightOp();
            if (r instanceof CastExpr) {
                CastExpr ce = (CastExpr) r;
                findRunnableDef(b, (Local) ce.getOp(), ass);
            } else if (r instanceof Local) {
                findRunnableDef(b, (Local) r, ass);
            } else if (r instanceof FieldRef) {
                FieldRef fr = (FieldRef) r;
                // AssignStmt fdef = findFieldDef(b, fr);
                // System.out.println("warning: not handling field ref for find runnable!");
                // Type t = ne.getType();

                Type t = fr.getType();
                SootClass sc = Scene.v().getSootClass(t.toString());
                if (sc.isInterface()) {
                    System.out
                            .println("warning: not generating field redirection for interface type "
                                    + sc);
                    return;
                }
              
                System.out.println("field type: " + t);

                LocalGenerator lg = new LocalGenerator(b);
                Local base_1 = lg.generateLocal(t);
                Unit fu = Jimple.v().newAssignStmt(base_1, fr);

                String targetMethod = "<" + t + ": void run()>";
                if (!Scene.v().containsMethod(targetMethod)) {
                	System.out.println("warning: scene does not contain method: "+ targetMethod);
                	return;
                }
                SootMethod method_1 = Scene.v().getMethod(targetMethod);
                Unit newU = Jimple.v().newInvokeStmt(
                        Jimple.v().newVirtualInvokeExpr(base_1, method_1.makeRef()));
                System.out.println("inserting " + fu);
                System.out.println("inserting " + newU);
                insertAfter(b, fu, def);
                insertAfter(b, newU, def);
            } else if (r instanceof ArrayRef) {
                System.out.println("warning: not handling array ref for find runnable!");
            } else if (r instanceof NewExpr) {
                NewExpr ne = (NewExpr) r;
                Type t = ne.getType();
                System.out.println("runnable type: " + t);
                Local base_1 = (Local) ass.getLeftOp();
                SootMethod method_1 = Scene.v().getMethod("<" + t + ": void run()>");
                Unit newU = Jimple.v().newInvokeStmt(
                        Jimple.v().newVirtualInvokeExpr(base_1, method_1.makeRef()));
                System.out.println("inserting " + newU);
                insertAfter(b, newU, def);
            }
        } else {
            throw new RuntimeException("error: unexpected def: " + def);
        }
    }

    private Type getTypeOfBase(Body b, Local base, Unit unit) {
        final UnitGraph g = new ExceptionalUnitGraph(b);
        final SmartLocalDefs localDefs = new SmartLocalDefs(g, new SimpleLiveLocals(g));
        final SimpleLocalUses localUses = new SimpleLocalUses(g, localDefs);

        List<Unit> defs = localDefs.getDefsOfAt((Local) base, unit);
        Unit def = defs.get(0);
        // check if base is @this
        if (def instanceof IdentityStmt) {
            DefinitionStmt d = (DefinitionStmt) def;
            IdentityStmt idstmt = (IdentityStmt) def;
            Value r = idstmt.getRightOp();
            if (r instanceof ThisRef) {
                ThisRef tr = (ThisRef) r;
                Type t = tr.getType();
                System.out.println("found this type: " + t);
                Local base_1 = (Local) d.getLeftOp();
                SootMethod method_1 = Scene.v().getMethod("<" + t + ": void run()>");
                Unit newU = Jimple.v().newInvokeStmt(
                        Jimple.v().newVirtualInvokeExpr(base_1, method_1.makeRef()));
                System.out.println("inserting " + newU);
                insertAfter(b, newU, def);
                return t;
            } else {
                System.out.println("warning: not handling parameter: " + d);
                return null;
            }
        } else if (def instanceof AssignStmt) {
            AssignStmt ass = (AssignStmt) def;
            Value r = ass.getRightOp();
            if (r instanceof FieldRef) {
                // check if base is from field (then find how field is
                // initialized)
                System.out.println("thread from field...");
                FieldRef fr = (FieldRef) r;
                AssignStmt fdef = findFieldDef(b, fr);
                if (fdef == null) {
                    System.out.println("warning: no def for field " + fr);
                    return null;
                }
                System.out.println("fdef: " + fdef);
                Value v = fdef.getRightOp();
                if (!(v instanceof Local))
                	return null;
                Local flocal = (Local) v; 
                return getTypeOfBase(b, flocal, fdef);
            } else if (r instanceof NewExpr) {
                // check if base in Thread
                NewExpr ne = (NewExpr) r;
                Type net = ne.getType();
                if (net.toString().equals("java.lang.Thread")) {
                    System.out.println("found thread type: " + net);
                    findRunnableType(b, ass);
                    return net;
                } else {
                    System.out.println("found non-thread type: " + net);
                    // get sootclass
                    SootClass netSC = Scene.v().getSootClass(net.toString());
                    if (netSC.getSuperclass().toString().equals("android.os.HandlerThread")) {
                    	logger.warn("HandlerThreads are not handled yet!!!");
                    	return net;
                    } else if (!netSC.getSuperclass().toString().equals("java.lang.Thread")) {
                        logger.warn("Classes not implementing threads directly are not handled yet!!!");
                        return net;
                    }
                    
                    // replace start by run on new initialized object of type
                    // 'net'
                    Local base_1 = (Local) ass.getLeftOp();
                    SootMethod method_1 = Scene.v().getMethod("<" + net + ": void run()>");
                    Unit newU = Jimple.v().newInvokeStmt(
                            Jimple.v().newVirtualInvokeExpr(base_1, method_1.makeRef()));
                    System.out.println("inserting " + newU);
                    insertAfter(b, newU, def);
                    return net;
                }
            } else if (r instanceof Local) {
                return getTypeOfBase(b, (Local) r, ass);
            } else {
                System.out.println("warning: not handling " + ass);
                return null;
            }

        } else {
            throw new RuntimeException("error: unexpected def: " + def);
        }
    }

    private AssignStmt findFieldDef(Body b, FieldRef fr) {
        AssignStmt fdef = null;
        for (Unit u : b.getUnits()) {
            if (u instanceof AssignStmt) {
                AssignStmt fass = (AssignStmt) u;
                Value l = fass.getLeftOp();
                if (l instanceof FieldRef) {
                    FieldRef ffr = (FieldRef) l;
                    if (ffr.getField().getSignature().toString()
                            .equals(fr.getField().getSignature())) {
                        fdef = fass;
                        break;
                    }
                }
            }
        }
        return fdef;
    }

    private void insertAfter(Body b, Unit newU, Unit u) {
        Unit afterThisU = u;
        // we suppose there is at least one instruction after
        // the identity statements
        while (afterThisU instanceof IdentityStmt) {
            u = afterThisU;
            afterThisU = b.getUnits().getSuccOf(afterThisU);
        }
        b.getUnits().insertAfter(newU, u);
    }

}
