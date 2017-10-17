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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import soot.Body;
import soot.Local;
import soot.Modifier;
import soot.NullType;
import soot.PrimType;
import soot.RefType;
import soot.Scene;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Type;
import soot.Unit;
import soot.UnitBox;
import soot.Value;
import soot.ValueBox;
import soot.VoidType;
import soot.jimple.AssignStmt;
import soot.jimple.FieldRef;
import soot.jimple.IdentityStmt;
import soot.jimple.IntConstant;
import soot.jimple.InvokeExpr;
import soot.jimple.Jimple;
import soot.jimple.NullConstant;
import soot.jimple.ParameterRef;
import soot.jimple.StaticFieldRef;
import soot.jimple.Stmt;
import soot.jimple.StringConstant;
import soot.jimple.internal.JReturnStmt;
import soot.jimple.toolkits.typing.TypeAssigner;
import soot.jimple.toolkits.typing.fast.BottomType;
import soot.options.Options;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.scalar.SimpleLiveLocals;
import soot.toolkits.scalar.SimpleLocalUses;
import soot.toolkits.scalar.SmartLocalDefs;
import soot.toolkits.scalar.UnusedLocalEliminator;

public class GenerateServiceInit {

    public static String servicesInitClassName = "ServicesInit";
    SootClass contextClass = Scene.v().getSootClass("android.content.Context");
    SootClass contextImplClass = Scene.v().getSootClass(
            "android.app.ContextImpl");
    SootClass serviceFetcherClass = Scene.v().getSootClass(
            "android.app.ContextImpl$ServiceFetcher");
    SootClass staticServiceFetcherClass = Scene.v().getSootClass(
            "android.app.ContextImpl$StaticServiceFetcher");

    Map<String, SootClass> idToClass = null;

