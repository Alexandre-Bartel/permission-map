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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import soot.Body;
import soot.FastHierarchy;
import soot.Local;
import soot.PrimType;
import soot.RefType;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.SootMethodRef;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.javaToJimple.LocalGenerator;
import soot.jimple.CastExpr;
import soot.jimple.ClassConstant;
import soot.jimple.DefinitionStmt;
import soot.jimple.FieldRef;
import soot.jimple.IdentityStmt;
import soot.jimple.InvokeExpr;
import soot.jimple.Jimple;
import soot.jimple.ParameterRef;
import soot.jimple.ReturnStmt;
import soot.jimple.Stmt;
import soot.jimple.StringConstant;
import soot.jimple.ThisRef;
import soot.jimple.internal.JAssignStmt;
import soot.jimple.internal.JIdentityStmt;
import soot.jimple.internal.JNewExpr;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.scalar.SimpleLiveLocals;
import soot.toolkits.scalar.SmartLocalDefs;
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
  
  public static String getUriString(Body b, Value arg) {
    // TODO Auto-generated method stub
    return null;
  }

    public static void removeUnit(Body b, Unit u) {
        LocalGenerator lg = new LocalGenerator(b);
        if (u instanceof DefinitionStmt) {
            DefinitionStmt def = (DefinitionStmt) u;
            Type t = def.getRightOp().getType();
            if (!(t instanceof PrimType)) {
                Local l_obj = lg.generateLocal(RefType.v("java.lang.Object"));
                Local l_type = lg.generateLocal(t);
                Unit u1 = Jimple.v().newAssignStmt(l_obj,
                        Jimple.v().newNewExpr(RefType.v("java.lang.Object")));
                Unit u2 = Jimple.v().newAssignStmt(l_type, Jimple.v().newCastExpr(l_obj, t));
                def.getRightOpBox().setValue(l_type);
                b.getUnits().insertBefore(u1, u);
                b.getUnits().insertBefore(u2, u);
                return;
            }
        }
        b.getUnits().remove(u);
    }

    // TODO: move those fields
    public static Map<SootClass, Set<SootClass>> concreteServicesToInterfaces = new HashMap<SootClass, Set<SootClass>>();
    public static Map<String, SootClass> serviceIdToServiceClass = new HashMap<String, SootClass>();
    /**
     * Returns a list of Map from service names to concrete Soot classes.
     * 
     * @param sm
     * @return
     */
    public static List<Map<String, SootClass>> getStringClassMappingForMethod(SootMethod sm,
            String methodStringObjSig) {
        List<Map<String, SootClass>> list = new ArrayList<Map<String, SootClass>>();
        
        if (!sm.isConcrete()) {
            throw new RuntimeException("error: not concrete method "+ sm);
        }

        Body b = null;
        try {
            b = sm.retrieveActiveBody();
        } catch (Exception e) {
            throw new RuntimeException("error: no active body for method "+ sm);
        }

        ExceptionalUnitGraph eug = new ExceptionalUnitGraph(b);
        final SmartLocalDefs localDefs = new SmartLocalDefs(eug, new SimpleLiveLocals(eug));
        for (Unit u : b.getUnits()) {
            Stmt s = (Stmt) u;

            if (!s.containsInvokeExpr()) {
                continue;
            }
            InvokeExpr ie = s.getInvokeExpr();
            SootMethodRef smr = ie.getMethodRef();
            String sig = smr.getSignature();

            if (!sig.equals(methodStringObjSig)) {
                continue;
            }
            
            System.out.println("statement: " + s);
            List<Unit> defs = new ArrayList<Unit>();
            
            // arg0 = string representing the service name
            String serviceName = getStringName(b, u, 0);
            if (serviceName == null)
                throw new RuntimeException("error: service name is null for '" + u + "'");
            System.out.println("add1 service name: " + serviceName + " " + u);
            
            // arg1 = IBinder referencing the service class
            Value v = ie.getArg(1);
            if (!(v instanceof Local)) {
                System.out
                .println("Warning: adding a service without a local reference! Skipping...");
                continue;
            }

            Local l = (Local) v;
            System.out.println("local referencing service: " + l);

            // find definition of local referencing service (go through
            // aliases)
            Unit curUnit = u;
            do {
                List<Unit> lu = localDefs.getDefsOfAt(l, curUnit);
                if (lu != null) {
                    Unit luUnit = lu.get(0);
                    if (luUnit instanceof JAssignStmt) {
                        JAssignStmt ass = (JAssignStmt) luUnit;
                        if (ass.getRightOp() instanceof Local) {
                            curUnit = luUnit;
                            l = (Local) ass.getRightOp();
                            continue;
                        }
                        defs.addAll(lu);
                        break;
                    } else if (luUnit instanceof IdentityStmt) {
                        JIdentityStmt istmt = (JIdentityStmt) luUnit;
                        // could be a method parameter or 'this'
                        defs.add(luUnit);
                        break;
                    } else {
                        throw new RuntimeException("[E] Not assignstmt nor identitystmt: "
                                + luUnit);
                    }
                } else {
                    throw new RuntimeException("[E] No def for local in stmt: " + s);
                }

            } while (true);

            SootClass concreteClass = null;
            //
            for (Unit d : defs) {
                System.out.println("  def: " + d);

                concreteClass = getConcreteClass(d, b, localDefs);
                if (concreteClass == null) {
                    System.out.println("warning: could not find name of class! " + d);
                    continue;
                }
                System.out.println("  concrete class: " + concreteClass);
                FastHierarchy fh = Scene.v().getOrMakeFastHierarchy();
                Set<SootClass> implementedInterafaces = Util.getAllInterafaces(concreteClass);
                concreteServicesToInterfaces.put(concreteClass, implementedInterafaces);
                serviceIdToServiceClass.put(serviceName, concreteClass);
                // if
                // (serverThread.getName().equals(sm.getDeclaringClass().getName()))
                // { // record
                // // services
                // // initialized
                // // in
                // // server
                // // thread
                // serverThreadServices.add(serviceName);
                // } else {
                // otherServices.add(serviceName);
                // }
                break;
            }
            
            Map m = new HashMap<String, SootClass>();
            m.put(serviceName, concreteClass);
            list.add(m);


        }

        return list;

    }

    /**
     * Get the String value for String parameter0 of invoke at Unit u.
     * 
     * @param b
     * @param u
     * @return
     */
    public static String getStringName(Body b, Unit u, int argNumber) {
        String serviceName = null;

        ExceptionalUnitGraph eug = new ExceptionalUnitGraph(b);
        final SmartLocalDefs localDefs = new SmartLocalDefs(eug, new SimpleLiveLocals(eug));

        System.out.println(" [-] retrieve service name in unit: " + u);
        Stmt s = (Stmt) u;
        InvokeExpr ie = s.getInvokeExpr();
        List<Unit> defs = new ArrayList<Unit>();
        Value v = ie.getArg(argNumber);
        if (v instanceof StringConstant) {
            serviceName = ((StringConstant) v).value;
            return serviceName;
        } else if (v instanceof ClassConstant) {
        	serviceName = ((ClassConstant) v).value;
        	return serviceName;
        } 
        if (!(v instanceof Local)) {
            System.out.println("Warning: getSystemService without a local reference ("
                    + v.getType() + ")! Skipping...");
            return serviceName;
        }
        Local l = (Local) v;
        Unit curUnit = u;
        do {
            List<Unit> lu = localDefs.getDefsOfAt(l, curUnit);
            if (lu != null) {
                if (lu.get(0) instanceof IdentityStmt)
                    return "unknown_from_identity_stmt";
                JAssignStmt ass = (JAssignStmt) lu.get(0);
                if (ass.getRightOp() instanceof Local) {
                    curUnit = lu.get(0);
                    l = (Local) ass.getRightOp();
                    continue;
                }
                defs.addAll(lu);
                break;
            }
        } while (true);
        for (Unit d : defs) {
            System.out.println("  def: " + d);

            if (!(d instanceof JAssignStmt)) { // should not happen
                System.out.println("W: not instance of AssignStmt! (" + d + ")");
                break;
            }

            JAssignStmt ass = (JAssignStmt) d;
            Value rop = ass.getRightOp();

            SootClass concreteService = null;
            if (rop instanceof StringConstant) {
                StringConstant str = (StringConstant) rop;
                serviceName = str.value;
                break;
            } else if (rop instanceof InvokeExpr) {
            	InvokeExpr rie = (InvokeExpr)rop;
            	// in the latest android "M" only com.android.ims.ImsManager has this method call
            	// and the method directly returns the string.
            	// if more have this, the implem. should be improved to handle more complicated situations.
            	ReturnStmt rets = (ReturnStmt) rie.getMethod().retrieveActiveBody().getUnits().getLast();
            	serviceName = ((StringConstant)rets.getOp()).value;
            	break;
            }
        }

        return serviceName;
    }

    static String REFLECTION_01 = "<java.lang.reflect.Constructor: java.lang.Object newInstance(java.lang.Object[])>";

    /**
     * Finds and return concrete Soot class for local at unit d.
     * 
     * @param d
     * @param b
     * @param localDefs
     * @return
     */
    private static SootClass getConcreteClass(Unit d, Body b, SmartLocalDefs localDefs) {
        Value rop = null;
        if (d instanceof JAssignStmt) {
            JAssignStmt ass = (JAssignStmt) d;
            rop = ass.getRightOp();
        } else if (d instanceof JIdentityStmt) {
            JIdentityStmt istmt = (JIdentityStmt) d;
            rop = istmt.getRightOp();
        } else {
            throw new RuntimeException("error: not assignment nor identity! " + d);
        }
        SootClass concreteService = null;
        if (rop instanceof JNewExpr) {
            JNewExpr newExpr = (JNewExpr) rop;
            concreteService = Scene.v().getSootClass(newExpr.getType().toString());
        } else if (rop instanceof InvokeExpr) {
            InvokeExpr iexpr = (InvokeExpr) rop;
            String type = iexpr.getType().toString();
            SootMethodRef smref = iexpr.getMethodRef();
            if (smref.getSignature().equals(REFLECTION_01)) {
                // TODO
                return null;
            }
            if (type.contains("android.os.IBinder")) { // must get type of
                                                       // 'this'
                System.out.println("signature: " + smref.getSignature());
                type = smref.getSignature().split(": ")[0].replaceAll("<", "");
            }
            concreteService = Scene.v().getSootClass(type);
        } else if (rop instanceof ThisRef) {
            ThisRef tr = (ThisRef) rop;
            concreteService = Scene.v().getSootClass(tr.getType().toString());
        } else if (rop instanceof FieldRef) {
            FieldRef fref = (FieldRef) rop;
            concreteService = Scene.v().getSootClass(fref.getType().toString());
        } else if (rop instanceof CastExpr) {
            CastExpr ce = (CastExpr) rop;
            // if local get def and find type of def
            if (ce.getOp() instanceof Local) {
                Local rl = (Local) ce.getOp();
                List<Unit> lu = localDefs.getDefsOfAt(rl, d);
                // should only have one def
                return getConcreteClass(lu.get(0), b, localDefs);
            } else {
                throw new RuntimeException("not local for cast expr right value!");
            }
        } else if (rop instanceof Local) {
            Local rl = (Local) rop;
            List<Unit> lu = localDefs.getDefsOfAt(rl, d);
            return getConcreteClass(lu.get(0), b, localDefs);
        } else if (rop instanceof ParameterRef) {
        	System.err.println("warning: parameters not handled yet: "+ rop);
        	return null;
        } else {
            throw new RuntimeException("right expression not instance of JNewExpr||InvokeExpr! "
                    + rop + " (" + rop.getClass() + ")" + "\n" + b);
        }
        return concreteService;
    }

    /**
     * Returns a list of Map from service names to concrete Soot classes.
     * 
     * @param sm
     * @return
     */
    public static Map<Unit, String> getGetUnitStringMap(SootMethod sm, String methodStringObjSig) {
        Map<Unit, String> map = new HashMap<Unit, String>();

        if (!sm.isConcrete()) {
            throw new RuntimeException("error: not concrete method " + sm);
        }

        Body b = null;
        try {
            b = sm.retrieveActiveBody();
        } catch (Exception e) {
            throw new RuntimeException("error: no active body for method " + sm);
        }

        ExceptionalUnitGraph eug = new ExceptionalUnitGraph(b);
        final SmartLocalDefs localDefs = new SmartLocalDefs(eug, new SimpleLiveLocals(eug));
        for (Unit u : b.getUnits()) {
            Stmt s = (Stmt) u;

            if (!s.containsInvokeExpr()) {
                continue;
            }
            InvokeExpr ie = s.getInvokeExpr();
            SootMethodRef smr = ie.getMethodRef();
            String sig = smr.getSignature();

            if (!sig.endsWith(methodStringObjSig)) {
                continue;
            }

            String name = getStringName(b, u, 0);
            map.put(u, name);

        }

        return map;
    }

    
    public static
    <T extends Comparable<? super T>> List<T> asSortedList(Collection<T> c) {
      List<T> list = new ArrayList<T>(c);
      java.util.Collections.sort(list);
      return list;
    }


    
}
