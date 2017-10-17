// 
// (c) 2012 University of Luxembourg - Interdisciplinary Centre for 
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

package lu.uni.fpc.spark;

import java.io.File;
import java.io.FileWriter;
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
import lu.uni.fpc.Util;
import lu.uni.fpc.identity.ClearRestoreCallingIdentity;
import lu.uni.fpc.string.CheckForPermission;
import soot.PackManager;
import soot.Scene;
import soot.SceneTransformer;
import soot.SootMethod;
import soot.Transform;
import soot.jimple.spark.SparkTransformer;
import soot.jimple.toolkits.callgraph.CHATransformer;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.options.Options;


public class SparkFindPermissionChecks {
  
  private static Logger logger = LoggerFactory.getLogger(lu.uni.fpc.spark.SparkFindPermissionChecks.class);
//  private static void initLogger() {
//    // default logger prints on console
//    BasicConfigurator.configure();
//    // log stuff >= INFO
//    logger.setLevel(Level.INFO);
//  }

  public HashMap<EntryPointKey, Set<String>> entryPoints2PermissionsMap = new HashMap<EntryPointKey, Set<String>>();
  private static long spark_start = 0;
  private static Output o = null;
  private Output epo = null;
  private Output too = null;
  //static String configFilename = null;
  
  public SparkFindPermissionChecks() throws IOException {
    o = new Output(Options.v().output_dir() +File.separatorChar+ System.currentTimeMillis() +"."+ "spark" +".gz");
    o.open();
    epo = new Output(Options.v().output_dir() +File.separatorChar +"epo."+ "spark" +".gz");
    epo.open();
    
    SparkTimeOut.v().initOutput();
    logger.info("current working dir: "+ System.getProperty("user.dir"));
  }
  
  
  protected void clean() {
    o.close(); 
    epo.close();
    SparkTimeOut.v().closeOutput();
  }
  
  /**
   * 
   * @param options
   */
  public void generateSparkCallGraph(Map options) {
    HashMap opt = new HashMap(options);
    opt.put("verbose","true");
    opt.put("propagator","worklist"); //worklist
    opt.put("simple-edges-bidirectional","true");
    opt.put("on-fly-cg","true");
    opt.put("set-impl","double");
    opt.put("double-set-old","hybrid");
    opt.put("double-set-new","hybrid");
    opt.put("dump-html","false");
    opt.put("dump-pag","false");
    opt.put("string-constants","false");
    SparkTransformer.v().transform("",opt);
    //logger.info("Current point to point analysis: "+ Scene.v().getPointsToAnalysis());
  }

  protected static void generateDumpCallGraph(Map options) {
    CHATransformer.v().transform();
  }
  
  
  private void printCurrentDepthMethod(int depth, String mString) {
    o.print(System.currentTimeMillis() - spark_start+ " | ");
    // print method name
    for (int i=0; i<depth; i++) {
      o.print(" ");
    }
    o.println(depth +"> "+ mString);
  }
  
  
  private void cachePreviousBasicMethodsAndEntryPointMethod(
      Set<String> currentBasicMethods, EntryPointKey previousKey) {
    alreadyComputedBasicMethods.addAll(currentBasicMethods);
    if (previousKey == null)
      return;
    alreadyComputedEntryPointMethods.put(previousKey.getEpString(), previousKey);
  }



  Map computedMethods = new HashMap();
  public static boolean timedOut = false;
  long d10lastTime = 0;

  SootMethod currentEntryPoint = null;
  SootMethod currentWrapper = null;
  EntryPointKey previousepk = null;
  
  // Already computed methods:
  // 1) First case
  // entry point A = wrapper1 + method1
  // if entry point B = wrapper2 + method1,
  // reuse permission set from wrapper1  method1
  //
  // 2) Second case
  // methods with only primitve or string parameters
  // called "basic methods" should not be recomputed.
  Map<String, EntryPointKey> alreadyComputedEntryPointMethods = new HashMap<String, EntryPointKey>();
  Set<String> alreadyComputedBasicMethods = new HashSet<String>();
  
  Set<String> currentBasicMethods = new HashSet<String>();
  
