// 
// (c) 2012 University of Luxembourg Interdisciplinary Centre for 
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

package lu.uni.fpc.cha;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lu.uni.fpc.EntryPointKey;
import lu.uni.fpc.Output;
import lu.uni.fpc.graph.OutputGraph;
import lu.uni.fpc.graph.Tarjan;
import lu.uni.fpc.identity.ClearRestoreCallingIdentity;
import lu.uni.fpc.string.CheckForPermission;
import soot.PackManager;
import soot.Scene;
import soot.SceneTransformer;
import soot.SootMethod;
import soot.Transform;
import soot.jimple.toolkits.callgraph.CHATransformer;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.options.Options;

public class CHAFindPermissionChecks {

    private static long cha_start = 0;
    private static CallGraph permCg = new CallGraph();
    private static CallGraph cg = null;
    // 1 build call graph
    // 2 remove scc
    // 3 profit

    private static Logger logger = LoggerFactory.getLogger(CHAFindPermissionChecks.class);

    // private static void initLogger() {
    // // default logger prints on console
    // BasicConfigurator.configure();
    // // log stuff >= INFO
    // logger.setLevel(Level.INFO);
    // }

    protected void generateCHACallGraph(Map options) {
        CHATransformer.v().transform();
    }

    private static void addPermissionFromMethod(SootMethod from, SootMethod to) {
        if (!methodToPermissionSet.containsKey(from))
            throw new RuntimeException("error: methodToPermissionSet does not contain key '" + from
                    + "'");
        Set<String> pSet = methodToPermissionSet.get(from);
        // System.out.println(" Permission for method "+ from);
        // for (String p: pSet)
        // System.out.println("    p: "+ p);
        if (methodToPermissionSet.containsKey(to)) {
            methodToPermissionSet.get(to).addAll(pSet);
        } else {
            methodToPermissionSet.put(to, pSet);
        }
    }

    // protected static void computeEntryPointPermissions(CallGraph cg,
    // SootMethod sm, int depth) {
    // assert depth <= 2;
    // if (depth == 1) {
    // currentWrapper = sm;
    // }
    // if (depth == 2) {
    // currentEntryPoint = sm;
    // EntryPointKey k = new EntryPointKey(currentWrapper, currentEntryPoint);
    // entryPoints2PermissionsMap.put(k,
    // methodToPermissionSet.get(currentEntryPoint));
    // return;
    // }
    // Iterator<Edge> it = cg.edgesOutOf(sm);
    // while (it.hasNext()) {
    // Edge e = it.next();
    // computeEntryPointPermissions(cg, e.tgt(), depth+1);
    // }
    //
    // }

    private void addPermissionsToCurrentMethod(Map<SootMethod, Set<String>> methodToPermissionSet,
            HashSet<String> permissions, SootMethod sm) {
        if (!CheckForPermission.isCheckPermissionMethod(sm)) {
            if (methodToPermissionSet.containsKey(sm)) {
                methodToPermissionSet.get(sm).addAll(permissions);
            } else {
                methodToPermissionSet.put(sm, new HashSet<String>(permissions));
            }
        }

    }

    // private void printMethod2PSet() {
    // for (SootMethod sm : methodToPermissionSet.keySet()) {
    // logger.info("printm2ps: " + sm);
    // for (String p : methodToPermissionSet.get(sm)) {
    // logger.info("  " + p);
    // }
    // }
    // }

    private void printMethod(int depth, String message) {
        String s = (System.currentTimeMillis() - cha_start + " | ");
        for (int i = 0; i < depth; i++) {
            s += (" ");
        }
        s += (depth + "> " + message);
        if (ConfigCHA.v().printLiveCallGraph())
            System.out.println(s);
    }

    static SootMethod currentWrapper = null; // store method at depth 1 (class
    // wrapper)
    static SootMethod currentEntryPoint = null; // store method at depth 2
    // (method entry point)
    static EntryPointKey epk = null; // key for the current entry point (wrapper
    // + method)

    // record the list of permissions for every entry point. An entry point is
    // the couple (wrapper, method entry point)
    static Map<EntryPointKey, Set<String>> entryPoints2PermissionsMap = new HashMap<EntryPointKey, Set<String>>();

