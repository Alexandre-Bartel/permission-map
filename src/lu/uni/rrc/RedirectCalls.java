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

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lu.uni.rrc.contentprovider.Provider;
import lu.uni.rrc.managers.HandleManagers;
import lu.uni.rrc.services.GenerateServiceInit;
import lu.uni.rrc.services.HandleServices;
import lu.uni.rrc.services.ServiceMapper;
import soot.Body;
import soot.FastHierarchy;
import soot.Local;
import soot.PackManager;
import soot.Scene;
import soot.SceneTransformer;
import soot.BodyTransformer;
import soot.SootClass;
import soot.SootMethod;
import soot.SootMethodRef;
import soot.Transform;
import soot.Unit;
import soot.jimple.InterfaceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.Jimple;
import soot.jimple.SpecialInvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.spark.SparkTransformer;
import soot.jimple.toolkits.callgraph.CHATransformer;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.jimple.toolkits.scalar.DeadAssignmentEliminator;
import soot.jimple.toolkits.scalar.NopEliminator;
import soot.jimple.toolkits.scalar.UnreachableCodeEliminator;
import soot.options.Options;
import soot.tagkit.Tag;
import soot.toolkits.exceptions.TrapTightener;
import soot.toolkits.scalar.UnusedLocalEliminator;


public class RedirectCalls {
	
	private static final Logger logger = LoggerFactory.getLogger(RedirectCalls.class);