  /**
   * 
   * @param stack
   * @param cg
   * @param ep
   */
  public void normalRecursiveCall( Stack<SootMethod> stack, CallGraph cg, SootMethod ep) {
    int depth = stack.size();
    SootMethod m = ep;
    String mString = m.toString();
    String mName = mString.replaceAll("<", "").replaceAll(">", "");//.split(":")[0];

    //FilterSpark.v().updateFromFiles();
    
    if (depth > 6) {
    	System.out.println("depth > 6, skipping!");
    	return;
    }
   
//    if (ConfigSpark.v().printLiveCallGraph()) // debug only, should remove
//        printCurrentDepthMethod(depth, mString);
    
    // depth 0 = root
    // depth 1 = test-class
    // depth 2 = entry point method
    if (depth == 1) {
      // update current wrapper
      currentWrapper = m;
      // print already computed entry point's permission list
      printEntryPointPermissions();
      SparkTimeOut.v().printTimedOutEntryPoints();
      logger.info(System.currentTimeMillis() - spark_start +" | <1> "+ m);
      System.out.println("stats: "+ SStats.v().toString());
    }
    if (depth == 2) {
      
      cachePreviousBasicMethodsAndEntryPointMethod(currentBasicMethods, previousepk);
      
      // clear basic methods
      currentBasicMethods = new HashSet<String>();
   
      // add current entry point
      currentEntryPoint = m;
      
      EntryPointKey epk = new EntryPointKey(currentWrapper, currentEntryPoint);
      

      if (alreadyComputedEntryPointMethods.containsKey(currentEntryPoint.toString())) {
        System.out.println("entry point already computed: "+ epk);
        EntryPointKey k = alreadyComputedEntryPointMethods.get(currentEntryPoint.toString());
        if (k == null || !(k instanceof EntryPointKey))
          throw new RuntimeException("error: invalid entry point key: "+ k);
        Set<String> pset = entryPoints2PermissionsMap.get(k);
        if (pset == null || !(pset instanceof Set))
          throw new RuntimeException("error: invalid pset "+ pset);
        if (SparkTimeOut.v().isTimedOutMethod(k)) {
          SparkTimeOut.v().addTimedOutMethod(currentWrapper, currentEntryPoint);
          SStats.v().addTimedOutEp();
        } else {
          entryPoints2PermissionsMap.put(epk, pset);
        }
        SStats.v().addAlreadyComputedEp();
        return;
      }
      
      SStats.v().addEntryPoint();
      o.println(SStats.v().toString());
      System.out.println("sstats: "+ SStats.v().toString());
      if (!entryPoints2PermissionsMap.containsKey(epk)) {
        entryPoints2PermissionsMap.put(epk, new HashSet<String>());
      }
      long t = System.currentTimeMillis() - spark_start;
      logger.info(t +" ("+ Util.time2Str(t) +") |  <2> "+ m);
      
      previousepk = epk;
    }
    
    if (FilterSpark.v().mustSkip(stack, m)) {
        SStats.v().addMustSkipMethod();
        return;
    }
    
    ////
    // handle timeouts 
    if (depth <= SparkTimeOut.v().depth()) { // reset timeout
      d10lastTime = System.currentTimeMillis(); 
      SStats.v().addTimeoutReset();
      timedOut = false;
    } else { // check if timed out
      if (timedOut) {
        SStats.v().addTimeout();
        o.println("timedout! "+ currentWrapper +" "+ currentEntryPoint);
        return;
      }
      if (d10lastTime != 0) {
        long diffInSeconds = (System.currentTimeMillis() - d10lastTime) / 1000;
        if (diffInSeconds > SparkTimeOut.v().time()) {
          SparkTimeOut.v().addTimedOutMethod(currentWrapper, currentEntryPoint);
          timedOut = true;
          SStats.v().addTimeout();
          o.println("timedout! "+ currentWrapper +" "+ currentEntryPoint);
          return;
        }
      }
    } 
    //
    ////
    
    // to avoid cycles, we do not anlyze a method twice
    if (depth > 1 && (stack.search (m) != -1)) {
      o.println("[Already Visited] depth: "+ depth +" method:'"+ m +"' wrapper: "+ currentWrapper);
      SStats.v().addAvoidCycle();
      return;
    }

    if (ConfigSpark.v().hasReadZeroPermMethods()) {
      if (FilterSpark.v().mustSkipZeroPermMethod(stack, m)) {
        SStats.v().addZeroPermMethod();
        return;
      }
    }
    
    if (alreadyComputedBasicMethods.contains(m.toString()))
      return;

    if (ConfigSpark.v().printLiveCallGraph())
      printCurrentDepthMethod(depth, mString);

    ///
    stack.push (m);
    ///
    List<Edge> edgesFromCurrentMethod = null;
    // check for clearCallingIdentity / restoreCallingIdentity
    Iterator<Edge> itEdge = cg.edgesOutOf(ep);
    while (itEdge.hasNext()) {
      Edge e = itEdge.next();
      SootMethod tgt = e.getTgt().method();
      String tgtString = tgt.toString();
      if (tgtString.contains("clearCallingIdentity(")) {
        o.print(System.currentTimeMillis() - spark_start+ " | ");
        // print method name
        for (int i=0; i<depth; i++) {
          o.print(" ");
        }
        o.println(depth +"> CONTAINS CLEAR CALLING ID "+ mString);
        edgesFromCurrentMethod = ClearRestoreCallingIdentity.removeBetweenClearAndRestoreIdentity(m, cg);
        for (Edge ee: edgesFromCurrentMethod) {
          o.println("handling those edges: "+ ee);
        }
        break;
      }
    }
    if (edgesFromCurrentMethod == null) {
      edgesFromCurrentMethod = new ArrayList<Edge>();
      itEdge = cg.edgesOutOf(ep);
      while (itEdge.hasNext()) {
        edgesFromCurrentMethod.add(itEdge.next());
      }
    }
    
    for (Edge e: edgesFromCurrentMethod) {
      
      if (ConfigSpark.v().printLiveCallGraph())
        printCurrentDepthMethod(depth, " for edge..."+ mString); // for debug only, should remove
      
      if (depth <= 1) { // to avoid checkmethod for skipped entry point 
        normalRecursiveCall( stack, cg, e.tgt());
        continue;
      }
      Set<String> methodPermissionSet = new HashSet<String>();
      boolean isCheckMethod = CheckForPermission.checkForPermission (e, methodPermissionSet, stack);
      for (String p: methodPermissionSet) {
        o.println("[F] "+ p); 
        EntryPointKey epk = new EntryPointKey(currentWrapper, currentEntryPoint);
        entryPoints2PermissionsMap.get(epk).add(p);
      } 
      if (!isCheckMethod) {
        normalRecursiveCall (stack, cg, e.tgt());
      }
    }
    ///
    stack.pop();
    ///

  }