    // For every method (not just entry point methods) record the list of
    // permissions it require
    static Map<SootMethod, Set<String>> methodToPermissionSet = new HashMap<SootMethod, Set<String>>();

    static Map<SootMethod, Set<SootMethod>> methodCalled = new HashMap<SootMethod, Set<SootMethod>>();

    // Set, containing all methods for which the list of permissions has already
    // been computed.
    static Set<SootMethod> computedMethods = new HashSet<SootMethod>();

    // For cycles
    static Cycles cycles = new Cycles();

    static Set<EntryPointKey> epkeys = new HashSet<EntryPointKey>();
    static Set<SootMethod> epMethods = new HashSet<SootMethod>();
    static Set<SootMethod> wMethods = new HashSet<SootMethod>();

    /**
     * 
     * @param stack
     * @param cg
     * @param ep
     */
    public void normalRecursiveCall(Stack<SootMethod> stack, CallGraph cg, SootMethod m,
            Edge newEdge) {
        int depth = stack.size();
        // if (depth>9) {return;}
        String mString = m.toString(); // get method signature (ex:

        if (FilterCHA.v().mustSkip(stack, m)) {
            return;
        }

        if (depth == 1) {
            currentWrapper = m;
            wMethods.add(currentWrapper);
        }

        if (depth == 2) {
            currentEntryPoint = m;
            if (currentEntryPoint.toString().contains("GenerationGG") ||
            // currentEntryPoint.toString().contains("init>") ||
                    currentEntryPoint.toString().contains("ServicesInit:")) {

            } else {
                EntryPointKey epk = new EntryPointKey(currentWrapper, currentEntryPoint);
                epkeys.add(epk);
                epMethods.add(currentEntryPoint);
            }
        }

        // add new edge in call graph
        if (newEdge != null)
            permCg.addEdge(newEdge);

        // To avoid cycles, we do not analyze a method twice
        if (stack.contains(m)) // cycles.addCycleIfNecessary(stack, m))
            return;

        // If the method has already been computed, add its set of permissions
        // to all the methods in the stack.
        if (depth > 2 && computedMethods.contains(m)) {
            printMethod(depth, "ALREADY COMPUTED" + mString);
            return;
        }

        // printEdgesDebug(cg);

        // Print current method
        if (depth < 600) {
            printMethod(depth, mString);
        }

        // The current method is analyzed.
        stack.push(m);
        List<Edge> edgesFromCurrentMethod = null;

        if (!ConfigCHA.v().skipInverseIdentity()) {
            // Check for clearCallingIdentity / restoreCallingIdentity
            // Method calls between clearCI and restoreCI must not be
            // analyzed.
            Iterator<Edge> itEdge = cg.edgesOutOf(Scene.v().getMethod(m.toString()));
            while (itEdge.hasNext()) {
                Edge e = itEdge.next();
                SootMethod tgt = e.getTgt().method();
                String tgtString = tgt.toString();
                if (tgtString.contains("clearCallingIdentity(")) {
                    printMethod(depth + 1, "> CONTAINS CLEAR CALLING ID " + mString); // depth
                                                                                      // +
                                                                                      // 1
                                                                                      // since
                                                                                      // m
                                                                                      // was
                                                                                      // pushed
                                                                                      // on
                                                                                      // the
                    // stack
                    edgesFromCurrentMethod = ClearRestoreCallingIdentity
                            .removeBetweenClearAndRestoreIdentity(m, cg);
                    for (Edge ee : edgesFromCurrentMethod) {
                        logger.info("handling those edges: " + ee);
                    }
                    break;
                }
            }
        }
        if (edgesFromCurrentMethod == null) {
            // logger.info("get edges from method: "+ m.toString());
            edgesFromCurrentMethod = new ArrayList<Edge>();
            Iterator<Edge> itEdge = cg.edgesOutOf(Scene.v().getMethod(m.toString()));
            while (itEdge.hasNext()) {
                edgesFromCurrentMethod.add(itEdge.next());
            }
        }

        // Analyze method calls from the current method's body
        // Permission(s) checked for this method (if any)

        // logger.info("");
        // for (Edge e : edgesFromCurrentMethod) {
        // logger.info("DEBUG edge: "+ e.tgt());
        // }
        // logger.info("");

        for (Edge e : edgesFromCurrentMethod) {
            // logger.info("handling edge: "+ e);
            HashSet<String> methodPermissionSet = new HashSet<String>();
            boolean isCheckMethod = CheckForPermission.isCheckPermissionMethod(e.tgt());

            // do not analyze permission check methods which are at depth 2
            if (depth == 1 && isCheckMethod) {
                logger.info("skip depth 2 check method: " + e);
                continue;
            }

            boolean theDestNodeIsAPermCheckMethod = theDestNodeIsAPermCheckMethod = CheckForPermission
                    .checkForPermission(e, methodPermissionSet, stack);
            if (theDestNodeIsAPermCheckMethod) {
                // System.err.println(m.toString());
                // CheckForPermission.printLogValue();
                int permSetSize = methodPermissionSet.size();
                System.err.println("perm set size: " + permSetSize);

                addPermissionsToCurrentMethod(methodToPermissionSet, methodPermissionSet, m);

                // if (depth >= 2)
                // entryPoints2PermissionsMap.get(epk).addAll(methodPermissionSet);
                printMethod(depth + 1, "> " + e.tgt() + " has permissions: stopping here"); // depth
                                                                                            // +
                                                                                            // 1
                                                                                            // since
                // m was pushed
                // on the stack
                for (String p : methodPermissionSet) {
                    logger.info("[FFF] " + p);
                }
                // logger.info(ep);
                // logger.info("    p> continue;");
                continue;
            }
            // logger.info("type is: "+ e.kind());
            Edge newedge = new Edge(e.getSrc(), e.srcUnit(), e.getTgt(), e.kind());
            normalRecursiveCall(stack, cg, e.tgt(), newedge);
        }

        if (depth > 2 && (m.toString().contains(" check") || m.toString().contains(" validateP"))) { // TODO:
                                                                                                     // find
                                                                                                     // better
                                                                                                     // way
                                                                                                     // to
                                                                                                     // handle
                                                                                                     // methods
                                                                                                     // which
                                                                                                     // should
                                                                                                     // not
                                                                                                     // set
                                                                                                     // as
                                                                                                     // "already comuted"
                                                                                                     // because
                                                                                                     // they
                                                                                                     // are
                                                                                                     // in
                                                                                                     // the
                                                                                                     // path
                                                                                                     // of
                                                                                                     // a
                                                                                                     // checkPermission
                                                                                                     // method
        } else {
            computedMethods.add(m);
        }
        stack.pop();

    }