    static boolean WANT_PERMISSION_STRINGS_INFO = false;
    static Config config = new Config();

  
    /**
     * 
     * @param args
     */
    public static void main(String[] args) {

        List<String> argsRaw = new ArrayList<String>(Arrays.asList(args));
        List<String> argsList = new ArrayList<String>();

        // parse non-Soot parameters
        int SKIP_ARGS = 4;
        try {
            // config
            String cfgFileFlag = argsRaw.get(0);
            String cfgFilePath = argsRaw.get(1);

            if (!cfgFileFlag.equals("-cfg"))
                throw new Exception("Expected -cfg flag! not '" + cfgFileFlag
                        + "'");
            config.loadConfig(cfgFilePath);

            // content providers
            String cpFileFlag = argsRaw.get(2);
            String cpFilePath = argsRaw.get(3);

            if (!cpFileFlag.equals("-cp"))
                throw new Exception("Expected -cp flag! not '" + cfgFileFlag + "'");
            Provider.loadCP(cpFilePath);

        } catch (Throwable e) {
            System.err.println("error: " + e.getMessage());
            usage();
            System.exit(-1);
        }

        // continue with Soot parameters
        for (int i = SKIP_ARGS; i < argsRaw.size(); i++) {
            argsList.add(argsRaw.get(i));
        }
        // argsList.addAll(Arrays.asList(new String[]{
        // "-w"
        // }));

        PackManager.v().getPack("wjtp")
                .add(new Transform("wjtp.myTrans", new SceneTransformer() {
                    @Override
                    protected void internalTransform(String phaseName,
                            Map options) {

                        // SootClass sc =
                        // Scene.v().getSootClass("android.app.ContextImpl");
                        // for (SootMethod sm: sc.getMethods()) {
                        // System.out.println("sm: "+ sm);
                        // }
                        // System.exit(-1);

                        System.out.println("Redirect Calls started.");
                        // SootClass view =
                        // Scene.v().getSootClass("android.view.ViewDebug$ExportedProperty");
                        // System.out.println("tags for Viewe...");
                        for (SootClass sc : Scene.v().getClasses()) {
                            for (Tag ct : sc.getTags()) {
                                // System.out.println("class tag ("+ sc +"): "+
                                // ct.toString());
                            }
                            // sc.removeAllTags(); //.removeTag("Visibility");
                            for (SootMethod sm : sc.getMethods()) {
                                for (Tag mt : sc.getTags()) {
                                    // System.out.println("   method tag ("+ sm
                                    // +"): "+ mt.toString());
                                }
                                sm.removeAllTags();
                            }
                        }
                        // for (Tag t: view.getTags()) {
                        // System.out.println("tag: "+ t.toString());
                        // }
                        // if (1>0)
                        // return;

                        if (RedirectCalls.WANT_PERMISSION_STRINGS_INFO) {
                            System.out.println("start FPS...");
                            long fps_start = System.currentTimeMillis();
                            FindPermissionString fps = new FindPermissionString();
                            fps.find();
                            long fps_end = System.currentTimeMillis()
                                    - fps_start;
                            System.out.println("FPS has run for " + fps_end
                                    + "");
                            // /////////////////////////////////////////////////
                            System.out.println("exiting...");
                            System.exit(0);// //////////////////////////////////
                            // /////////////////////////////////////////////////
                        }

                        RedirectCalls rc = new RedirectCalls();
                        rc.redirectCalls();

                        System.out.println("Redirect Calls ended.");
                        // System.exit(0);
                        
//                        // correct bytecode for invoke-interface
//                        for (SootClass sc: Scene.v().getClasses()) {
//                        	for (SootMethod sm: sc.getMethods()) {
//                        		if (!sm.isConcrete())
//                        			continue;
//                        		System.out.println("checking method "+ sm);
//                        		Body b = sm.retrieveActiveBody();
//    
//                        	}
//                        }

            }
                }));
        
       

        // enable whole program mode
        argsList.add("-w");

        // //argsList.add("-ws");

//        argsList.add("-full-resolver");
        //
        
//        argsList.add("-p");
//        argsList.add("bb.lso");
//        argsList.add("enabled:false");
        
        
        // //
//        argsList.add("-p");
//        argsList.add("wjop");
//        argsList.add("enabled:false");
//
//        argsList.add("-p");
//        argsList.add("wjtp");
//        argsList.add("enabled:true");
//
//        argsList.add("-p");
//        argsList.add("wjap");
//        argsList.add("enabled:false");
//
//        argsList.add("-p");
//        argsList.add("jtp");
//        argsList.add("enabled:false");
//
//        argsList.add("-p");
//        argsList.add("jap");
//        argsList.add("enabled:false");
//
//        argsList.add("-p");
//        argsList.add("jop");
//        argsList.add("enabled:false");

        // call graph
        argsList.add("-p");
        argsList.add("cg");
        argsList.add("enabled:false");
        //
        // argsList.add("-p");
        // argsList.add("cg");
        // argsList.add("verbose:false");
        // //
        // argsList.add("-p");
        // argsList.add("cg.cha");
        // argsList.add("enabled:false");
        // //
        // argsList.add("-p");
        // argsList.add("cg");
        // argsList.add("safe-forname:false");
        //
        // // Spark
        // argsList.add("-p");
        // argsList.add("cg.spark");
        // argsList.add("enabled:false");
        //
        // // Paddle
        // argsList.add("-p");
        // argsList.add("cg.paddle");
        // argsList.add("enabled:false");

        args = argsList.toArray(new String[0]);

        System.out.println("arguremst to soot: "+ argsList);
        
        try {
        	soot.Main.main(args);
        } catch (Throwable t) {
        	
        }
        
        // move files
        File outputDir = new File(Options.v().output_dir());
        File systeminitDir = new File(outputDir.getParentFile().getAbsolutePath() + File.separatorChar + "systeminit.code");
        systeminitDir.mkdirs();
        File systemClass = new File(Options.v().output_dir() + File.separatorChar + GenerateServiceInit.servicesInitClassName +".class");
        systemClass.renameTo(new File(systeminitDir.getAbsolutePath() + File.separatorChar + GenerateServiceInit.servicesInitClassName +".class"));
        
        
        
    }


    private static void usage() {
        System.out
                .println("Usage: java RedirectCalls -c <path/to/config/file>");
    }

