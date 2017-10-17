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

package lu.uni.fpc;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

import junit.framework.TestCase;
import lu.uni.fpc.cha.CHAFindPermissionChecks;


public class CHACycleTest extends TestCase {
  
//  SOOT_CMD="${CG_METHOD} \
//    -soot-class-path $SOOT_CLASSPATH \
//    -d $SOOT_OUT_DIR \
//    -f n \
//    -w \
//    -i android. \
//    -i com.android. \
//    -allow-phantom-refs \
//    -full-resolver \
//    -main-class MainClass \
//    $PROCESS_THIS
//   "
//  -soot-class-path /tmp/:/home/alex/prog/soot_findPermissionChecks/../entryPointWrapper-testLib/lib/EntryPointWrapper-testLib.jar:/tmp/:/home/alex/experiments/permissionMap/system-platform/android-4.0.1_r1.jar: 
//  -d -f n -w -i android. -i com.android. -allow-phantom-refs -full-resolver -main-class MainClass -process-dir /tmp/

  
  List<String> sootClassPath = new ArrayList<String>();
  List<String> sootOutputDir = new ArrayList<String>();
  List<String> sootOptions = new ArrayList<String>();
  List<String> sootTarget = new ArrayList<String>();
  // 
  String targetDir = "./test-data/CHAcycle/";
  String bytecodeDir = targetDir +"/bytecode/";
  String epDir = targetDir +"/ep/";
  String outputDir = "./testOut/";
  
  String keepFilePath = epDir +"/keep.cfg";
  String skipFilePath = epDir +"/skip.cfg";
  String cfgFilePath = epDir +"/fpc.cfg";
  
  protected void setUp() throws Exception {
    
    cleanOutputDir();
    
    sootClassPath.add("-soot-class-path");
    sootClassPath.add(bytecodeDir +":"+ epDir +":"+ "./libs/rt.jar");
    
    sootTarget.add("-process-dir");
    sootTarget.add(epDir);
    
    sootOptions.add("-d");
    sootOptions.add(outputDir);
    sootOptions.add("-f");
    sootOptions.add("n");
    sootOptions.add("-w");   
    sootOptions.add("-allow-phantom-refs");
    sootOptions.add("-full-resolver");
    sootOptions.add("-main-class");
    sootOptions.add("MainClass");
 
  }
  
  public void testFindPermissionCheckCHA() throws FileNotFoundException, IOException { 
    List<String>args = new ArrayList<String>();
    args.add("-keep");
    args.add(keepFilePath);
    args.add("-skip");
    args.add(skipFilePath);
    args.add("-cfg");
    args.add(cfgFilePath);
    args.addAll(sootClassPath);
    args.addAll(sootOutputDir);
    args.addAll(sootOptions);
    args.addAll(sootTarget);
    String argsA[] = new String[1];
    argsA = args.toArray(argsA);
    System.out.println("args size: "+ argsA.length);
    for (int i=0; i<argsA.length; i++)
      System.out.println("arg"+ i +": "+ argsA[i]);
    CHAFindPermissionChecks.main(argsA);
    
    File outdir = new File (outputDir);
    File[] list = outdir.listFiles();
    assertEquals(3, list.length);
    File target = null;
    for (File f: list) {
      if (f.getName().matches("^epo.*")) { //^[0-9].*")) {
        target = f;
        break;
      }
    }
    assertNotNull(target);
    System.out.println("target: "+ target.getName());
    InputStreamReader in = new InputStreamReader(new GZIPInputStream(new FileInputStream(target)));
    BufferedReader br = new BufferedReader(in);
    
    int pos = 0;  
    String s;
    while ((s = br.readLine()) != null) {
      if (s.equals("entry points to permission set:")) 
        pos++;
    }
    System.out.println("pos: "+ pos);
    int i = 0;
    List<String> entryPoints2PermissionList = new ArrayList<String>();
    br.close();
    
    in = new InputStreamReader(new GZIPInputStream(new FileInputStream(target)));
    br = new BufferedReader(in);
    while ((s = br.readLine()) != null) {
      if (s.equals("entry points to permission set:")) {
        i++;
        if (i == pos) {  
          s = br.readLine(); // skip first line starting with "-"
          while (true) {
            s = br.readLine();
            if (s.startsWith("-"))
              break;
            System.out.println("add line: "+ s);
            entryPoints2PermissionList.add(s);
          }
          break;
        }
      }
    }
    int checks = 0;
    for (String m: entryPoints2PermissionList) {
      System.out.println("m: "+ m);
      if (m.startsWith("Wrapper_test_Ep2 -> <android.test.Ep1: void x()> :")) {
        assertEquals(true, m.endsWith(": \"android.permission.P2\" \"android.permission.P1\""));
        checks++;
      }
      if (m.startsWith("Wrapper_test_Ep1 -> <android.test.Ep1: void a()> :")) {
        assertEquals(true, m.endsWith("> : \"android.permission.P2\" \"android.permission.P1\""));
        checks++;
      }
    }
    assertEquals(2, checks);
    
    
  }
  
  public void tearDown() {
    cleanOutputDir();
  }
  
  public void cleanOutputDir() {
    // clear output dir
    File outdir = new File (outputDir);
    if (!outdir.exists())
      outdir.mkdir();
    for (File f: outdir.listFiles()) {
      if (f.isDirectory()) {
        System.out.println("warning: not removing directory "+ f.getAbsolutePath());
        continue;
      }
      f.delete();
    }
  }
}
