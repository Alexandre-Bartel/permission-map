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


public class ConcreteClassTest extends SootTestCase {
 
  List<String> mainClassOracle = new ArrayList<String>();
  List<String> wrapperOracle = new ArrayList<String>();
  List<String> genOracle = new ArrayList<String>();

  protected void initOracles() {
	  mainClassOracle.add("public class MainClass{");
	  mainClassOracle.add("  public static void main (String[] args) {");
	  mainClassOracle.add("    Wrapper_lu_uni_epw_test_ConcreteClass.test_lu_uni_epw_test_ConcreteClass();");
	  mainClassOracle.add("  }");
	  mainClassOracle.add("}");

		wrapperOracle.add("public class Wrapper_lu_uni_epw_test_ConcreteClass{");
		wrapperOracle.add("  public static void test_lu_uni_epw_test_ConcreteClass() {");
		wrapperOracle.add("    lu.uni.epw.test.ConcreteClass o_luuniepwtestConcreteClass = null;");
		wrapperOracle.add("    o_luuniepwtestConcreteClass = GenerationGG.get_luuniepwtestConcreteClass();");
		wrapperOracle.add("    try {");
		wrapperOracle.add("    o_luuniepwtestConcreteClass.returnZero();");
		wrapperOracle.add("    } catch (Throwable e) {}");
		wrapperOracle.add("    try {");
		wrapperOracle.add("    o_luuniepwtestConcreteClass.add((int)0, (int)0);");
		wrapperOracle.add("    } catch (Throwable e) {}");
		wrapperOracle.add("  }");
		wrapperOracle.add("}");

		genOracle.add("public class GenerationGG {");
		genOracle.add("  public static lu.uni.epw.test.ConcreteClass get_luuniepwtestConcreteClass() {");
		genOracle.add("    lu.uni.epw.test.ConcreteClass o_o = (lu.uni.epw.test.ConcreteClass) null;");
		genOracle.add("    o_o = new lu.uni.epw.test.ConcreteClass((int)0,(int)0);");
		genOracle.add("    return o_o;");
		genOracle.add("  }");
		genOracle.add("}");
 
  }
 
  protected void setUp() throws Exception {
    initOracles();
    String targetDir = "./test-data/concreteClass/";
    setUpSoot(targetDir, defaultOutputDir);
  }
  
  public void testConcreteClassk() throws FileNotFoundException, IOException { 
   
    runSoot();
    
    //
    File outdir = new File (defaultOutputDir);
    File[] list = outdir.listFiles();
    assertEquals(4, list.length); // GenerationGG.java, Wrapper....java, MainClass.java, stats.txt

    //
    testJavaFile(mainClassOracle, outdir.getPath() +"/MainClass.java"); 
    testJavaFile(wrapperOracle, outdir.getPath() +"/Wrapper_lu_uni_epw_test_ConcreteClass.java"); 
    testJavaFile(genOracle, outdir.getPath() +"/GenerationGG.java"); 
  }
 
  public void tearDown() {
    tearDownSoot();
  }
  
}