    /**
     * Create a SootClass in which all Android System services are initialized.
     * Initializing those services is a must when using Spark. Otherwise, Spark
     * would suppose services are not initialized and would not process the call
     * graph further. This is not necessary for CHA though.
     * 
     * @param serviceIdToSootClass
     * @param serviceToInterfaces
     * @return
     */
    public SootClass createServicesInitClass(Map<String, SootClass> idToClass) {
        this.idToClass = idToClass;
        SootClass initClass = new SootClass(servicesInitClassName,
                Modifier.PUBLIC);
        initClass.setSuperclass(Scene.v().getSootClass("java.lang.Object"));
        Scene.v().addClass(initClass);
        initClass.setApplicationClass();

        List<SootClass> serviceClasses = new ArrayList<SootClass>();

        // add static fields for services
        for (String k : idToClass.keySet()) {
            String serviceName = k;
            SootClass serviceClass = idToClass.get(k);
            if (serviceClass == null) {
            	System.err.println("warning: null service class for service '"+ k +"'!");
            	continue;
            }
            serviceClasses.add(serviceClass);
            SootField sf = new SootField(k, serviceClass.getType(),
                    Modifier.STATIC);
            initClass.addField(sf);
            System.out.println("add field '" + sf + "' to class " + initClass);
        }
        // add static field for context
        SootField sf = new SootField(makeFieldName(contextClass.getType()),
                contextClass.getType(), Modifier.STATIC);
        initClass.addField(sf);

        // // type to constructs
        // Set<SootClass> types = new HashSet<SootClass>();
        // for (String k: serviceIdToSootClass.keySet()) {
        // String serviceName = k;
        // SootClass serviceClass = serviceIdToSootClass.get(k);
        // }
        //
        // // add constructors
        // HashMap<String, List<SootMethod>> serviceIDtoConstructor = new
        // HashMap<String, List<SootMethod>>();
        // List<SootMethod> constructorsOnly = new ArrayList<SootMethod>();
        // for (String k: serviceIdToSootClass.keySet()) {
        // SootClass service = serviceIdToSootClass.get(k);
        // for (SootMethod sm: service.getMethods()) {
        // if (sm.isConstructor()) {
        // // must be public
        // if (!sm.isPublic()) {
        // sm.setModifiers(Modifier.PUBLIC);
        // }
        // if (serviceIDtoConstructor.containsKey(k)) {
        // serviceIDtoConstructor.get(k).add(sm);
        // } else {
        // List<SootMethod> constructors = new ArrayList<SootMethod>();
        // constructors.add(sm);
        // }
        // // for test: add constructor's parameters
        // addTypes (types, sm);
        // constructorsOnly.add(sm);
        // break; // keep only first constructor
        // }
        // }
        // }


        // create new method
        List<Type> mtypes = new ArrayList<Type>();
        Type returnType = VoidType.v();
        SootMethod initMethod = new SootMethod("initServices", mtypes,
                returnType);
        Body b = Jimple.v().newBody();
        // generate local for context
        {
            Local l = Jimple.v().newLocal(
                    makeLocalName(contextClass.getType()),
                    contextClass.getType());
            b.getLocals().add(l);
        }
        // generate a local for every service
        for (SootClass sc : serviceClasses) {
            Local l = Jimple.v().newLocal(makeLocalName(sc.getType()),
                    sc.getType());
            b.getLocals().add(l);
        }

        // initialize every local to null
        for (Local l : b.getLocals()) {
            Value lvalue = l;
            Value rvalue = NullConstant.v();
            Unit u = Jimple.v().newAssignStmt(lvalue, rvalue);
            b.getUnits().add(u);
        }
        // construct context
        {
            Type localType = Scene.v().getSootClass("android.content.Context")
                    .getType();
            String c1 = "<android.app.ContextImpl: void <init>()>";
            String c2 = "<android.app.ContextImpl: android.app.ContextImpl getImpl(android.content.Context)>";
            String c3 = "<android.app.ApplicationContext: void <init>()>";
            SootMethod constructor = null;
            if (Scene.v().containsMethod(c1)) {
	            // android 2.3 ...
	            constructor = Scene.v().getMethod(c1);
            } else if (Scene.v().containsMethod(c2)) {
            	// android 5 ...
	            constructor = Scene.v().getMethod(c2);
            } else if (Scene.v().containsMethod(c3)) {
	           // android api3 ...
		       constructor = Scene.v().getMethod(c3);
            } else {
            	throw new RuntimeException("error: no constructor found for ContextImpl");
            }
            constructor.setModifiers(constructor.getModifiers() | Modifier.PUBLIC);
            constructNewObject(b, localType, constructor);
        }

        // // construct non-serverThread services
        // for (SootClass sc : otherServiceClasses) {
        // if (!sc.isConcrete()) {
        // System.out.println("Warning: not concrete class: " + sc);
        // }
        // Type localType = sc.getType();
        // SootMethod constructor = null;
        // for (SootMethod sm : sc.getMethods()) {
        // if (sm.isConstructor()) {
        // constructor = sm;
        // sm.setModifiers(Modifier.PUBLIC);
        // System.out.println("generating for method " + sm);
        // break;
        // }
        // }
        // constructNewObject(b, localType, constructor);
        // // assign service to static field
        // assignServiceToField(b, initClass, sc);
        // }
        b.getUnits().add(Jimple.v().newNopStmt());
        b.getUnits().add(Jimple.v().newNopStmt());
        b.getUnits().add(Jimple.v().newNopStmt());

        // construct serverThread services
        for (SootClass sc : serviceClasses) {
            if (!sc.isConcrete()) {
                System.out.println("Warning: not concrete class: " + sc);
            }
            Type localType = sc.getType();
            SootMethod constructor = null;
            for (SootMethod sm : sc.getMethods()) {
                if (sm.isConstructor()) {
                    constructor = sm;
                    sm.setModifiers(Modifier.PUBLIC);
                    System.out.println("generating for method " + sm);
                    break;
                }
            }
            constructNewObject(b, localType, constructor);
            // assign service to static field
            assignServiceToField(b, initClass, sc);
            // add nop
            b.getUnits().add(Jimple.v().newNopStmt());
        }

        // add return statement
        b.getUnits().add(Jimple.v().newReturnVoidStmt());

        b.setMethod(initMethod);
        initMethod.setActiveBody(b);
        initMethod.setModifiers(Modifier.PUBLIC | Modifier.STATIC);
        initClass.addMethod(initMethod);

        System.out.println("init class:");
        System.out.println(initClass);
        System.out.println("initMethod:");
        System.out.println(initMethod);
        System.out.println(b);
        System.out.println();
        return initClass;
    }

