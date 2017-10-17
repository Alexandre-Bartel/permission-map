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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lu.uni.fpc.EntryPointKey;
import lu.uni.fpc.Output;



import soot.SootMethod;
import soot.options.Options;

public class SparkTimeOut {
  
  private static SparkTimeOut instance = null;
  
  private static Logger logger = LoggerFactory.getLogger(lu.uni.fpc.spark.SparkFindPermissionChecks.class);
  
  private int TIMEOUT_DEPTH = 10;
  private int TIMEOUT_SECONDS = 60;
  
  private Output too = null;
  
  private HashMap<SootMethod, Set<SootMethod>> timedOutMethods = new HashMap<SootMethod, Set<SootMethod>>();
  
  private SparkTimeOut() {};
  
  public static SparkTimeOut v() {
    if (instance == null)
      instance = new SparkTimeOut();
    return instance;
  }
  
  public void reset() {
    System.out.println("reset spark timeout!");
    setTimeOutDepth(10);
    setTimeOutTime(60);
  }
  
  public void setTimeOutTime(int t) {
    if (t < 0)
      throw new RuntimeException("error: timeout time must be a positive integer! "+ t);
    TIMEOUT_SECONDS = t;
    System.out.println("[spark] time out time set to '"+ t +"'");
  }
  
  public void setTimeOutDepth(int d) {
    if (d < 0)
      throw new RuntimeException("error: timeout depth must be a positive integer! "+ d);
    TIMEOUT_DEPTH = d;
    System.out.println("[spark] time out depth set to '"+ d +"'");
  }
  
  public int depth() { return TIMEOUT_DEPTH; }
  public int time() { return TIMEOUT_SECONDS; }
  
  public void addTimedOutMethod(SootMethod wrapper, SootMethod sm) {
    if (timedOutMethods.containsKey(wrapper)) {
      timedOutMethods.get(wrapper).add(sm);
    } else {
      Set<SootMethod> set = new HashSet<SootMethod>();
      set.add(sm);
      timedOutMethods.put(wrapper, set);
    }
    
  }
  
  public boolean isTimedOutMethod(EntryPointKey k) {
    return isTimedOutMethod(k.getWrapper(), k.getEp());
  }
  
  private boolean isTimedOutMethod(SootMethod wrapper,SootMethod sm) {
    if (timedOutMethods.keySet().contains(wrapper)) {
      if (timedOutMethods.get(wrapper).contains(sm))
        return true;
    }
    return false;
  }
  
  public void printTimedOutEntryPoints() {
    try {
    too.println();
    too.println("timed out entry points:");
    too.println("------------------------------");
    for (SootMethod wrapper: timedOutMethods.keySet()) { // for each entry point
      for (SootMethod ep: timedOutMethods.get(wrapper))
        too.println(""+ wrapper.getDeclaringClass() +" -> "+ ep);
    }
    too.println("------------------------------");
    too.println();
    too.flush();
    
    printLastTimedOutEntryPoints();
    
    } catch (IOException e) {
      e.printStackTrace();
      System.err.println("io error... exiting");
      System.exit(-1);
    }
  }
  
  private void printLastTimedOutEntryPoints() throws IOException {
    PrintWriter pw = new PrintWriter(new FileWriter(Options.v().output_dir() +File.separatorChar +"too.last.txt"));
    for (SootMethod wrapper: timedOutMethods.keySet()) { // for each entry point
      for (SootMethod ep: timedOutMethods.get(wrapper))
        pw.println(""+ wrapper.getDeclaringClass() +" -> "+ ep);
    }
    pw.close();
  }

  public void initOutput() {
    too = new Output(Options.v().output_dir() +File.separatorChar +"too."+ "spark" +".gz");
    too.open();
  }

  public void closeOutput() {
    too.close();
  }

}
