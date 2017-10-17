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
import lu.uni.fpc.spark.SparkTimeOut;


public class TimeOutTest extends SootTestCase {

  
  protected void setUp() throws Exception {
    setUpSoot("Spark", "./test-data/timeout/", SootTestCase.defaultOutputDir);
    //SparkTimeOut.reset();
    // time out parameters are define in the config file
    //SparkTimeOut.setTimeOutDepth(2);
    //SparkTimeOut.setTimeOutTime(1);
  }
  
  public void testSparkTimeOut() throws FileNotFoundException, IOException { 
    runSoot();
    
    File outdir = new File (SootTestCase.defaultOutputDir);
    File[] list = outdir.listFiles();
    assertEquals(3, list.length);
    File target = null;
    for (File f: list) {
      if (f.getName().matches("^too.spark.gz$")) { //^[0-9].*")) {
        target = f;
        break;
      }
    }
    assertNotNull(target);
    
    // get last print of "timed out entry points"
    System.out.println("target: "+ target.getName());
    InputStreamReader in = new InputStreamReader(new GZIPInputStream(new FileInputStream(target)));
    BufferedReader br = new BufferedReader(in);    
    int pos = 0;  
    String s;
    while ((s = br.readLine()) != null) { 
      if (s.equals("timed out entry points:")) 
        pos++;
    }
    System.out.println("pos: "+ pos);
    int i = 0;
    List<String> entryPoints2PermissionList = new ArrayList<String>();
    br.close();
    
    // check that timedOutMethod() is in there (has timed out)
    in = new InputStreamReader(new GZIPInputStream(new FileInputStream(target)));
    br = new BufferedReader(in);
    String targetLine = null;
    int timedOutMethods = 0;
    while ((s = br.readLine()) != null) {
      if (s.equals("timed out entry points:")) {
        i++;
        if (i == pos) {  
          s = br.readLine(); // skip first line starting with "-"
          while (true) {
            s = br.readLine();
            if (s.startsWith("-"))
              break;
            targetLine = s;
            timedOutMethods++;
          }
          break;
        }
      }
    }
    assertEquals(1, timedOutMethods);
    System.out.println("targetLine : "+ targetLine);
    assertEquals(true, targetLine.startsWith("lu.uni.fpc.test.WrapperEntryPoint1 -> <lu.uni.fpc.test.EntryPoint1: void timeOutMethod()>"));
    br.close();
    
  }
  
  public void tearDown() {
    SparkTimeOut.v().reset();
    //cleanSoot();
  }
  
}