    private void assignServiceToField(Body b, SootClass initClass, SootClass sc) {
        Value l = getLocalWithName(b, makeLocalName(sc.getType()), sc.getType());
        SootField f = initClass.getFieldByName(makeFieldName(sc.getType()));
        StaticFieldRef sfr = Jimple.v().newStaticFieldRef(f.makeRef());
        Unit u = Jimple.v().newAssignStmt(sfr, l);
        b.getUnits().add(u);
    }

    private String makeFieldName(RefType type) {
        if (type.toString().endsWith("content.Context"))
            return type.toString().replaceAll("\\.", "");

        for (String k : idToClass.keySet()) {
        	if (idToClass.get(k) == null)
        		continue;
            if (idToClass.get(k).getName().equals(type.toString()))
                return k;
        }
        throw new RuntimeException("error: no service name found for service '"
                + type + "'");
    }

    private void constructNewObject(Body b, Type localType, SootMethod constructor) {
        // "new" statement
        Value lvalue = getLocalWithName(b, makeLocalName(localType), localType);
        Value rvalue = Jimple.v().newNewExpr(
                constructor.getDeclaringClass().getType());
        AssignStmt ass = Jimple.v().newAssignStmt(lvalue, rvalue);
        b.getUnits().add(ass);
        // <init> invocation statement
        List<Value> args = new ArrayList<Value>();
        for (Type argt : (List<Type>) constructor.getParameterTypes()) {
            if (argt instanceof PrimType) {
                args.add(IntConstant.v(0));
            } else {
                args.add(getLocalWithName(b, makeLocalName(argt), argt));
            }
        }
        Value invokeExpr = null;
        Unit u = null;
        if (constructor.isStatic()) {
        	invokeExpr = Jimple.v().newStaticInvokeExpr(constructor.makeRef(), args);
        	u = Jimple.v().newAssignStmt(lvalue, invokeExpr);
        } else {
        	invokeExpr = Jimple.v().newSpecialInvokeExpr((Local) lvalue,
                constructor.makeRef(), args);
        	u = Jimple.v().newInvokeStmt(invokeExpr);
        }
         
        b.getUnits().add(u);
    }

    private String makeLocalName(Type localType) {
        return localType.toString().replaceAll("\\.", "")
                .replaceAll("\\$", "__");
    }

    private void addTypes(Set<SootClass> types, SootMethod sm) {
        types.add(sm.getDeclaringClass());
        System.out.println("adding types for method " + sm);
        for (Type t : (List<Type>) sm.getParameterTypes()) {
            SootClass psc = Scene.v().getSootClass(t.toString());
            if (!types.contains(psc)) {
                types.add(psc);
                for (SootMethod smc : psc.getMethods()) {
                    if (smc.isConstructor()) {
                        addTypes(types, smc);
                        break;
                    }
                }
            }
        }
    }

    private Value getLocalWithName(Body b, String name, Type t) {
        for (Local l : b.getLocals()) {
            if (l.getName().equals(name))
                return l;
        }

        // local not found, create one
        Local l = Jimple.v().newLocal(name, t);
        b.getLocals().add(l);
        SootClass sc = Scene.v().getSootClass(t.toString());
        SootMethod constructor = null;
        for (SootMethod sm : sc.getMethods()) {
            if (sm.isConstructor()) {
                constructor = sm;
                break;
            }
        }

        if (constructor != null) {
            System.out.println(" [-] constructing new local with constructor '" + constructor);
            constructNewObject(b, t, constructor);
            return l;
        } else {

            String localNames = "\n";
            for (Local lo : b.getLocals()) {
                localNames += lo.getName() + "  type: " + lo.getType() + " \n";
            }
            System.out.println("[Warning!] no local found with name '" + name + "'" + localNames
                    + " No constructor found for type '" + t + "' returning NullConstant!");
            return NullConstant.v();
        }
    }