    /**
     * For each computed entry point, print the corresponding permission list
     * 
     * @param entryPoints2PermissionsMap2
     */
    protected void printEntryPointPermissionsToFile(String targetFile,
            Map<EntryPointKey, Set<String>> entryPoints2PermissionsMap2) {
        try {
            Output epo = new Output(targetFile);
            epo.open();
            epo.println();
            epo.println("entry points to permission set:");
            epo.println("------------------------------");
            for (EntryPointKey epk : entryPoints2PermissionsMap.keySet()) {
                epo.print(epk + " : ");
                for (String p : entryPoints2PermissionsMap.get(epk)) {
                    epo.print(p + " ");
                }
                epo.println();
            }
            epo.println("------------------------------");
            epo.println();
            epo.flush();
            epo.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("io error... exiting");
            System.exit(-1);
        }
    }

    /**
     * Dump entry point methods with 0 permissions to a file
     * 
     * @param targetFile
     * @param ep2p
     */
    protected static void dumpNoPermEntryPoints(String targetFile,
            Map<EntryPointKey, Set<String>> ep2p) {
        try {
            PrintWriter o = new PrintWriter(targetFile);
            for (EntryPointKey ep : ep2p.keySet()) {
                if (ep2p.get(ep).size() == 0) {
                    o.println(ep.toString());
                }
            }
            o.flush();
            o.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("io error... exiting");
            System.exit(-1);
        }
    }

