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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lu.uni.rrc.HandleIntents;
import lu.uni.rrc.Util;
import soot.Body;
import soot.FastHierarchy;
import soot.Local;
import soot.Scene;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.SootMethodRef;
import soot.Unit;
import soot.Value;
import soot.jimple.CastExpr;
import soot.jimple.FieldRef;
import soot.jimple.IdentityStmt;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.StringConstant;
import soot.jimple.ThisRef;
import soot.jimple.internal.JAssignStmt;
import soot.jimple.internal.JIdentityStmt;
import soot.jimple.internal.JNewExpr;
import soot.tagkit.StringConstantValueTag;
import soot.tagkit.Tag;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.scalar.SimpleLiveLocals;
import soot.toolkits.scalar.SmartLocalDefs;

public class ServiceMapper {
	
	private static final Logger logger = LoggerFactory.getLogger(ServiceMapper.class);

    private HashMap<SootClass, Set<SootClass>> concreteServicesToInterfaces = new HashMap<SootClass, Set<SootClass>>();
    private HashMap<String, SootClass> serviceIdToServiceClass = new HashMap<String, SootClass>();
    private HashMap<String, SootClass> serviceIdToManagerClass = new HashMap<String, SootClass>();
    private HashMap<String, List<String>> serviceIdCalledByMethods = new HashMap<String, List<String>>();
    private List<String> serverThreadServices = new ArrayList<String>();
    private List<String> otherServices = new ArrayList<String>();
    private Set<String> contextServiceId = new HashSet<String>();

    // services
    public static String addServiceSig = "<android.os.ServiceManager: void addService(java.lang.String,android.os.IBinder)>";
    public static String getServiceSig = "<android.os.ServiceManager: android.os.IBinder getService(java.lang.String)>";


    public SootClass serverThread = Scene.v().getSootClass("com.android.server.ServerThread");

    private static ServiceMapper singleton = null;

    public static ServiceMapper v() {
        if (singleton == null)
            singleton = new ServiceMapper();
        return singleton;
    }

    private ServiceMapper() {
        System.out.println(" [*] initialize service mapper");
        contextServiceId = computeContextServiceId();
    }

    private Set<String> computeContextServiceId() {

        Set<String> ids = new HashSet<String>();

        SootClass contextClass = Scene.v().getSootClass("android.content.Context"); // "android.content.Context");

        for (SootField sf : contextClass.getFields()) {
            if (!(sf.getName().endsWith("SERVICE"))) {
                continue;
            }
            for (Tag t : sf.getTags()) {
                if (t instanceof StringConstantValueTag) {
                    StringConstantValueTag stringval = (StringConstantValueTag) t;
                    String id = stringval.getStringValue();
                    System.out.println("add service id '" + id + "'");
                    ids.add(id);
                }
            }
        }
        return ids;
    }

    /**
     * 
     * @param serverThread
     */
    public void getServerThreadServiceMapping(SootClass serverThread) {
        System.out.println("processing server thread class: " + serverThread);
        SootMethod runM = serverThread.getMethodByName("run");
        getServiceMappingForMethod(runM);
    }

    // TODO: We get concrete class for each service.
    // TODO: For spark we need to have the initialization process of those class
    // as well.
    // public void getServiceMapping(SootClass sc) {
    // System.out.println("processing " + sc);
    // for (SootMethod sm : sc.getMethods()) {
    // if (sc.isInterface())
    // continue;
    // if (sm.isConcrete()) {
    // getServiceMappingForMethod(sm);
    // }
    // }
    // }