    // // to sort constructors
    // Comparator<SootMethod> comparator = new Comparator<SootMethod>() {
    //
    // @Override
    // public int compare(SootMethod arg0, SootMethod arg1) {
    //
    // // arg0 config
    // List<Type> arg0ParamsTypes = arg0.getParameterTypes();
    // Type arg0ReturnType = arg0.getReturnType();
    // SootClass arg0ReturnClass =
    // Scene.v().getSootClass(arg0ReturnType.toString());
    // Set<SootClass> arg0Interfaces = new HashSet<SootClass>();
    // if (serviceToInterfaces.containsKey(arg0ReturnClass)) {
    // arg0Interfaces = serviceToInterfaces.get(arg0ReturnClass);
    // }
    //
    // // arg1 config
    // List<Type> arg1ParamsTypes = arg1.getParameterTypes();
    // Type arg1ReturnType = arg1.getReturnType();
    // SootClass arg1ReturnClass =
    // Scene.v().getSootClass(arg1ReturnType.toString());
    // Set<SootClass> arg1Interfaces = new HashSet<SootClass>();
    // if (serviceToInterfaces.containsKey(arg1ReturnClass)) {
    // arg1Interfaces = serviceToInterfaces.get(arg1ReturnClass);
    // }
    //
    // // return -1 if arg0 depends on arg1
    // for (Type t: arg0ParamsTypes) {
    // SootClass p = Scene.v().getSootClass(t.toString());
    // if (p.toString().equals(arg1ReturnClass.toString())) {
    // return -1;
    // }
    // if (arg1Interfaces.contains(p)) {
    // return -1;
    // }
    // }
    //
    // // return 1 if arg1 depends on arg0
    // for (Type t: arg1ParamsTypes) {
    // SootClass p = Scene.v().getSootClass(t.toString());
    // if (p.toString().equals(arg0ReturnClass.toString())) {
    // return 1;
    // }
    // if (arg0Interfaces.contains(p)) {
    // return 1;
    // }
    // }
    //
    // // no relation between arg1 and arg2
    // return 0;
    // }
    //
    // };

    // System.out.println("before sort:");
    // for (SootMethod sm: constructorsOnly) {
    // System.out.println(" sm: "+ sm);
    // }
    // Collections.sort(constructorsOnly, comparator);
    // System.out.println();
    // System.out.println("after sort:");
    // for (SootMethod sm: constructorsOnly) {
    // System.out.println(" sm: "+ sm);
    // }

    //
    // Create Managers
    //

    public SootClass createManagers(SootClass sc, Map<String, SootClass> managers) {
        System.out.println("Service Fetcher class: " + serviceFetcherClass);
        System.out.println("methods:");
        for (SootMethod sm : serviceFetcherClass.getMethods()) {
            System.out.println("m: " + sm);
        }
        boolean hasServiceFetcher = Scene.v().containsClass(
                serviceFetcherClass.getName());
        Map<String, String> managerType2getMethod = null;
        if (hasServiceFetcher && serviceFetcherClass.isApplicationClass()
                && !serviceFetcherClass.isPhantom()) {
            System.out.println(" [*] creating managers from system fetchers...");
            managerType2getMethod = createManagersFromSystemFetchers(sc, managers);
        } else {
            System.out.println(" [*] creating managers from getSystemService()...");
            CreateManagers cm = new CreateManagers();
            managerType2getMethod = cm.createManagersFromGetSystemService(sc);
        }
        writeManagerConfig(managerType2getMethod, Options.v().output_dir()
                + "/managers.txt");
        for (SootMethod sm : sc.getMethods()) {
            System.out.println("body for method " + sm);
            System.out.println(sm.retrieveActiveBody());
        }
        return sc;
    }