    /**
     * 
     */
    public void redirectCalls() {

        FastHierarchy fh = Scene.v().getOrMakeFastHierarchy();
        SootClass iinterface = Scene.v().getSootClass("android.os.IInterface");
        SootClass binder = Scene.v().getSootClass("android.os.Binder");

        if (iinterface == null)
            throw new RuntimeException("android.os.IInterface not found!");

        long getServices_start = System.currentTimeMillis();

        ServiceMapper.v(); // initialize service mapper

        // Compute target methods for all modules
        FindTargetMethod.v().addInterest("method", "thread.start", HandleThreads.threadStartSig);
        FindTargetMethod.v().addInterest("method", "thread.run", HandleThreads.threadRunSig);

        FindTargetMethod.v().addInterest("method", "message.post", HandleMessages.post);
        FindTargetMethod.v().addInterest("method", "message.sendEmpty", HandleMessages.sendEmpty);
        FindTargetMethod.v().addInterest("method", "message.sendMessage",
                HandleMessages.sendMessage);

        HandleIntents tmpi = new HandleIntents(new ArrayList<Set<SootMethod>>());
        FindTargetMethod.v().addInterest("method", "intent", tmpi.sigList);

        HandleContentProviders tmpcp = new HandleContentProviders(new ArrayList<Set<SootMethod>>());
        FindTargetMethod.v().addInterest("method", "contentProvider", tmpcp.sigList);

        FindTargetMethod.v().addInterest("r_method", "publishService1", HandleServices.r_publishService1Sig);
        FindTargetMethod.v().addInterest("r_method", "publishService2", HandleServices.r_publishService2Sig);
		FindTargetMethod.v().addInterest("method", "addService1", HandleServices.addService1Sig);
        FindTargetMethod.v().addInterest("method", "addService2", HandleServices.addService2Sig);
        FindTargetMethod.v().addInterest("method", "startSystemService", HandleServices.startSystemServiceSig);
        FindTargetMethod.v().addInterest("method", "getService", HandleServices.getServiceSig);
        FindTargetMethod.v().addInterest("r_method", "getSystemService", HandleServices.r_getSystemServiceSig);
        FindTargetMethod.v().addInterest("r_method", "asInterface", HandleServices.r_asInterfaceSig);

        FindTargetMethod.v().addInterest("method", "addManager", HandleManagers.registerManagerSig);
        FindTargetMethod.v().addInterest("r_method", "getManager", HandleManagers.r_getManagerSig);

        FindTargetMethod.v().addInterest("field", "context", "android.content.Context");
        FindTargetMethod.v().addInterest("method", "getActivityManager",
                HandleActivityManager.getActivityManagerSig);
        
        FindTargetMethod.v().computeInterests();

        // Do work on methods
        HandleThreads ht = new HandleThreads(FindTargetMethod.v().getInterest("method",
                "thread.start"), FindTargetMethod.v().getInterest("method", "thread.run"));
        ht.doWork();

        HandleMessages hm = new HandleMessages(FindTargetMethod.v().getInterest("method",
                "message.post"), FindTargetMethod.v().getInterest("method", "message.sendEmpty"),
                FindTargetMethod.v().getInterest("method", "message.sendMessage"));
        hm.doWork();

        HandleIntents hi = new HandleIntents(FindTargetMethod.v().getInterest("method", "intent",
                HandleIntents.sigList));
        hi.doWork();

        HandleContentProviders hcp = new HandleContentProviders(FindTargetMethod.v().getInterest(
                "method", "contentProvider", HandleContentProviders.sigList));
        // hcp.doWork() // ServiceInit class has to exist before calling this

        HandleContext hc = new HandleContext(FindTargetMethod.v().getInterest("field", "context"));
        // hc.doWork() // ServiceInit class has to exist before calling this

        HandleActivityManager ham = new HandleActivityManager(FindTargetMethod.v().getInterest(
                "method", "getActivityManager"));
        // ham.doWork() // ServiceInit class has to exist before calling this

        HandleServices hs = new HandleServices(
        		FindTargetMethod.v().getInterest("r_method", "publishService1"),
        		FindTargetMethod.v().getInterest("r_method", "publishService2"),
        		FindTargetMethod.v().getInterest("method", "addService1"),
        		FindTargetMethod.v().getInterest("method", "addService2"),
        		FindTargetMethod.v().getInterest("method", "startSystemService"),
        		FindTargetMethod.v().getInterest("method", "getService"),
        		FindTargetMethod.v().getInterest("r_method", "getSystemService"),
                FindTargetMethod.v().getInterest("r_method", "asInterface")
                );
        hs.doWork();

        HandleManagers hmgrs = new HandleManagers(FindTargetMethod.v().getInterest("method",
                "addManager"), FindTargetMethod.v().getInterest("r_method", "getManager"));
        hmgrs.doWork();

        // // get system services
        // SootClass serverThreadSC =
        // Scene.v().getSootClass(ServiceMapper.v().serverThread.getName());
        // System.out.println(" [*] Get system services initialized in server thread.");
        // ServiceMapper.v().getServerThreadServiceMapping(serverThreadSC);
        //
        // // Loop on all method adding a service
        // for (SootMethod sm : FindTargetMethod.v().getInterest("method",
        // "addService")) {// Scene.v().getClasses())
        // // {
        // if
        // (!(sm.getDeclaringClass().getName().equals(ServiceMapper.v().serverThread)))
        // {
        // // get services not initialized in server thread
        // System.out.println(" [*] Get other services not initialized in server thread.");
        // ServiceMapper.v().getServiceMappingForMethod(sm);
        // }
        // }
        //
        // // Loop on methods registering a service (Manager)
        // for (SootMethod sm : FindTargetMethod.v().getInterest("method",
        // "registerService")) {
        // System.out.println(" [*] Get register service");
        // ServiceMapper.v().getRegisterServiceMapping(sm);
        // }
        //
        //
        //
        // // get services constructed on the fly directly in getSystemService()
        // //
        // System.out.println(" [*] Get services constructed on-the-fly in getSystemService()");
        // // SootClass otf_sc = Scene.v().getSootClass("android.)
        // // ServiceMapper.v().getOnTheFlyServices(otf_sc);
        //
        // long getServices_end = System.currentTimeMillis() -
        // getServices_start;
        // System.out.println("getServices has run for " + getServices_end +
        // "");
        // System.out.println();
        //
        // // print service mapping
        // System.out.println("service number: "
        // + ServiceMapper.v().getServiceIdToSootClass().keySet().size());
        // for (String k : ServiceMapper.v().getServiceIdToSootClass().keySet())
        // {
        // SootClass sc = ServiceMapper.v().getServiceIdToSootClass().get(k);
        // System.out.println(k + ":   " + sc);
        // }
        // System.out.println();
        //
        // // going through all bytecode to see where getSystemService() is
        // called
        // RedirectServiceManager rsm = new RedirectServiceManager();
        // HashMap<String, List<String>> serviceIdCalledByMethods =
        // ServiceMapper
        // .v().getServiceIdCalledByMethods();
        //
        // Set<String> calledServicesSet = new HashSet<String>();
        // Set<String> calledSystemServicesSet = new HashSet<String>();
        //
        // for (SootClass sc : Scene.v().getClasses()) {
        //
        // // 1. filter classes
        // if (!sc.getName().startsWith("android.")
        // && !sc.getName().startsWith("com.android."))
        // continue;
        //
        // for (SootMethod sm : sc.getMethods()) {
        //
        // // 2. filter methods
        // if (!sm.isConcrete())
        // continue;
        // if (sm.getName().equals("getSystemService")) {
        // System.out.println("skipping " + sm);
        // continue;
        // }
        // Body b = sm.retrieveActiveBody();
        // if (b == null)
        // continue;
        //
        // // 3. check if the method has a call to getSystemService
        // List<Unit> getSystemServiceList = rsm
        // .hasCallToSystemServices(b);
        // List<Unit> getServiceList = rsm.hasCallToService(b);
        //
        // List<String> systemServicesCalled = ServiceMapper.v()
        // .getServiceNames(b, getSystemServiceList);
        // List<String> servicesCalled = ServiceMapper.v()
        // .getServiceNames(b, getServiceList);
        //
        // for (String s : servicesCalled) {
        // if (s == null) {
        // System.out.println("warning: null service call!!");
        // continue;
        // }
        // calledServicesSet.add(s);
        // }
        // for (String s : systemServicesCalled) {
        // if (s == null) {
        // System.out.println("warning: null system service call!!");
        // continue;
        // }
        // calledSystemServicesSet.add(s);
        // }
        //
        // String serviceNames = "";
        // // 4. add method that call getSystemService
        // for (String s : systemServicesCalled) {
        // if (serviceIdCalledByMethods.containsKey(s)) {
        // List<String> mList = serviceIdCalledByMethods.get(s);
        // mList.add(sm.toString());
        // } else {
        // List<String> mList = new ArrayList<String>();
        // mList.add(sm.toString());
        // serviceIdCalledByMethods.put(s, mList);
        // }
        // serviceNames += " " + s;
        // }
        // }
        // }
        //
        // // print info about called and initialized services
        // for (String k1 :
        // ServiceMapper.v().getServiceIdToSootClass().keySet()) {
        // System.out.print(" [*] service that can be initialized: " + k1);
        // boolean found = false;
        // for (String k2 : calledServicesSet) {
        // if (k1.equals(k2)) {
        // System.out.println(" is called -> " + k2);
        // found = true;
        // break;
        // }
        // }
        // if (!found) {
        // System.out.println();
        // }
        // }
        // for (String k1 : calledServicesSet) {
        // System.out.print(" [*] service that is called: " + k1);
        // boolean found = false;
        // for (String k2 : ServiceMapper.v().getServiceIdToSootClass()
        // .keySet()) {
        // if (k1.equals(k2)) {
        // System.out.println(" is init -> " + k2);
        // found = true;
        // break;
        // }
        // }
        // if (!found) {
        // System.out.println();
        // }
        // }
        //

        // create class to initialize services
        System.out.println(" [*] Create Service Initialization class.");
        GenerateServiceInit gsi = new GenerateServiceInit();
        SootClass servicesInitClass = gsi.createServicesInitClass(hs.getInitServicesMap());
        // create managers
        gsi.createManagers(servicesInitClass, hmgrs.getManagers());

        hs.redirect(servicesInitClass);
        hmgrs.redirect(servicesInitClass);

        // // redirect call to system services
        // System.out.println(" [*] Redirect calls to services.");
        // RedirectIntent ri = new RedirectIntent();
        // RedirectContentProvider rcp = new RedirectContentProvider();
        // for (SootClass sc : Scene.v().getClasses()) {
        // if (!sc.getName().startsWith("android.")
        // && !sc.getName().startsWith("com.android.")
        // && !sc.getName().startsWith("ServicesInit"))
        // continue;
        // for (SootMethod sm : sc.getMethods()) {
        // if (!sm.isConcrete())
        // continue;
        // if (sm.getName().equals("getSystemService")) {
        // System.out.println("skipping " + sm);
        // continue;
        // }
        // Body b = sm.retrieveActiveBody();
        // if (b == null)
        // continue;
        //
        // System.out.println("Handling method " + sm);
        //
        // // redirect services
        // if (Config.doRedirectService()) {
        // rsm.redirectInstanceOf(b);
        // List<Unit> getSystemService = rsm
        // .hasCallToSystemServices(b);
        // if (getSystemService.size() != 0) {
        // List<String> servicesCalled = ServiceMapper.v()
        // .getServiceNames(b, getSystemService);
        // System.out.println("Redirecting to static service... "
        // + sm + " calls ");
        // for (String s : servicesCalled)
        // System.out.println("service: " + s);
        // rsm.redirectGetSystemServiceCalls(sm.getActiveBody(),
        // getSystemService, servicesCalled,
        // servicesInitClass);
        // }
        // }
        // // redirect intents
        // if (Config.doRedirectIntents()) {
        // System.out.println("Redirecting Intent...");
        // ri.locateAndRedirectIntent(b);
        // }
        //
        // // redirect content providers
        // if (Config.doRedirectContentProviders()) {
        // System.out.println("Redirecting Content Provider...");
        // rcp.locateAndRedirectContentProvider(b);
        // }
        //
        // }
        // }
        //
        // ServiceInit class has to exist before calls to content providers
        // are redirected
        hcp.doWork();

        hc.doWork();

        ham.doWork();

        // Empty method related to activity lifecycle in
        // ActivityManagerService
        // to speed up call graph analysis.
        HandleActivityLifeCycle.empty();

        for (SootClass sc : Scene.v().getClasses()) {
            for (SootMethod sm : sc.getMethods()) {
                if (!sm.isConcrete())
                    continue;
                Body b = sm.retrieveActiveBody();
                if (b == null)
                    continue;
                
                logger.debug("correcting methods if necessary...");
        		for (Unit u: b.getUnits()) {
        			Stmt s  = (Stmt)u;
        			if (!s.containsInvokeExpr())
        				continue;
        			InvokeExpr ie = (InvokeExpr) s.getInvokeExpr();
        			//System.out.println("invoketype: "+ ie.getClass());
        			if ((ie instanceof InterfaceInvokeExpr)) {
	        			InterfaceInvokeExpr iie = (InterfaceInvokeExpr)ie;
	        			SootMethodRef smr = iie.getMethodRef();
	        			//System.out.println("dcinterface?: "+ smr.declaringClass().isInterface() +" - "+ iie.getMethod().getDeclaringClass().isInterface() +" method abstract?"+ iie.getMethod().isAbstract());
	        			if (!smr.declaringClass().isInterface()) {
	        				System.out.println("Warning: error in bytecode. Updating invoke-interface instruction in "+ sm);
	        				s.getInvokeExprBox().setValue(Jimple.v().newVirtualInvokeExpr((Local) iie.getBase(), iie.getMethodRef(), iie.getArgs()));
	        			}
        			} else if (ie instanceof SpecialInvokeExpr) {
	        			SpecialInvokeExpr iie = (SpecialInvokeExpr)ie;
	        			SootMethodRef smr = iie.getMethodRef();
	        			//System.out.println("dcinterface?: "+ smr.declaringClass().isInterface() +" - "+ iie.getMethod().getDeclaringClass().isInterface() +" method abstract?"+ iie.getMethod().isAbstract());
	        			if (iie.getMethod().getDeclaringClass().isAbstract() || iie.getMethod().getDeclaringClass().isInterface()) {
	        				System.out.println("Warning: error in bytecode. Updating invoke-special instruction in "+ sm);
	        				s.getInvokeExprBox().setValue(Jimple.v().newVirtualInvokeExpr((Local) iie.getBase(), iie.getMethodRef(), iie.getArgs()));
	        			}
        			}
        		}
                

                logger.debug("Transforming method " + sm);
                
                

                TrapTightener.v().transform(b);
                // Aggregator.v().transform(b);
                // CopyPropagator.v().transform(b);
                // ConditionalBranchFolder.v().transform(b);
                UnreachableCodeEliminator.v().transform(b);
                DeadAssignmentEliminator.v().transform(b);
                UnusedLocalEliminator.v().transform(b);
                NopEliminator.v().transform(b);
            }
        }

        //
        // // print methods which connect to a service
        // System.out.println();
        // for (String k : serviceIdCalledByMethods.keySet()) {
        // System.out.println("service '" + k + "'");
        // for (String m : serviceIdCalledByMethods.get(k)) {
        // System.out.println("  " + m);
        // }
        // }
        //
        // // print the list of called services
        // System.out.println();
        // List<String> sList = new ArrayList<String>();
        // for (String s : serviceIdCalledByMethods.keySet()) {
        // if (s != null) {
        // sList.add(s);
        // } else {
        // System.out.println("warning: null service called by methods keyset!!! FIXME!");
        // }
        // }
        // System.out.println("called services (" + sList.size() + "):");
        // Collections.sort(sList);
        // for (String k : sList) {
        // System.out.println("  " + k);
        // }
        //
        // // System.out.println();
        // // List<String> sssList = new
        // // ArrayList<String>(serviceIdToSootClass.keySet());
        // // System.out.println("services initialized in system server ("+
        // // sssList.size() +"):");
        // // Collections.sort(sssList);
        // // for (String k: sssList) {
        // // System.out.println("  "+ k);
        // // }
        // //
        // // System.out.println();
        // // System.out.println("not initialized by system server:");
        // // for (String k: sList) {
        // // if (!sssList.contains(k)) {
        // // System.out.println("  "+ k);
        // // }
        // // }
        //
        // // print services numbers
        // int nbrServerThreadServices = ServiceMapper.v()
        // .getServerThreadServices().size();
        // int nbrServices =
        // ServiceMapper.v().getServiceIdToSootClass().keySet()
        // .size();
        // int calledServices = serviceIdCalledByMethods.keySet().size();
        // System.out.println("# server thread services: "
        // + nbrServerThreadServices);
        // for (String s : ServiceMapper.v().getServerThreadServices())
        // System.out.println("   " + s);
        // System.out.println("# services: " + nbrServices);
        // for (String s : ServiceMapper.v().getServiceIdToSootClass().keySet())
        // System.out.println("   " + s);
        // System.out.println("# called services: " + calledServices);
        // for (String s : serviceIdCalledByMethods.keySet())
        // if (!(ServiceMapper.v().getServerThreadServices().contains(s)))
        // System.out.println("   NOT in server thread: " + s);
        //
        // // print if generated from .aidl or not
        // HashMap<SootClass, SootClass> sFromAidl = ServiceMapper.v()
        // .checkServicesGeneratedFromAidl();
        //
        // // print services generated from aidl
        // System.out.println("generated from .aidl(" + sFromAidl.size() + ")");
        // for (SootClass s : sFromAidl.keySet())
        // System.out.println(s);
        // System.out.println();
        //
        // // print services NOT generated from aidl
        // for (SootClass s :
        // ServiceMapper.v().getConcreteServicesToInterfaces()
        // .keySet()) {
        // if (!(sFromAidl.containsKey(s))) {
        // System.out.println("NOT generated from .aidl: " + s);
        // }
        // }
        // System.out.println();
        //
        // // Find class containing an aidl interface field (should be the
        // manager
        // // class associated to the service)
        // HashMap<SootClass, List<SootClass>> concreteServiceToManager = new
        // HashMap<SootClass, List<SootClass>>();
        // HashMap<SootClass, Set<SootClass>> aidlInterfaceToConcreteService =
        // new HashMap<SootClass, Set<SootClass>>();
        // for (SootClass s : sFromAidl.keySet()) {
        // SootClass aidlC = sFromAidl.get(s);
        // if (aidlInterfaceToConcreteService.containsKey(aidlC)) {
        // aidlInterfaceToConcreteService.get(aidlC).add(s);
        // } else {
        // Set<SootClass> concreteServices = new HashSet<SootClass>();
        // concreteServices.add(s);
        // aidlInterfaceToConcreteService.put(aidlC, concreteServices);
        // }
        //
        // }
        // // Find Managers
        // // for (SootClass sc: Scene.v().getClasses()) {
        // // if (sc.getName().endsWith("Manager")) {
        // // for (SootMethod c: sc.getMethods()) {
        // // if (!(c.getName().equals("<init>"))){
        // // continue;
        // // }
        // // // only constructors
        // // for (SootClass type: aidlInterfaceToConcreteService.keySet()) {
        // // if (f.getType().toString().equals(type.getName().toString())) {
        // // System.out.println("f type: "+ f.getType());
        // // System.out.println("type :"+ type);
        // // if (concreteServiceToManager.containsKey(type)) {
        // // concreteServiceToManager.get(type).add(s);
        // // } else {
        // // List<SootClass> managers = new ArrayList<SootClass>();
        // // managers.add(s);
        // // concreteServiceToManager.put(type, managers);
        // // }
        // // }
        // // }
        // // }
        // // }
        // // }
        // // for (SootClass s: Scene.v().getClasses()) {
        // // for (SootField f: s.getFields()) {
        // // for (SootClass type: aidlInterfaceToConcreteService.keySet()) {
        // // if (f.getType().toString().equals(type.getName().toString())) {
        // // System.out.println("f type: "+ f.getType());
        // // System.out.println("type :"+ type);
        // // if (concreteServiceToManager.containsKey(type)) {
        // // concreteServiceToManager.get(type).add(s);
        // // } else {
        // // List<SootClass> managers = new ArrayList<SootClass>();
        // // managers.add(s);
        // // concreteServiceToManager.put(type, managers);
        // // }
        // // }
        // // }
        // // }
        // // }
        // // print managers
        // System.out.println();
        // for (SootClass concreteService : concreteServiceToManager.keySet()) {
        // System.out.println(concreteService);
        // for (SootClass manager : concreteServiceToManager
        // .get(concreteService)) {
        // System.out.println("   " + manager);
        // }
        // }
        // System.out.println();

    }





}