    /**
     * Get registered account managers
     * 
     * @param sm
     */
    public void getRegisterServiceMapping(SootMethod sm) {
        if (!sm.isConcrete())
            return;

        Body b = null;
        try {
            b = sm.retrieveActiveBody();
        } catch (Exception e) {
            throw new RuntimeException("[E] when retrieving body for method " + sm);
        }
        ExceptionalUnitGraph eug = new ExceptionalUnitGraph(b);
        final SmartLocalDefs localDefs = new SmartLocalDefs(eug, new SimpleLiveLocals(eug));
        for (Unit u : b.getUnits()) {
            Stmt s = (Stmt) u;
            if (s.containsInvokeExpr()) {
                InvokeExpr ie = s.getInvokeExpr();
                SootMethodRef smr = ie.getMethodRef();
                String sig = smr.getSignature();
                if (sig.equals(addServiceSig)) {
                    System.out.println("statement: " + s);
                    List<Unit> defs = new ArrayList<Unit>();
                    // arg0 = string representing the service name
                    String serviceName = getServiceName(b, u);
                    if (serviceName == null)
                        throw new RuntimeException("error: service name is null for '" + u + "'");
                    System.out.println("add register manager name: " + serviceName + " " + u);
                    // arg1 = IBinder referencing the service class
                    Value v = ie.getArg(1);
                    if (!(v instanceof Local)) {
                        System.out
                                .println("Warning: adding a register manager without a local reference! Skipping...");
                        continue;
                    }
                }
            }
        }
    }

    public void getServiceMappingForMethod(SootMethod sm) {

        if (!sm.isConcrete())
            return;

        Body b = null;
        try {
            b = sm.retrieveActiveBody();
        } catch (Exception e) {
            System.out.println("[E] when retrieving body for method " + sm);
            return;
        }
        ExceptionalUnitGraph eug = new ExceptionalUnitGraph(b);
        final SmartLocalDefs localDefs = new SmartLocalDefs(eug, new SimpleLiveLocals(eug));
        for (Unit u : b.getUnits()) {
            Stmt s = (Stmt) u;
            if (s.containsInvokeExpr()) {
                InvokeExpr ie = s.getInvokeExpr();
                SootMethodRef smr = ie.getMethodRef();
                String sig = smr.getSignature();
                if (sig.equals(addServiceSig)) {
                    System.out.println("statement: " + s);
                    List<Unit> defs = new ArrayList<Unit>();
                    // arg0 = string representing the service name
                    String serviceName = getServiceName(b, u);
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

                    //
                    for (Unit d : defs) {
                        System.out.println("  def: " + d);

                        SootClass concreteService = getConcreteService(d, b, localDefs);
                        if (concreteService == null) {
                            System.out.println("warning: could not find name of service! " + d);
                            continue;
                        }
                        System.out.println("  concrete service: " + concreteService);
                        FastHierarchy fh = Scene.v().getOrMakeFastHierarchy();
                        Set<SootClass> implementedInterafaces = Util
                                .getAllInterafaces(concreteService);
                        concreteServicesToInterfaces.put(concreteService, implementedInterafaces);
                        serviceIdToServiceClass.put(serviceName, concreteService);
                        if (serverThread.getName().equals(sm.getDeclaringClass().getName())) { // record
                                                                                               // services
                                                                                               // initialized
                                                                                               // in
                                                                                               // server
                                                                                               // thread
                            serverThreadServices.add(serviceName);
                        } else {
                            otherServices.add(serviceName);
                        }
                    }
                }
            }
        }

    }

    String REFLECTION_01 = "<java.lang.reflect.Constructor: java.lang.Object newInstance(java.lang.Object[])>";