  /**
   * For each computed entry point, print the corresponding permission list
   */
  protected void printEntryPointPermissions() {
    try {
    epo.println();
    epo.println("entry points to permission set:");
    epo.println("------------------------------");
    for (EntryPointKey epk: entryPoints2PermissionsMap.keySet()) {
      epo.print(epk +" : ");
      for (String p: entryPoints2PermissionsMap.get(epk)) {
        epo.print (p+" ");
      }
      epo.println();
    }
    epo.println("------------------------------");
    epo.println();
    epo.flush();
    
    printLastEntryPointPermissions();
    
    } catch (IOException e) {
      e.printStackTrace();
      System.err.println("io error... exiting");
      System.exit(-1);
    }
    
  }
  
  private void printLastEntryPointPermissions() throws IOException {
    PrintWriter pw = new PrintWriter(new FileWriter(Options.v().output_dir() +File.separatorChar +"epo.spark.last.txt"));
    for (EntryPointKey epk: entryPoints2PermissionsMap.keySet()) {
      pw.print(epk +" : ");
      for (String p: entryPoints2PermissionsMap.get(epk)) {
        pw.print (p+" ");
      }
      pw.println();
    }
    pw.close();
  }

  public static void usage() {
    logger.info("Usage: java SparkFindPermissionCheck -keep <path/to/config/file> -skip <path/to/config/file> <soot-args>");
  }

  /**
   * 
   * @param args
   */
  public static void main(String[] args) {

    //initLogger();
    try {
      Thread.sleep(2000);
    } catch (InterruptedException e1) {
      e1.printStackTrace();
      System.exit(-1);
    }
    
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
        throw new Exception("Expected -keep flag! not '" + keepFileFlag
            + "'");
      File f = new File(keepFilePath);
      if (!f.exists()) {
        System.out.println("[W] Keep file does not exists! (" + f + ")");
        FilterSpark.v().addDefaultClassesToKeep();
      } else {
        FilterSpark.v().addClassesToAnalyze(f);
      }
      
      if (!skipFileFlag.equals("-skip"))
        throw new Exception("Expected -skip flag! not '" + skipFileFlag
            + "'");
      File fskip = new File(skipFilePath);
      if (!fskip.exists()) {
        System.out.println("[W] Skip file does not exists! (" + fskip
            + ")");
        FilterSpark.v().addDefaultClassesToSkip();
      } else {
        FilterSpark.v().addClassesToSkip(fskip);
      }
        
      
      if (!cfgFileFlag.equals("-cfg"))
        throw new Exception("Expected -cfg flag! not '"+ cfgFileFlag +"'");
      ConfigSpark.v().loadConfig(cfgFilePath);
      
    } catch (Throwable e) {
      e.printStackTrace();
      logger.info("error: "+ e);
      usage();
      System.exit(-1);
    }
    
    
    // continue with Soot parameters
    for (int i=SKIP_ARGS; i<argsRaw.size(); i++) {
      argsList.add( argsRaw.get(i) );
    }
    argsList.addAll(Arrays.asList(new String[]{
        "-w"
    }));