    private Map<String, String> createManagersFromSystemFetchers(SootClass initSC,
            Map<String, SootClass> managers) {
        Map<String, String> managerType2getMethod = new HashMap<String, String>();
        // FastHierarchy fh = Scene.v().getOrMakeFastHierarchy();
        for (String k : managers.keySet()) {

            SootClass c = managers.get(k);

            System.out.println("[manager] handling " + k + " " + c);

            boolean hasGetInstanceMethod = false;
            SootMethod getI = hasGetInstanceMethod(c);
            if (getI != null)
                hasGetInstanceMethod = true;

            // if (!(c.getName().startsWith("android.app.")))
            // continue;
            // if (!(c.getName().matches(".*[0-9]$")))
            // continue;
            // if (c.getName().startsWith("java.lang")) {
            // continue;
            // }

            // special case: wallpaper
            if (k.equals("wallpaper")) {
                c = Scene.v().getSootClass("android.app.ContextImpl$1");
            }

            if (c.isConcrete()
                    && c.getSuperclass() != null
                    && c.getSuperclass().getName()
                            .equals(serviceFetcherClass.getName())
                    || c.isConcrete()
                    && c.getSuperclass() != null
                    && c.getSuperclass().getName()
                            .equals(staticServiceFetcherClass.getName())
                    || hasGetInstanceMethod) {
                System.out.println("[manager] class '" + c + "' extends "
                        + serviceFetcherClass);
                String managerName = k;// getManagerNameFromSystemFetcher(c,
                                       // getI);
                String name = "getManager_" + managerName;
                System.out.println("manager name '" + name + "'");
                List<Type> args = new ArrayList<Type>();
                Type returnType = VoidType.v(); // temporarily
                SootMethod sm = new SootMethod(name, args, returnType);
                Body b = getBodyFromSystemFetcher(c, getI);
                returnType = getSystemFetcherReturnType(b);
                sm.setReturnType(returnType);
                b.setMethod(sm);
                sm.setActiveBody(b);
                sm.setModifiers(Modifier.STATIC | Modifier.PUBLIC);
                sm.setDeclaringClass(initSC);

                System.out.println("[from system fetcher] add manager \n" + b);

                boolean cont = false;
                for (SootMethod sm2 : initSC.getMethods()) {
                    if (sm.getName().equals(sm2.getName())) {
                        System.out
                                .println("warning: method already exists in ServiceInit class!!!");
                        cont = true;
                        break;
                    }
                }
                if (cont)
                    continue;
                initSC.addMethod(sm);
                managerType2getMethod.put(returnType.toString(), name);
                
                // check types
            	System.out.println("type assigner for body \n"+ b);
            	TypeAssigner.v().transform(b);
            	UnusedLocalEliminator.v().transform(b);
            	for (Local l: b.getLocals()) {
            		if (l.getType() instanceof BottomType) {
            			throw new RuntimeException("bottom_type in body! \n"+b);
            		} else if (l.getType() instanceof NullType) {
            			l.setType(RefType.v("java.lang.Object"));
            		}
            	}
                
            } else {
                System.out
                        .println("[manager] not candidate: " + c + " super: "
                        + c.getSuperclass());
            }
        }
        return managerType2getMethod;
    }

    private SootMethod hasGetInstanceMethod(SootClass sc) {
        String cname = sc.getName();
        if (!(cname.startsWith("android") || cname.startsWith("com.android")))
            return null;
        for (SootMethod sm: sc.getMethods()) {
            if (sm.isConcrete() && sm.getName().equals(("getInstance"))) {
                Body b = sm.retrieveActiveBody();
                for (Unit u: b.getUnits()){
                    Stmt s = (Stmt)u;
                    if (s.containsInvokeExpr()) {
                        InvokeExpr ie = (InvokeExpr)s.getInvokeExpr();
                        String name = ie.getMethodRef().name();
                        if (name.equals("getService"))
                            return sm;
                        if (name.equals("getSystemService"))
                            return sm;
                    }
                }
            }
        }
        return null;
    }

    private Type getSystemFetcherReturnType(Body b) {
        ExceptionalUnitGraph g = new ExceptionalUnitGraph(b);
        SmartLocalDefs localDefs = new SmartLocalDefs(g,
                new SimpleLiveLocals(g));
        SimpleLocalUses localUses = new SimpleLocalUses(g, localDefs);

        for (Unit u : b.getUnits()) {
            if (u instanceof JReturnStmt) {
                JReturnStmt ret = (JReturnStmt) u;
                Local l = (Local) ret.getOp();
                List<Unit> defs = localDefs.getDefsOfAt(l, u);
                for (Unit d : defs) {
                    if (d instanceof AssignStmt) {
                        AssignStmt ass = (AssignStmt) d;
                        Type t = ass.getRightOp().getType();
                        if (t instanceof NullType) {
                            System.out.println("warning: null type! " + ass);
                        } else {
                            System.out.println("ok: found return type: " + t);
                            return t;
                        }
                    }
                }
            }
        }
        System.out.println("warning: no return type found returning void!");
        return VoidType.v();
    }