    private SootClass getConcreteService(Unit d, Body b, SmartLocalDefs localDefs) {
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
                return getConcreteService(lu.get(0), b, localDefs);
            } else {
                throw new RuntimeException("not local for cast expr right value!");
            }
        } else if (rop instanceof Local) {
            Local rl = (Local) rop;
            List<Unit> lu = localDefs.getDefsOfAt(rl, d);
            return getConcreteService(lu.get(0), b, localDefs);
        } else {
            throw new RuntimeException("right expression not instance of JNewExpr||InvokeExpr! "
                    + rop + " (" + rop.getClass() + ")" + "\n" + b);
        }
        return concreteService;
    }

    public HashMap<SootClass, SootClass> checkServicesGeneratedFromAidl() {

        System.out.println("see if service generated by .aidl...");

        List<String> sFromAidlList = new ArrayList<String>();
        List<String> sNOTFromAidlList = new ArrayList<String>();
        HashMap<SootClass, SootClass> concreteServiceToAidlInterface = new HashMap<SootClass, SootClass>();

        System.out.println();
        // for all concrete services check if generated from .aidl
        for (SootClass k : concreteServicesToInterfaces.keySet()) {
            System.out.println("aidl check for " + k);
            SootClass isFromAidl = null;
            Set<SootClass> impIntfs = concreteServicesToInterfaces.get(k);
            if (!(impIntfs.size() <= 1)) { // should at least contain IBinder
                                           // and the service Interface
                for (SootClass i : impIntfs) {
                    String n = i.getName();
                    if (n.equals("android.os.IBinder")) {
                        continue;
                    }
                    if (Scene.v().containsClass(n + "$Stub")
                            && Scene.v().containsClass(n + "$Stub$Proxy")) {
                        isFromAidl = i;
                        break;
                    }
                }
            }

            if (isFromAidl != null) {
                System.out.println("!class " + k + " generated from AIDL " + isFromAidl);
                System.out.println(k);
                sFromAidlList.add(k.toString());
                concreteServiceToAidlInterface.put(k, isFromAidl);
            } else {
                System.out.println(" uclass " + k + " NOT generated from AIDL " + isFromAidl);
                sNOTFromAidlList.add(k.toString());
            }
            isFromAidl = null;
        }

        return concreteServiceToAidlInterface;

    }

    /**
     * 
     * @param b
     * @param getSystemService
     * @return
     */
    public List<String> getServiceNames(Body b, List<Unit> getSystemService) {
        List<String> r = new ArrayList<String>();
        for (Unit u : getSystemService) {
            // System.out.println("u: "+ u);
            String serviceName = getServiceName(b, u);
            r.add(serviceName);
            System.out.println("add2 service name: " + serviceName + " " + u);
        }
        return r;
    }



    /**
     * 
     * @param b
     * @param u
     * @return
     */
    String getServiceName(Body b, Unit u) {
        String serviceName = null;

        ExceptionalUnitGraph eug = new ExceptionalUnitGraph(b);
        final SmartLocalDefs localDefs = new SmartLocalDefs(eug, new SimpleLiveLocals(eug));

        System.out.println(" [-] retrieve service name in unit: " + u);
        Stmt s = (Stmt) u;
        if (!s.containsInvokeExpr()) {
            throw new RuntimeException("error: statement does not contain invoke expr: " + s);
        }
        InvokeExpr ie = s.getInvokeExpr();

        List<Unit> defs = new ArrayList<Unit>();

        if (ie.getArgs().size() == 0) {
            throw new RuntimeException(
                    "error: no Argument for get service method (should at least have a String representing the name of the service). "
                            + s);
        }
        Value v = ie.getArg(0);
        if (v instanceof StringConstant) {
            serviceName = ((StringConstant) v).value;
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
            logger.debug("  def: " + d);

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
            }
        }

        return serviceName;
    }

    public HashMap<SootClass, Set<SootClass>> getConcreteServicesToInterfaces() {
        return concreteServicesToInterfaces;
    }

    public void setConcreteServicesToInterfaces(
            HashMap<SootClass, Set<SootClass>> concreteServicesToInterfaces) {
        this.concreteServicesToInterfaces = concreteServicesToInterfaces;
    }

    public HashMap<String, SootClass> getServiceIdToSootClass() {
        return serviceIdToServiceClass;
    }

    public void setServiceIdToSootClass(
HashMap<String, SootClass> serviceIdToSootClass) {
        this.serviceIdToServiceClass = serviceIdToSootClass;
    }

    public List<String> getServerThreadServices() {
        return serverThreadServices;
    }

    public void setServerThreadServices(List<String> serverThreadServices) {
        this.serverThreadServices = serverThreadServices;
    }

    public HashMap<String, List<String>> getServiceIdCalledByMethods() {
        return serviceIdCalledByMethods;
    }

    public void setServiceIdCalledByMethods(
HashMap<String, List<String>> serviceIdCalledByMethods) {
        this.serviceIdCalledByMethods = serviceIdCalledByMethods;
    }

    public Set<String> getContextServiceId() {
        return contextServiceId;
    }

    public void setContextServiceId(Set<String> contextServiceId) {
        this.contextServiceId = contextServiceId;
    }

    public List<String> getOtherServices() {
        return otherServices;
    }

    public void setOtherServices(List<String> otherServices) {
        this.otherServices = otherServices;
    }
}