    /**
     * Dump methods with 0 permissions to a file
     * 
     * @param targetFile
     * @param m2p
     */
    protected static void dumpNoPermMethods(String targetFile, Map<SootMethod, Set<String>> m2p) {
        try {
            PrintWriter o = new PrintWriter(targetFile);
            for (SootMethod sm : m2p.keySet()) {
                if (m2p.get(sm).size() == 0) {
                    o.println(sm.toString());
                }
            }
            o.flush();
            o.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("io error... exiting");
            System.exit(-1);
        }
    }

    public static void usage() {
        System.out
                .println("Usage: java CHAFindPermissionCheck -keep <path/to/config/file> -skip <path/to/config/file> <soot-args>");
    }

    private static void myDebug(String message) {
        logger.info("start debug... " + message);
        {
            CallGraph cg = Scene.v().getCallGraph();
            Iterator<Edge> edges = cg
                    .edgesOutOf(Scene
                            .v()
                            .getMethod(
                                    "<android.accounts.IAccountAuthenticatorResponse$Stub$Proxy: void onError(int,java.lang.String)>"));
            while (edges.hasNext()) {
                Edge e = edges.next();
                logger.info("edge: " + e);
            }
        }
        logger.info("end debug... " + message);
    }


    // private void printEdgesDebug (CallGraph cg) {
    // SootMethod sm =
    // Scene.v().getMethod("<com.android.server.WallpaperManagerService: void checkPermission(java.lang.String)>");
    // logger.info(sm.getActiveBody());
    // Iterator<Edge> it = cg.edgesOutOf(sm);
    //
    // logger.info("");
    // logger.info("debug for "+ sm);
    // boolean hasPermCheckMethod = false;
    // while (it.hasNext()) {
    // Edge e = it.next();
    // System.out.println("edge" + e.tgt());
    // if (e.tgt().toString().contains(" check"))
    // hasPermCheckMethod = true;
    // }
    // if (!hasPermCheckMethod)
    // logger.info("WARNING: has not perm check method!!!!");
    // logger.info("");
    // }