    private Body getBodyFromSystemFetcher(SootClass sc, SootMethod getI) {
        SootMethod sm = null;
        if (getI != null) {
            sm = getI;
        } else {
            if (sc.declaresMethodByName("createService")) {
                sm = sc.getMethodByName("createService");
            } else if (sc.declaresMethodByName("createStaticService")) {
                sm = sc.getMethodByName("createStaticService");
            } else if (sc.declaresMethodByName("getService")) {
                sm = sc.getMethodByName("getService");
            } else {
                throw new RuntimeException(
                        "error: not method 'createService' or 'createStaticService' or 'getService' in class "
                                + sc);
            }
        }

        Body b = sm.retrieveActiveBody();
        Body newBody = Jimple.v().newBody();
        newBody.importBodyContentsFrom(b);
        newBody.setMethod(sm); // this is temporary otherwise an exception will
                               // be raised during the unit graph construction
        updateNewBody(newBody);

        return newBody;
    }

    private void updateNewBody(Body b) {

        SootClass servicesInitClass = Scene.v().getSootClass(
                servicesInitClassName);

        // since method is static, just remove first statement if necessary
        // // change reference to @this type
        // Unit first = b.getUnits().getFirst();
        // if (!(first instanceof IdentityStmt))
        // throw new
        // RuntimeException("error: first statement not instance of IdentityStmt: "+
        // first);
        // IdentityStmt idstmt = (IdentityStmt)first;
        // Local left = (Local) idstmt.getLeftOp();
        // Value right = idstmt.getRightOp();
        // RefType t = servicesInitClass.getType();
        // ThisRef newThisRef = Jimple.v().newThisRef(t);
        // left.setType(t);
        // Unit newIdentityStmt = Jimple.v().newIdentityStmt(left, newThisRef);
        // b.getUnits().swapWith(first, newIdentityStmt);
        b.getUnits().removeFirst();
        //

        // if context parameter is present redirect it
        Unit second = b.getUnits().getFirst(); // .getSuccOf(newIdentityStmt);
        if (second instanceof IdentityStmt) {
            IdentityStmt pStmt = (IdentityStmt) second;
            Local l = (Local) pStmt.getLeftOp();
            Value r = pStmt.getRightOp();
            SootField sf = servicesInitClass
                    .getFieldByName("androidcontentContext");
            Value newR = Jimple.v().newStaticFieldRef(sf.makeRef());
            AssignStmt newStmt = Jimple.v().newAssignStmt(l, newR);
            b.getUnits().swapWith(pStmt, newStmt);
        }

        // redirect getService methods
        RedirectService rs = new RedirectService();
        List<Unit> getSystemService = rs.hasCallToGetSystem(b);
        if (getSystemService.size() != 0) {
            System.out.println("redirecting getService for body");
            List<String> servicesCalled = ServiceMapper.v().getServiceNames(b,
                    getSystemService);
            rs.redirectGetSystemServiceCalls(b, getSystemService,
                    servicesCalled, servicesInitClass);
        }

        redirectContextFieldAssignments(b);
        
        checkUsageofR0(b);

    }
    
    public static void checkUsageofR0(Body b) {
    	boolean initR0 = false;
    	Local r0Local = null;
    	Set<Unit> useR0List = new HashSet<Unit>();
    	for (Unit u: b.getUnits()) {
	    	for (ValueBox vb: u.getUseBoxes()) {
	    		if (vb.getValue() instanceof Local) {
	    			Local l = (Local)vb.getValue();
	    			if (l.getName().equals("r0")) {
	    				r0Local = l;
	    				System.out.println("use of r0: "+ u);
	    				useR0List.add(u);
	    				initR0 = true;
	    			}
	    		}
	    	}
    	}

    	for (Unit u: useR0List) {
    		if (u instanceof AssignStmt) {
//    			boolean toNOP = false;
    			boolean swap = false;
    			AssignStmt ass = (AssignStmt)u;
    			for (ValueBox vb: ass.getLeftOp().getUseBoxes()) {
    				if (vb.getValue() == r0Local) {
						b.getUnits().swapWith(u, Jimple.v().newNopStmt());
						swap = true;
						break;
    				}
    			}
    			if (swap)
    				continue;
    			
    			ass.setRightOp(Jimple.v().newNewExpr(RefType.v("java.lang.Object")));

				//break;
//    			if (toNOP) {
//    				b.getUnits().swapWith(u, Jimple.v().newNopStmt());
//    			} else {
//    				ass.setRightOp(NullConstant.v());
//    			}

    		} else {
    			throw new RuntimeException("not assign stmt: "+ u);
    		}
    	}
//    	if (initR0) {
//        	Unit afterThisU = null;
//    		for (Unit u: b.getUnits()) {
//    			afterThisU = u;
//    			if (!(u instanceof IdentityStmt)) {
//    				break;
//    			}
//    		}
//    		Unit toInsert = Jimple.v().newAssignStmt(r0Local, NullConstant.v());
//    		Unit point = afterThisU;
//    		b.getUnits().insertAfter(toInsert, point);
//    		System.out.println("adding initialization of r0: "+ toInsert);
//    	}
    	
    	
    }

