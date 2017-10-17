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


public class AbstractClassTest extends SootTestCase {
 
  List<String> mainClassOracle = new ArrayList<String>();
  List<String> concreteWrapperOracle = new ArrayList<String>();
  List<String> abstractWrapperOracle = new ArrayList<String>();
  List<String> genOracle = new ArrayList<String>();

  protected void initOracles() {
	  mainClassOracle.add("public class MainClass{");
	  mainClassOracle.add("  public static void main (String[] args) {");
// compute wrappers only for Abstract	  mainClassOracle.add("    Wrapper_lu_uni_epw_test_ConcreteClass.test_lu_uni_epw_test_ConcreteClass();");
	  mainClassOracle.add("    Wrapper_lu_uni_epw_test_AbstractClass.test_lu_uni_epw_test_AbstractClass();");
	  mainClassOracle.add("  }");
	  mainClassOracle.add("}");

	  abstractWrapperOracle.add("public class Wrapper_lu_uni_epw_test_AbstractClass{");
		abstractWrapperOracle.add("  public static void test_lu_uni_epw_test_AbstractClass() {");
		abstractWrapperOracle.add("    lu.uni.epw.test.AbstractClass o_luuniepwtestAbstractClass = null;");
		abstractWrapperOracle.add("    o_luuniepwtestAbstractClass = GenerationGG.get_luuniepwtestAbstractClass();");
		abstractWrapperOracle.add("    try {");
		abstractWrapperOracle.add("    o_luuniepwtestAbstractClass.abst1();");
		abstractWrapperOracle.add("    } catch (Throwable e) {}");
		abstractWrapperOracle.add("    try {");
		abstractWrapperOracle.add("    o_luuniepwtestAbstractClass.abstParam((lu.uni.epw.test.AbstractClass)GenerationGG.get_luuniepwtestAbstractClass());");
		abstractWrapperOracle.add("    } catch (Throwable e) {}");
		abstractWrapperOracle.add("    try {");
		abstractWrapperOracle.add("    o_luuniepwtestAbstractClass.conc1();");
		abstractWrapperOracle.add("    } catch (Throwable e) {}");
		abstractWrapperOracle.add("    try {");
		abstractWrapperOracle.add("    o_luuniepwtestAbstractClass.conc2((int)0, (int)0);");
		abstractWrapperOracle.add("    } catch (Throwable e) {}");
		abstractWrapperOracle.add("  }");
		abstractWrapperOracle.add("}");	

//		concreteWrapperOracle.add("public class Wrapper_lu_uni_epw_test_ConcreteClass{");
//		concreteWrapperOracle.add("  public static void test_lu_uni_epw_test_ConcreteClass() {");
//		concreteWrapperOracle.add("    lu.uni.epw.test.ConcreteClass o_luuniepwtestConcreteClass = null;");
//		concreteWrapperOracle.add("    o_luuniepwtestConcreteClass = GenerationGG.get_luuniepwtestConcreteClass();");
//		concreteWrapperOracle.add("    try {");
//		concreteWrapperOracle.add("    o_luuniepwtestConcreteClass.abst1();");
//		concreteWrapperOracle.add("    } catch (Throwable e) {}");
//		concreteWrapperOracle.add("    try {");
//		concreteWrapperOracle.add("    o_luuniepwtestConcreteClass.conc1();");
//		concreteWrapperOracle.add("    } catch (Throwable e) {}");
//		concreteWrapperOracle.add("    try {");
//		concreteWrapperOracle.add("    o_luuniepwtestConcreteClass.conc2((int)0, (int)0);");
//		concreteWrapperOracle.add("    } catch (Throwable e) {}");
//		concreteWrapperOracle.add("  }");
//		concreteWrapperOracle.add("}");	

		genOracle.add("public class GenerationGG {");
		genOracle.add("  public static lu.uni.epw.test.ConcreteClass get_luuniepwtestConcreteClass() {");
		genOracle.add("    lu.uni.epw.test.ConcreteClass o_o = (lu.uni.epw.test.ConcreteClass) null;");
		genOracle.add("    o_o = new lu.uni.epw.test.ConcreteClass((int)0,(int)0);");
		genOracle.add("    return o_o;");
		genOracle.add("  }");
		genOracle.add("  public static lu.uni.epw.test.AbstractClass get_luuniepwtestAbstractClass() {");
		genOracle.add("    lu.uni.epw.test.AbstractClass o_o = (lu.uni.epw.test.AbstractClass) null;");
		genOracle.add("    o_o = (lu.uni.epw.test.AbstractClass)get_luuniepwtestConcreteClass();");
		genOracle.add("    return o_o;");
		genOracle.add("  }");
		genOracle.add("}");
 
  }
 
  protected void setUp() throws Exception {
    initOracles();
    String targetDir = "./test-data/abstractClass/";
    setUpSoot(targetDir, defaultOutputDir);
  }
  
  public void testConcreteClassk() throws FileNotFoundException, IOException { 
   
    runSoot();
    
    //
    File outdir = new File (defaultOutputDir);
    File[] list = outdir.listFiles();
    assertEquals(4, list.length); // GenerationGG.java, 
    // Wrapper....java, MainClass.java, stats.txt

    //
    testJavaFile(mainClassOracle, outdir.getPath() +"/MainClass.java"); 
// only wrappers for Abstract are generated    testJavaFile(concreteWrapperOracle, outdir.getPath() +"/Wrapper_lu_uni_epw_test_ConcreteClass.java"); 
    testJavaFile(abstractWrapperOracle, outdir.getPath() +"/Wrapper_lu_uni_epw_test_AbstractClass.java"); 
    testJavaFile(genOracle, outdir.getPath() +"/GenerationGG.java"); 
  }
 
  public void tearDown() {
    tearDownSoot();
  }
  
}