    /**
     * 
     * @param args
     */
    public static void main(String[] args) {
System.out.println("toto");
        List<String> argsRaw = new ArrayList<String>(Arrays.asList(args));
        List<String> argsList = new ArrayList<String>();
        // parse non-Soot parameters
        int SKIP_ARGS = 6;
        try {
            String keepFileFlag = argsRaw.get(0);
            String keepFilePath = argsRaw.get(1);
            String skipFileFlag = argsRaw.get(2);
            String skipFilePath = argsRaw.get(3);
            String cfgFileFlag = argsRaw.get(4);
            String cfgFilePath = argsRaw.get(5);

            if (!keepFileFlag.equals("-keep"))
                throw new Exception("Expected -keep flag! not '" + keepFileFlag + "'");
            File f = new File(keepFilePath);
            if (!f.exists()) {
                System.out.println("[W] Keep file does not exists! (" + f + ")");
                FilterCHA.v().addDefaultClassesToKeep();
            } else {
                FilterCHA.v().addClassesToAnalyze(f);
            }

            if (!skipFileFlag.equals("-skip"))
                throw new Exception("Expected -skip flag! not '" + skipFileFlag + "'");
            File fskip = new File(skipFilePath);
            if (!fskip.exists()) {
                System.out.println("[W] Skip file does not exists! (" + fskip + ")");
                FilterCHA.v().addDefaultClassesToSkip();
            } else {
                FilterCHA.v().addClassesToSkip(fskip);
            }

            if (!cfgFileFlag.equals("-cfg"))
                throw new Exception("Expected -cfg flag! not '" + cfgFileFlag + "'");
            ConfigCHA.v().loadConfig(cfgFilePath);

            // log4j debug level
            // LoggerFactory.getLogger("lu.uni.fpc").setLevel(ConfigCHA.v().logLevel());
            // Logger.getRootLogger().setLevel(ConfigCHA.v().logLevel());

        } catch (Throwable e) {
            System.err.println("error: " + e.getMessage());
            usage();
            System.exit(-1);
        }

        // continue with Soot parameters
        for (int i = SKIP_ARGS; i < argsRaw.size(); i++) {
            argsList.add(argsRaw.get(i));
        }
        argsList.addAll(Arrays.asList(new String[] { "-w" }));

        PackManager.v().getPack("wjtp").add(new Transform("wjtp.myTrans", new SceneTransformer() {
            @Override
            protected void internalTransform(String phaseName, Map options) {

                // initLogger();
                CHAFindPermissionChecks cgpr = new CHAFindPermissionChecks();
                System.out.println("start of wjtp transformer...");
                System.out.flush();
                logger.debug("[BEFORE] entry point: ");
                for (SootMethod s : Scene.v().getEntryPoints()) {
                    logger.debug(" entry point: " + s);
                }

                List<SootMethod> entryPointsList = new ArrayList<SootMethod>();
                entryPointsList.add(Scene.v().getMainMethod());
                Scene.v().setEntryPoints(entryPointsList);

                logger.debug("[AFTER] entry point: ");
                for (SootMethod s : Scene.v().getEntryPoints()) {
                    logger.debug(" entry point: " + s);
                }

                List<SootMethod> targetMethods = new ArrayList<SootMethod>();
                targetMethods.addAll(Scene.v().getEntryPoints());

                /*
                 * logger.info(""); logger.info("*******************");
                 * logger.info("[I] Dumb call graph");
                 * logger.info("*******************");
                 * cgpr.generateDumpCallGraph(options);
                 */

                cha_start = System.currentTimeMillis();
                logger.info("");
                logger.info("*******************");
                logger.info("[I] Call graph with CHA");
                logger.info("*******************");
                cgpr.generateCHACallGraph(options);
                long cha_end = System.currentTimeMillis() - cha_start;
                logger.info("CHA has run for " + cha_end + "");

                System.out.println("cha call graph generation end.");

                if (!ConfigCHA.v().isForceNaiveCallGraphSearch()) {

                    logger.info("start CG...");
                    long cg_start = System.currentTimeMillis();
                    cg = Scene.v().getCallGraph();
                    // cgpr.printEdgesDebug(cg);
                    for (SootMethod ep : targetMethods) {
                        Stack<SootMethod> stack = new Stack<SootMethod>();
                        cgpr.normalRecursiveCall(stack, cg, ep, null);
                    }
                    long cg_end = System.currentTimeMillis() - cg_start;
                    logger.info("CG has run for " + cg_end + "");
                    logger.info("Call graph generation ended.");

                    // cgpr.printEdgesDebug(cg);

                    // propagatePermissions(methodToPermissionSet);



                    // step 1/3: find Strongly Connected Components (SCC)
                    logger.info("Tarjan start...");
                    long tarjan_start = System.currentTimeMillis();
                    Tarjan t = new Tarjan();
                    List<List<SootMethod>> scc = t.executeTarjan(permCg);
                    long tarjan_end = System.currentTimeMillis() - tarjan_start;
                    logger.info("Tarjan has run for " + tarjan_end + "");

                    // logger.info("--- Strongly connected components:");
                    // int i=0;
                    // for (List<SootMethod> l: scc) {
                    // if (l.size() <= 1)
                    // continue;
                    // logger.info("scc "+ i++);
                    // for (SootMethod sm: l) {
                    // logger.info ("   "+ sm);
                    // }
                    // }
                    // logger.info("---");

                    // step 2/3: replace each SCC by a single node
                    for (SootMethod w : wMethods)
                        System.out.println("w: " + w);
                    for (SootMethod ep : epMethods)
                        System.out.println("ep: " + ep);
                    long removeSCC_start = System.currentTimeMillis();
                    t.removeSCC(permCg, scc, wMethods, epMethods, methodToPermissionSet);
                    long removeSCC_end = System.currentTimeMillis() - removeSCC_start;
                    logger.info("RemoveSCC has run for " + removeSCC_end + "");

                    // step 3/3: backward propagate permission checks
                    // At this point permCg is a acyclic directed graph
                    // so the propagation of permission information starts
                    // from the leaves (permission check methods) and
                    // goes up to the main method (in MainClass)
                    long propagate_start = System.currentTimeMillis();
                    ForwardSearch.searchPermissions(permCg, methodToPermissionSet);
                    long propagate_end = System.currentTimeMillis() - propagate_start;
                    logger.info("Propagate permissions has run for " + propagate_end + "");

                    logger.info("clearing entry point to permissions");
                    logger.info("# entry points for phase 1 (find permissions): " + epkeys.size());
                    for (EntryPointKey k : epkeys) {
                        System.out.println("epk1: " + k);
                    }
                    entryPoints2PermissionsMap.clear();
                    entryPoints2PermissionsMap = ForwardSearch.getEntryPointsToPermissionSet();
                    for (EntryPointKey k : entryPoints2PermissionsMap.keySet()) {
                        System.out.println("epk2: " + k);
                    }

                } else { // Naive call graph search

                    System.out.println("start naive call graph search...");
                    long cg_naive_start = System.currentTimeMillis();
                    NaiveSearch ns = new NaiveSearch(methodToPermissionSet,
                            entryPoints2PermissionsMap, cg_naive_start);
                    logger.info("start CG...");

                    cg = Scene.v().getCallGraph();
                    for (SootMethod ep : targetMethods) {
                        Stack<SootMethod> stack = new Stack<SootMethod>();
                        ns.normalRecursiveCall(stack, cg, ep);
                    }
                    ns.clean();
                    long cg_naive_end = System.currentTimeMillis() - cg_naive_start;
                    logger.info("CG naive has run for " + cg_naive_end + "");
                    logger.info("Call graph generation ended.");
                    System.out.println("end of naive call graph search.");

                }

                // print stats
                Statistics.printMethodsAndTheirPermissions(methodToPermissionSet);
                Statistics.printMethodsPerPermissionSetSize(methodToPermissionSet);
                Statistics.entryPointsStats(entryPoints2PermissionsMap);
                Statistics.permissionCheckMethodsStats();

                // print all entry point methods and their permission set
                cgpr.printEntryPointPermissionsToFile(Options.v().output_dir() + File.separatorChar
                        + "epo.cha.gz", entryPoints2PermissionsMap);
                // print methods which do not require any permissions in a file
                cgpr.dumpNoPermMethods(Options.v().output_dir() + "/noPermMethods.txt",
                        methodToPermissionSet);
                // print entry point methods which do not require any permission
                // in a file
                cgpr.dumpNoPermEntryPoints(Options.v().output_dir() + "/noPermEntryPoints.txt",
                        entryPoints2PermissionsMap);

                if (ConfigCHA.v().doOuputGraph()) {
                    System.out.println("Writing graphs to file...");
                    OutputGraph og1 = new OutputGraph(cg);
                    OutputGraph og2 = new OutputGraph(permCg);
                    try {
                        og1.generateCSV("initialGraph.csv");
                        og2.generateCSV("simplifiedGraph.csv");
                    } catch (IOException e) {
                        throw new RuntimeException("error: cannot wirte graphs to file! " + e);
                    }
                }


                throw new RuntimeException("The End.");
            }

        }));

        // enable whole program mode
        argsList.add("-w");
        // argsList.add("-ws");
        // argsList.add("-full-resolver");

        //
        // argsList.add("-p");
        // argsList.add("jb");
        // argsList.add("enabled:false");

        //
        argsList.add("-p");
        argsList.add("wjop");
        argsList.add("enabled:true");

        // call graph
        argsList.add("-p");
        argsList.add("cg");
        argsList.add("enabled:true");
        //
        argsList.add("-p");
        argsList.add("cg");
        argsList.add("verbose:false");
        //
        argsList.add("-p");
        argsList.add("cg.cha");
        argsList.add("enabled:true");
        //
        argsList.add("-p");
        argsList.add("cg");
        argsList.add("all-reachable:false");

        //
        argsList.add("-p");
        argsList.add("cg");
        argsList.add("safe-forname:false");

        // Spark
        argsList.add("-p");
        argsList.add("cg.spark");
        argsList.add("enabled:false");

        // Paddle
        argsList.add("-p");
        argsList.add("cg.paddle");
        argsList.add("enabled:false");

        args = argsList.toArray(new String[0]);

        try {
            soot.Main.main(args);
        } catch (Throwable t) {
            t.printStackTrace();
            System.err.println("throwable when running Soot! " + t);
        }
    }




}