    public static void redirectContextFieldAssignments(Body b) {
        System.out.println(" [-] redirect context field assigmnents");

        List<Unit> mustChange = new ArrayList<Unit>();

        for (Unit u : b.getUnits()) {
            if (u instanceof AssignStmt) {
                System.out.println(" [-] assignment: " + u);
                AssignStmt ass = (AssignStmt) u;
                Value lop = ass.getLeftOp();
                Value rop = ass.getRightOp();
                if (lop instanceof FieldRef && rop instanceof Local) {
                    FieldRef fr = (FieldRef) lop;
                    String frTypeStr = fr.getFieldRef().type().toString();
                    String frClass = fr.getFieldRef().declaringClass()
                            .getName();

                    String returnTypeStr = b.getMethod().getReturnType()
                            .toString();
                    System.out.println(" [-] " + frTypeStr + " " + frClass
                            + " " + returnTypeStr);
                    if (frTypeStr.equals(returnTypeStr)) {
                        mustChange.add(u);
                    }
                }
            }
        }
        for (Unit u : mustChange) {
            AssignStmt ass = (AssignStmt) u;
            Unit newU = Jimple.v().newReturnStmt(ass.getRightOp());
            System.out.println(" [-] replacing " + u + " by " + newU);
            b.getUnits().swapWith(u, newU);
        }

    }

    static int unknowni = 0;
    private String getManagerNameFromSystemFetcher(SootClass sc, SootMethod getI) {
        SootMethod sm = null;

        System.out.println("try to find manager in class " + sc + " using method " + getI);

        if (getI != null) {
            sm = getI;
            return getNameFromGetInstance(sm);
        } else {

            if (sc.declaresMethodByName("createService")) {
                sm = sc.getMethodByName("createService");
            } else if (sc.declaresMethodByName("createStaticService")) {
                sm = sc.getMethodByName("createStaticService");
            } else if (sc.declaresMethodByName("getService")) {
                sm = sc.getMethodByName("getService");
            } else {
                throw new RuntimeException(
                        "error: not method 'createService' or 'createStaticService' or 'getService' in class "
                                + sc);
            }

        }

        Body b = sm.retrieveActiveBody();
        System.out.println(b);
        // for (Unit u: b.getUnits()) {
        // if (u instanceof JAssignStmt) {
        // JAssignStmt ass = (JAssignStmt)u;
        // if (ass.getRightOp() instanceof StringConstant) {
        // StringConstant constant = (StringConstant) ass.getRightOp();
        // return constant.value;
        // } else if (ass.getRightOp() instanceof JNewExpr) {
        // JNewExpr n = (JNewExpr)ass.getRightOp();
        // String name = makeManagerName(n.getType());
        // return name;
        // } else {
        // String name = getNameFromReturn(b);
        // break;
        // }
        // }
        // }
        // System.out.println("warning: no manager name found! "+ b);
        // return "Unknown_" +unknowni++;
        // throw new RuntimeException("error when looking for managaer!");
        String name = getNameFromReturn(b);
        System.out.println("try name: " + name);
        return name;
    }


    private String getNameFromGetInstance(SootMethod sm) {

        Body b = sm.retrieveActiveBody();
        for (Unit u: b.getUnits()){
            Stmt s = (Stmt)u;
            if (s.containsInvokeExpr()) {
                InvokeExpr ie = (InvokeExpr)s.getInvokeExpr();
                String name = ie.getMethodRef().name();
                if (name.equals("getService")|| name.equals("getSystemService")) {
                    List<Value> args = ie.getArgs();
                    int size = args.size();
                    Value v = args.get(size-1);
                    if (v instanceof StringConstant) {
                        StringConstant c = (StringConstant)v;
                        return c.value;
                    } else {
                        throw new RuntimeException("error: expected constant string: "+ b);
                    }

                }
            }
        }
        throw new RuntimeException("error: nothing found, expected constant string: "+ b);
    }