    PackManager.v().getPack("wjtp").add(new Transform("wjtp.myTrans", new SceneTransformer() {
      @Override
      protected void internalTransform(String phaseName, Map options) {
        
//        FilterSpark.v().addClassesToSkip(new File(Options.v().output_dir() +"/noPermMethods.txt"));
//        FilterSpark.v().addSkipEntryPoint(Options.v().output_dir() +"/noPermEntryPoints.txt");
        
        SparkFindPermissionChecks cgpr = null;
        try {
          cgpr = new SparkFindPermissionChecks();
        } catch (IOException e) {
          e.printStackTrace();
          System.exit(-1);
        }
        
        logger.info("[BEFORE] entry point: ");
        for (SootMethod s: Scene.v().getEntryPoints()) {
          logger.info(" entry point: "+ s);
        }

        List<SootMethod> entryPointsList = new ArrayList<SootMethod>();
        entryPointsList.add(Scene.v().getMainMethod());
        Scene.v().setEntryPoints(entryPointsList);

        logger.info("[AFTER] entry point: ");
        for (SootMethod s: Scene.v().getEntryPoints()) {
          logger.info(" entry point: "+ s);
        }

        List<SootMethod> targetMethods = new ArrayList<SootMethod>();
        targetMethods.addAll(Scene.v().getEntryPoints());

        // add methods from CHA with do not check any permissions to the method filter
        // since CHA yields an over-approximation of the permission set, this is sound
        //FilterSpark.addClassesToSkip(Options.v().process_dir().get(0) +"/cha.methods");
        
/*
        logger.info("");
        logger.info("*******************");
        logger.info("[I] Dumb call graph");
        logger.info("*******************");
        cgpr.generateDumpCallGraph(options);
*/

        spark_start = System.currentTimeMillis();
        System.out.println("");
        System.out.println("*************************");
        System.out.println("[I] Call graph with spark");
        System.out.println("*************************");
        cgpr.generateSparkCallGraph(options);
        long spark_end = System.currentTimeMillis() - spark_start;
        System.out.println("Spark has run for "+ spark_end +"");

        
        long cg_start = System.currentTimeMillis();
        for (SootMethod ep: targetMethods) {
          CallGraph cg = Scene.v().getCallGraph();
          Stack<SootMethod> stack = new Stack<SootMethod>();
          cgpr.normalRecursiveCall (stack, cg, ep);
        }
        long cg_end = System.currentTimeMillis() - cg_start;
        System.out.println("CG has run for "+ cg_end +"");
        System.out.println("Call graph generation ended.");
        
        cgpr.printEntryPointPermissions();
        SparkTimeOut.v().printTimedOutEntryPoints();
        cgpr.clean();
        
        throw new RuntimeException("The End.");
      }
    }));

    // enable whole program mode
    argsList.add("-w");
    //argsList.add("-ws");
    //argsList.add("-full-resolver");

    //
    argsList.add("-p");
    argsList.add("wjop");
    argsList.add("enabled:true");
    
//    //
//    argsList.add("-p");
//    argsList.add("wjtp");
//    argsList.add("enabled:false");
    
    //
    argsList.add("-p");
    argsList.add("jtp");
    argsList.add("enabled:false");
    
    // call graph
    argsList.add("-p");
    argsList.add("cg");
    argsList.add("enabled:false");
    //
    argsList.add("-p");
    argsList.add("cg");
    argsList.add("verbose:false"); 
    //
    argsList.add("-p");
    argsList.add("cg.cha");
    argsList.add("enabled:false");
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

    soot.Main.main(args);
  }



}
