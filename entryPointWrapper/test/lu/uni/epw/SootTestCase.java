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

package lu.uni.epw;
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


public abstract class SootTestCase extends TestCase {
  
// SOOT_CMD="lu.uni.epw.GenerateLibraryWrappers --soot-classpath $SOOT_CLASSPATH\
//   -d $SOOT_OUT_DIR \
//   -f n \
//  -allow-phantom-refs \
//  -v \
//  $PROCESS_THIS
// "
//     argsList.add("-p");
//     argsList.add("jb");
//     argsList.add("enabled:false");
//       
//     //
//     argsList.add("-p");
//     argsList.add("wjtp");
//     argsList.add("enabled:true");
//     
//     //
//     argsList.add("-p");
//     argsList.add("wjap");
//     argsList.add("enabled:false");
//     
//     //
//     argsList.add("-p");
//     argsList.add("wjop");
//     argsList.add("enabled:false");
// 
//     //
//     argsList.add("-p");
//     argsList.add("cg");
//     argsList.add("enabled:false");  

  public static String defaultOutputDir = "./testOut/";
  String outputDir = null;
  String targetDir = null;

  List<String> sootClassPath = new ArrayList<String>();
  List<String> sootOutputDir = new ArrayList<String>();
  List<String> sootOptions = new ArrayList<String>();
  List<String> sootTarget = new ArrayList<String>();
  
  protected void setUpSoot(String targetDir, String outputDir) throws Exception {
    this.targetDir = targetDir;
    this.outputDir = outputDir;

    // reset Soot
    soot.G.v().reset();
    Config.reset();

    File out = new File(outputDir);
    if (!out.exists()) 
      out.mkdir();
    this.outputDir = outputDir;
    cleanSoot();
    
    sootClassPath.add("-cp");
    sootClassPath.add(targetDir +":"+ "./libs/rt.jar:./libs/jce.jar");
    
    sootTarget.add("-process-dir");
    sootTarget.add(targetDir);
    
    sootOptions.add("-d");
    sootOptions.add(outputDir);
    sootOptions.add("-f");
    sootOptions.add("n");
    sootOptions.add("-w");   

    sootOptions.add("-p");   
    sootOptions.add("jb");   
    sootOptions.add("enabled:false");   

    sootOptions.add("-p");   
    sootOptions.add("wjtp");   
    sootOptions.add("enabled:true");   

    sootOptions.add("-p");   
    sootOptions.add("wjap");   
    sootOptions.add("enabled:false");   

    sootOptions.add("-p");   
    sootOptions.add("wjop");   
    sootOptions.add("enabled:false");   

    sootOptions.add("-p");   
    sootOptions.add("cg");   
    sootOptions.add("enabled:false");   

  }
  
  public void runSoot() throws FileNotFoundException, IOException { 
    List<String>args = new ArrayList<String>();
    args.add("-config");
    args.add(targetDir +"/config.cfg");
    args.add("-manager");
    args.add("none");
    args.addAll(sootClassPath);
    args.addAll(sootOutputDir);
    args.addAll(sootOptions);
    args.addAll(sootTarget);
    String argsA[] = new String[1];
    argsA = args.toArray(argsA);
    System.out.println("args size: "+ argsA.length);
    for (int i=0; i<argsA.length; i++)
      System.out.println("arg"+ i +": "+ argsA[i]);
    GenerateLibraryWrappers.main(argsA);
   
  }

  protected void testJavaFile(List<String> expected, String pathToJavaFileToTest) throws IOException{
    
    InputStreamReader in = new InputStreamReader(new FileInputStream(pathToJavaFileToTest));
    BufferedReader br = new BufferedReader(in);
   
    List<String> content = new ArrayList<String>(); 
    int pos = 0;  
    String s;
    while ((s = br.readLine()) != null) {
      if (s.matches("\\s*//.*"))  
        continue;
      if (s.matches("^\\s*\\n*$"))  
        continue;
      content.add(s);
      //System.out.println("add '"+ s+"'");
    }

    assertEquals(expected.size(), content.size());
    for (int i=0; i<content.size(); i++)
      assertEquals(expected.get(i).trim(), content.get(i).trim());
  }


  public void cleanSoot() {
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

  public void tearDownSoot() {
    cleanSoot();
  }

}