    private String getNameFromReturn(Body b) {
        ExceptionalUnitGraph g = new ExceptionalUnitGraph(b);
        SmartLocalDefs localDefs = new SmartLocalDefs(g,
                new SimpleLiveLocals(g));
        SimpleLocalUses localUses = new SimpleLocalUses(g, localDefs);

        for (Unit u : b.getUnits()) {
            if (u instanceof JReturnStmt) {
                JReturnStmt ret = (JReturnStmt) u;
                Local l = (Local) ret.getOp();
                List<Unit> defs = localDefs.getDefsOfAt(l, u);
                for (Unit d : defs) {
                    if (d instanceof AssignStmt) {
                        AssignStmt ass = (AssignStmt) d;
                        // if (ass.getRightOp() instanceof NewExpr) {
                        // JNewExpr n = (JNewExpr)ass.getRightOp();
                        Type t = ass.getRightOp().getType();
                        if (t.toString().equals("java.lang.Object")) {

                            if (ass.getRightOp() instanceof InvokeExpr) {
                                InvokeExpr ie = (InvokeExpr) ass.getRightOp();
                                t = ie.getMethodRef().declaringClass()
                                        .getType();
                            } else {

                                List<Unit> defs2 = localDefs.getDefsOfAt(
                                        (Local) ass.getRightOp(), d);
                                Unit u2 = defs2.get(0);
                                if (u2 instanceof AssignStmt) {
                                    AssignStmt ass2 = (AssignStmt) u2;
                                    t = ass.getRightOp().getType();
                                } else {
                                    throw new RuntimeException(
                                            "error: could not find interesting type!");
                                }
                            }
                        }
                        String name = makeManagerName(t);
                        return name;
                        // }
                    }
                }
            }
        }
        System.out.println("warning: no name found! " + b);
        return "Unknown" + unknowni++;
    }

    private String makeManagerName(Type type) {
        String n = type.toString().replaceAll("Manager$", "");
        String[] s = n.split("\\.");
        n = s[s.length - 1];
        n = n.toLowerCase();
        System.out.println("n: " + n);
        // for telephony service associated with "phone" service id
        if (n.equals("telephony")) {
            return new String("phone");
        }

        if (n.equals("windowmanagerimpl"))
            return "window";

        if (n.equals("ctx"))
            return "notification";

        if (n.equals("nsd")) // network service discovery
            return "servicediscovery";

        // compare against all service IDs
        Set<String> serviceId = new HashSet<String>();
        serviceId.addAll(ServiceMapper.v().getServiceIdToSootClass().keySet());
        serviceId.addAll(ServiceMapper.v().getServiceIdCalledByMethods()
                .keySet());
        serviceId.addAll(ServiceMapper.v().getContextServiceId());
        for (String k : serviceId) {
            if (k == null)
                continue; // TODO fix this
            String serviceName = k.toLowerCase();// k.replaceAll("[^a-z0-9]",
                                                 // "");
            System.out.println("comparing '" + serviceName + "' with '" + n
                    + "'");
            if (serviceName.equals(n) || n.startsWith(serviceName)
                    || n.endsWith(serviceName)) {
                System.out.println("returning '" + k + "'");
                return k;
            }
            String serviceName2 = serviceName.replaceAll("_", "");
            if (serviceName2.equals(n) || n.startsWith(serviceName2)
                    || n.endsWith(serviceName2)) {
                System.out.println("returning " + k);
                return k;
            }
            // replace network by net
            if (n.matches(".*network.*")) {
                String net = n.replaceAll("network", "net");
                if (serviceName.equals(net)) {
                    System.out.println("returning (net)'" + k + "'");
                    return k;
                }
            }
        }

        System.out.println("warning: returning '" + "unknown" + "'");
        return "Unkown" + unknowni++;
    }

    private void writeManagerConfig(Map<String, String> managerType2getMethod, String fn) { 
        BufferedWriter bw;
        try {
            File f = new File(fn);
            f.getParentFile().mkdirs();
            f.createNewFile();
            bw = new BufferedWriter(new FileWriter(f));
            for (String k : managerType2getMethod.keySet()) {
                String classname = k;
                String method = managerType2getMethod.get(k);
                String line = classname + " " + method + "\n";
                bw.write(line);
            }
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("error when writting manager config to " + fn);
            System.exit(-1);
        }
    }

}
