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

package lu.uni.fpc.cha;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
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
import lu.uni.fpc.string.CheckForPermission;
import soot.Scene;
import soot.SootMethod;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.options.Options;


/**
 * Rebuild version of the naive search in CHA call graph 
 * for experiment cha-naiveNaive.
 * The naive search does not record already computed methods's
 * permission sets.
 * 
 * @author alex
 *
 */
public class NaiveSearch {
  
  static SootMethod currentWrapper = null; // store method at depth 1 (class
  // wrapper)
  static SootMethod currentEntryPoint = null; // store method at depth 2
    // (method entry point)
  static EntryPointKey epk = null; // key for the current entry point (wrapper
  // + method)
  
  // record the list of permissions for every entry point. An entry point is
  // the couple (wrapper, method entry point)
  Map<EntryPointKey, Set<String>> entryPoints2PermissionsMap = null;//new HashMap<EntryPointKey, Set<String>>();
  
  Map<SootMethod, Set<String>> methodToPermissionSet = null;//new HashMap<SootMethod, Set<String>>();
  
  static Map<SootMethod, Set<SootMethod>> methodCalled = new HashMap<SootMethod, Set<SootMethod>>();
  
  // Set, containing all methods for which the list of permissions has already
  // been computed.
  static Set<SootMethod> computedMethods = new HashSet<SootMethod>();

// For cycles
static Cycles cycles = new Cycles();
  
  static Set<EntryPointKey> epkeys = new HashSet<EntryPointKey>();
  static Set<SootMethod> epMethods = new HashSet<SootMethod>();
  static Set<SootMethod> wMethods = new HashSet<SootMethod>();
  long cha_naive_start = 0;
  static PrintWriter pw = null;
  public NaiveSearch(Map<SootMethod, Set<String>> methodToPermissionSet2, Map<EntryPointKey, Set<String>> entryPoints2PermissionsMap2, long cha_naive_start) {
    this.methodToPermissionSet = methodToPermissionSet2;
    this.entryPoints2PermissionsMap = entryPoints2PermissionsMap2;
    this.cha_naive_start = cha_naive_start;
    try {
      pw = new PrintWriter(new FileWriter(Options.v().output_dir() +File.separatorChar +"epo.cha.naive.txt"));
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }
  
  public void clean() {
    pw.close();
  }
  
  
  private static final Logger logger = LoggerFactory.getLogger(NaiveSearch.class);
//  private static void initLogger() {
//    // default logger prints on console
//    BasicConfigurator.configure();
//    // log stuff >= INFO
//    logger.setLevel(Level.INFO);
//  }
  
  
  private void addPermissionsToCurrentMethod(
      Map<SootMethod, Set<String>> methodToPermissionSet,
      HashSet<String> permissions, SootMethod sm) {
   if (!CheckForPermission.isCheckPermissionMethod(sm)) {
     if (methodToPermissionSet.containsKey(sm)) {
       methodToPermissionSet.get(sm).addAll(permissions);
     } else {
       methodToPermissionSet.put(sm, new HashSet<String>(permissions));
     }
   }
    
  }
  
  private void printMethod(int depth, String message) {
    String s = (System.currentTimeMillis() - cha_naive_start + " | ");
    for (int i = 0; i < depth; i++) {
      s += (" ");
    }
    s += (depth + "> " + message);
    if (ConfigCHA.v().printLiveCallGraph())
      System.out.println(s);
  }
  
  /**
   * 
   * @param stack
   * @param cg
   * @param ep
   */
  public void normalRecursiveCall(Stack<SootMethod> stack, CallGraph cg,
      SootMethod m) {
    int depth = stack.size();
    // if (depth>9) {return;}
    String mString = m.toString(); // get method signature (ex:

    if (FilterCHA.v().mustSkip(stack, m)) {
      return;
    }
    
    if (depth == 1) {
      currentWrapper = m;
      wMethods.add(currentWrapper);
      pw.write(currentWrapper +"\n");
      pw.flush();
      System.out.println("new class: "+ currentWrapper);
    }
    
    if (depth == 2) {
      currentEntryPoint = m;
      if (currentEntryPoint.toString().contains("GenerationGG") ||
        //currentEntryPoint.toString().contains("init>") ||
        currentEntryPoint.toString().contains("ServicesInit:")) {
          return;
        } else {
      EntryPointKey epk = new EntryPointKey(currentWrapper, currentEntryPoint);
      epkeys.add(epk);
      epMethods.add(currentEntryPoint);
      System.out.println("ep: "+ epk);
        }
    }
    
    // To avoid cycles, we do not analyze a method twice
    if (stack.contains(m)) //cycles.addCycleIfNecessary(stack, m))
      return;

    // If the method has already been computed, add its set of permissions
    // to all the methods in the stack.
//    if (depth > 2 && computedMethods.contains(m)) {
//      //printMethod(depth, "ALREADY COMPUTED" + mString);
//      return;
//    }

    // Print current method
    if (depth < 600) {
      printMethod(depth, mString);
    }

    // The current method is analyzed.
    stack.push(m);
    List<Edge> edgesFromCurrentMethod = null;

    if (edgesFromCurrentMethod == null) {
      edgesFromCurrentMethod = new ArrayList<Edge>();
      Iterator<Edge> itEdge = cg.edgesOutOf(Scene.v().getMethod(
          m.toString()));
      while (itEdge.hasNext()) {
        edgesFromCurrentMethod.add(itEdge.next());
      }
    }

    // Analyze method calls from the current method's body
    // Permission(s) checked for this method (if any)

    for (Edge e : edgesFromCurrentMethod) {
      HashSet<String> methodPermissionSet = new HashSet<String>();
      boolean theDestNodeIsAPermCheckMethod = CheckForPermission
          .checkForPermission(e, methodPermissionSet, stack);
      if (theDestNodeIsAPermCheckMethod) {
        // System.err.println(m.toString());
        CheckForPermission.printLogValue();
        int permSetSize = methodPermissionSet.size();
        System.err.println("perm set size: " + permSetSize);
        if (permSetSize == 0) {
          System.err.println("$$$$$$$$$$$$$ we could not find a perm string in "+ m.toString());
          
          // we add the UNKNOWN perm
          // solution 1
         

          // solution 2
          // we force contonuing exploring the graph
          // theDestNodeIsAPermCheckMethod = false;
        }

        addPermissionsToCurrentMethod(methodToPermissionSet, methodPermissionSet, m);

        // if (depth >= 2)
        // entryPoints2PermissionsMap.get(epk).addAll(methodPermissionSet);
        //printMethod(depth + 1, "> " + e.tgt()
        //    + " has permissions: stopping here"); // depth + 1 since
        // m was pushed
        // on the stack
        for (String p : methodPermissionSet) {
          logger.info("[FFF] " + p);
          Set<String> pMap = entryPoints2PermissionsMap.get(currentEntryPoint);
          if (pMap == null) {
            Set<String> newpMap = new HashSet<String>();
            newpMap.add(p);
            entryPoints2PermissionsMap.put(epk, newpMap);
          } else {
            pMap.add(p);
          }
        }
        // logger.info(ep);
        // logger.info("    p> continue;");
        continue;
      }
      
      normalRecursiveCall(stack, cg, e.tgt());
    }

    //computedMethods.add(m); 
    stack.pop();

  }
  

}
