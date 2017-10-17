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

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import soot.ArrayType;
import soot.Hierarchy;
import soot.PrimType;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Type;
import soot.VoidType;
import soot.tagkit.Tag;
import soot.util.Chain;

public class Wrappers {
	
	private static Logger logger = LoggerFactory.getLogger(Wrappers.class);

	public String targetDir = null;
	public String statsFileName = null;
	PrintWriter statsOut = null;
	Set<SootMethod> skippedMethods = new HashSet<SootMethod>();
	Set<SootClass> wrappedClasses = new HashSet<SootClass>();
	Set<SootMethod> uniqueWrappedMethods = new HashSet<SootMethod>();
	Map<SootMethod, Integer> allWrappedMethods = new HashMap<SootMethod, Integer>();

	Hierarchy h = null;
	public static String generatorClassName = "GenerationGG";

	static String mainClassName = "MainClass";

	List<String> methodsToCallFromMainClass = new ArrayList<String>();


	private static Map<String, Set<SootClass>> typesToGenerate = new HashMap<String, Set<SootClass>>();
	private static Map<SootClass, Set<Integer>> arrays = new HashMap<SootClass, Set<Integer>>();
	private static Set<SootClass> handledClasses = new HashSet<SootClass>();
	{
		typesToGenerate.put("concrete", new HashSet<SootClass>());
		typesToGenerate.put("abstract", new HashSet<SootClass>());
		typesToGenerate.put("interface", new HashSet<SootClass>());
		typesToGenerate.put("notInScene", new HashSet<SootClass>());

	}

	
	
	public static void addClassToTTG(SootClass sc) {
		if (!sc.isPublic())
			throw new RuntimeException("error: trying to add a non public class! "+ sc);
		if (sc.toString().contains("$")) {
			logger.warn("/!\\  not adding inner classes! {}", sc);
			return;
		}
		System.out.println("add public class: "+ sc +" "+ sc.isPublic() +" "+ sc.isPrivate() +" "+ sc.isProtected());
		handledClasses.add(sc);
		if (sc.isInterface()) {
			typesToGenerate.get("interface").add(sc);
		} else if (sc.isAbstract()) {
			typesToGenerate.get("abstract").add(sc);
		} else if (sc.isConcrete()) {
			typesToGenerate.get("concrete").add(sc);
		} else {
			throw new RuntimeException("error: class not interface nor abstract nor concrete! "+ sc);
		}
		
	}
	
	public static boolean isGeneratedType(SootClass sc) {
		if (typesToGenerate.get("interface").contains(sc)) {
			return true;
		}
		if (typesToGenerate.get("abstract").contains(sc)) {
			return true;
		}
		if (typesToGenerate.get("concrete").contains(sc)) {
			return true;
		}
		
		return false;
	}
	
	public static boolean isHandledClass(SootClass sc) {
		boolean ihc = handledClasses.contains(sc);
		System.out.println("isHandledeClass: "+ sc +" --> "+ ihc);
		return ihc;
	}
	
	public static void addToArray(SootClass base, int dim) {
        addClassToTTG(base);
        if (!arrays.containsKey(base)) {
        	arrays.put(base, new HashSet<Integer>());
        }
        System.out.println("add public array: "+ base +" "+ dim +" "+ base.isPublic() +" "+ base.isPrivate() +" "+ base.isProtected());
        arrays.get(base).add(new Integer(dim));
		
	}
	
	public static Map<SootClass, Set<Integer>> getArrays() {
		return arrays;
	}

	/**
	 * 
	 * @param targetDir directory where java classes are written
	 * @throws IOException 
	 * @desc Generates the following files in /targetDir/ :
	 * - MainClass.java, the class calling all Wrappers
	 * - a Wrapper.*.java class for each non-filtered class
	 */
	public Map<String, Set<SootClass>> generateWrappers(String targetDir) throws IOException {
		this.targetDir = targetDir;
		this.statsFileName = this.targetDir +"/stats.txt";



		// output file to print stats
		File statsFile = new File(statsFileName);
		if (!statsFile.exists())
			statsFile.createNewFile();
		statsOut = new PrintWriter(new FileOutputStream(statsFile));


		// print classes present in Soot's Scene
		Chain<SootClass> scChain = Scene.v().getClasses();
		List<SootClass> scList = new ArrayList<SootClass>();
		for(SootClass sc: scChain) {
			scList.add(sc);
		}
		for(SootClass sc: scList) {
			
			System.out.println("current class: "+ sc);

			// Class Filter
			String sclass = sc.toString();
			if (!Config.v().isTargetClass(sclass)) {
				System.out.println("do not generate wrapper for '"+ sc +"'");
				continue;
			}

			if (sc.isPrivate() || sc.isProtected()) {
				System.out.println("do not generate wrapper for private or protocted: "+ sc);
				continue;
			}

			if (sclass.contains("$")) {
				System.out.println("do not generate wrapper for inner class '"+ sc +"'");
				continue;
			}

			System.out.println("generate wrapper for: "+ sc);

			Debug.printDbg("[I] Class "+ sc 
					+" abstract: "+ (sc.isAbstract()?"1":"0")
					+" concrete: "+ (sc.isConcrete()?"1":"0")
					+" interface: "+ (sc.isInterface()?"1":"0")
					+" private: "+ (sc.isPrivate()?"1":"0")
					+" protected: "+ (sc.isProtected()?"1":"0")
					+" public: "+ (sc.isPublic()?"1":"0")
					+" modifiers: "+ Integer.toBinaryString(sc.getModifiers())
					+" tags: "+ sc.getTags()
					);
			List<SootMethod> smList = sc.getMethods();

			smList = Util.getAccessiblePublicMethods(sc);
			for (SootMethod sm: smList) {
				Debug.printDbg("  [I] Method get...(): "+ sm
						+" abstract: "+ (sm.isAbstract()?"1":"0")
						+" concrete: "+ (sm.isConcrete()?"1":"0")
						+" declared: "+ (sm.isDeclared()?"1":"0")
						+" native: "+ (sm.isNative()?"1":"0")
						+" private: "+ (sm.isPrivate()?"1":"0")
						+" protected: "+ (sm.isProtected()?"1":"0")
						+" public: "+ (sm.isPublic()?"1":"0")
						+" static: "+ (sm.isStatic()?"1":"0")
						+" synchronized: "+ (sm.isSynchronized()?"1":"0")
						+" modifiers: "+ Integer.toBinaryString(sm.getModifiers()) +" 0x"+ Integer.toHexString(sm.getModifiers())
						+" synthetic/bridge: "+ ((sm.getModifiers() & Util.ACC_SYNTHETIC) != 0 ? "1":"0")
						//+" tags: "+ sc.getTags()
						);
			}

			// does the real stuff: generates test files
			if ( (sc.isPublic() ) && 
					(sc.isConcrete() || sc.isAbstract() || sc.isInterface()) && 
					(sc.getModifiers() & Util.ACC_SYNTHETIC) == 0 ) {

				addClassToTTG(sc);

				List<SootMethod> mList = new ArrayList<SootMethod>();

				for (SootMethod sm: smList) {

					if (!Config.v().isFrameworkMethod(sm)) {
						System.out.println("not a framework method! Skipping "+ sm);
						continue;
					}

					if ((sm.isPublic() ) &&
							((sm.getModifiers() & Util.ACC_SYNTHETIC) == 0) &&
							(sm.isConcrete() || ((sc.isAbstract() || sc.isInterface()) && sm.isAbstract())) ) {
						mList.add (sm);
						List<Type> checkTypeList = new ArrayList<Type>();

						checkTypeList.add (sm.getReturnType());
						checkTypeList.addAll((List<Type>)sm.getParameterTypes());
						for (Type t: checkTypeList) {
							// skip primitive types
							if (t instanceof PrimType) 
								continue;
							// skip void type
							if (t instanceof VoidType)
								continue;
							// check that type is in Soot's Scene
							Type baseType = null;
							int dim = 0;
							if (t instanceof ArrayType) {
								baseType = ((ArrayType) t).baseType;
								dim = ((ArrayType) t).numDimensions;
							} else {
								baseType = t;
							}
							if (baseType instanceof PrimType)
								continue;
							
							SootClass baseClass = Scene.v().getSootClass(baseType.toString());

							if (baseClass.toString().startsWith("java"))
								continue;
							
							if (baseClass == null || baseClass.isPhantom() || !baseClass.isPublic()) {
								throw new RuntimeException("errr: class required for method parameter not  in scene! '"+ t +"' for method "+ sm);
//								typesToGenerate.get("notInScene").add(baseClass);
//								continue;
							}

							addClassToTTG(baseClass);
							for (int i = 1; i<=dim; i++) {
								addToArray(baseClass, i);
							}

						}
					}
				}

				if (mList.size() > 0) {

					generateWrapper(sc, mList);

				} else { // if(mList.size() > 0)
					Debug.printDbg("[W] "+ sc + "'s methods list is empty.");
				}
			} else {// if(sc.isConcrete ... 
				Debug.printDbg("[W] "+ sc + "is not concrete or/and public.");
			}
		} // for( classes in scene  ...

		generateMainClass(methodsToCallFromMainClass);

		statsOut.println();
		statsOut.println("wrapped classes: "+ wrappedClasses.size());
		statsOut.println("unique wrapped methods: "+ uniqueWrappedMethods.size());
		int allWrappedMethodsSize = 0;
		for (SootMethod sm: allWrappedMethods.keySet())
			allWrappedMethodsSize += allWrappedMethods.get(sm).intValue();
		statsOut.println("all wrapped methods: "+ allWrappedMethodsSize);
		statsOut.println("skipped methods: "+ skippedMethods.size());
		statsOut.println();
		for (SootMethod sm: skippedMethods)
			statsOut.println("  skipped method: "+ sm);

		Comparator<SootMethod> bvc =  new Comparator<SootMethod>() {

			Map<SootMethod, Integer> base = allWrappedMethods;
			// Note: this comparator imposes orderings that are inconsistent with equals.    
			public int compare(SootMethod a, SootMethod b) {
				if (base.get(a).intValue() >= base.get(b).intValue()) {
					return -1;
				} else {
					return 1;
				} // returning 0 would merge keys
			}
		};

		TreeMap<SootMethod, Integer> sorted_map = new TreeMap<SootMethod, Integer>(bvc);
		sorted_map.putAll(allWrappedMethods);

		for (SootMethod sm: allWrappedMethods.keySet()) {
			statsOut.println("a  "+ allWrappedMethods.get(sm) +" "+ sm);
		}
		for (SootMethod sm: sorted_map.keySet()) {
			statsOut.println("s  "+ allWrappedMethods.get(sm) +" "+ sm);
		}

		statsOut.close();

		return typesToGenerate;

	}


	/**
	 * 
	 * @param sc SootClass from which to generate a wrapper
	 * @param mList Subset of methods from sc called in the wrapper
	 * @throws IOException 
	 */
	private void generateWrapper(SootClass sc, List<SootMethod> mList) throws IOException {

		String className = sc.getName();
		String classNameWithUS = className.replaceAll("\\.", "_");
		String classt = className;
		String classti = "o_"+ className.replaceAll("\\.", "");

		if (className.contains("$")) { 
			Debug.printDbg("[W] "+ className +" contains $");
			className = className.replaceAll("\\$", "\\.");
			classt = classt.replaceAll("\\$", "\\.");
			classNameWithUS = classNameWithUS.replaceAll("\\$", "__");
			classti = classti.replaceAll("\\$", "__");
			Debug.printDbg("[W] "+ className +" does not contain $ anymore");
			//continue; 
		}

		FileWriter fw = null;
		PrintWriter pw = null; 
		try {
			fw = new FileWriter(targetDir +"Wrapper_"+ classNameWithUS + ".java");
			Debug.printDbg("[file] opening "+ targetDir +"Wrapper_"+ classNameWithUS + ".java" );
			pw = new PrintWriter(fw);               
		} catch (Exception e) {
			System.err.println("[E] "+ e);
			System.exit(-1);
		}

		methodsToCallFromMainClass.add (classNameWithUS);

		// start class
		wrappedClasses.add(sc);
		statsOut.println("class: "+ sc);
		pw.println("public class Wrapper_"+ classNameWithUS +"{");

		// start methodname

		pw.println("\tpublic static void test_"+ classNameWithUS +"() {");
		// ^--- substitute "." by "_"?
		pw.println("\t"+ classt +" "+ classti +" = null;");

		// classify methods (constructors, methods, ...)
		List<SootMethod> constructorList = new ArrayList<SootMethod>();
		List<SootMethod> methodList = new ArrayList<SootMethod>();


		for (SootMethod sm: mList) {
			if ((sm.getModifiers() & Util.ACC_SYNTHETIC) != 0) {
				Debug.printDbg("[MW] Synthetic method has been added: "+ sm +"!");
			}
			if (sm.toString().contains("<clinit>")) { 
				continue; 
			} else if (sm.toString().contains("<init>")) {
				constructorList.add(0, sm);
			} else if (sm.getReturnType().toString().equals(classt) && sm.isStatic()) {
				constructorList.add(constructorList.size(), sm);
			} else {
				methodList.add(sm);
			}

		}

		Debug.printDbg("[I] constructors: "+ constructorList.size() +" methods: "+ methodList.size());

		List<Tag> cTagList = sc.getTags();

		pw.println("");
		pw.println("// ** Constructors");
		if (Config.v().getGenerateType()) {
			pw.print("  "+ classti +" = "+ generatorClassName);
			String scGeneratorName = sc.getName().replaceAll("\\.", "").replaceAll("\\$", "__");
			if (sc.isConcrete()) {
				pw.println("."+ "get_" + scGeneratorName +"();");
			} else if (sc.isInterface()) {
				pw.println("."+ "get_" + scGeneratorName +"();");
			} else if (sc.isAbstract()) {
				pw.println("."+ "get_" + scGeneratorName +"();");
			} else {
				System.err.println("Error: unknown type of class '"+ sc +"'");
				System.exit(-1);
			}
		} else { 
			// dumb: does nothing
		}

		pw.println("");
		pw.println("// ** Methods");
		for (SootMethod sm: methodList) {
			boolean skipMethod = false;
			if (sm.getName().toString().equals("compareTo") ||
					sm.toString().startsWith("<java.")) {
				// avoid methods "classname: T compareTo(java.lang.Enum)"
				pw.println("// avoiding: "+ sm);
				continue; 
			}
			if (sm.getName().toString().startsWith("setAdapter")) {
				System.out.println("avoid method setAdapter: "+ sm);
				continue;
			}
			/*
      for (Tag t: sm.getTags()) {
        if (t instanceof SignatureTag) {
          pw.println("// '"+ sm +"' uses generics. SignatureTag: "+ t);
          skipMethod = true;
          break;
        }
      }
			 */
			for (Type t: (List<Type>)sm.getParameterTypes()) {
				//Debug.printDbg("["+ sm +"] loop through types in GenerateFWT: type: "+ t);
				if (Util.isParameterPrimType(t.toString())) { continue; }
				SootClass tc= Scene.v().getSootClass(t.toString()
						.replaceAll("\\[.*","")
						.replaceAll("<.*",""));
				if (!tc.isPublic()) {
					pw.println("// parameter '"+ t +"' is not public '"+ sm +"'");
					skipMethod = true;
					break;
				}
				if (t.toString().startsWith("java.lang.Object")) {
					pw.println("// parameters contain java.lang.Object '"+ sm +"'");
					skipMethod = true;
					break;
				}

				if (t.toString().contains("$")) {
					pw.println("// parameters contain innerclass '"+ sm +"'");
					//skipMethod = true;
					break;
				}

			}
			if (skipMethod) {
				skippedMethods.add(sm);
				continue;
			}

			if (allWrappedMethods.containsKey(sm)) {
				allWrappedMethods.put(sm, new Integer(allWrappedMethods.get(sm).intValue() + 1));
			} else {
				allWrappedMethods.put(sm, new Integer(1));
			}
			uniqueWrappedMethods.add(sm);
			statsOut.println("method: "+ sm);    
			boolean throwsException = true; // put try/catch everywhere since Soot has a hard time with methods with the same name one throwing an exception, the other not (sm.getExceptions().size() > 0 )?true:false;
			if (throwsException){ pw.println("\ttry {");}
			String parameters = null;
			if (sm.isStatic()) {
				pw.println("// "+ sm);
				if (Config.v().getGenerateType())
					parameters = new GenerateParameter(/*Scene.v(),*/ sm/*, 0*/).generateFromGG();
				else
					parameters = new GenerateParameter(/*Scene.v(),*/ sm/*, 0*/).generateDumb();
				pw.println("\t"+ classt +"."+ sm.getName() +"("+ parameters +");");
			} else {
				pw.println("// "+ sm);
				if (Config.v().getGenerateType())
					parameters = new GenerateParameter(/*Scene.v(),*/ sm/*, 0*/).generateFromGG();
				else
					parameters = new GenerateParameter(/*Scene.v(),*/ sm/*, 0*/).generateDumb();
				pw.println("\t"+ classti +"."+ sm.getName() +"("+ parameters +");");
			}
			if (throwsException){ pw.println("\t} catch (Throwable e) {}");}
		}

		// end methodname
		pw.println("\n}");
		// end class
		pw.println("}");

		try {
			pw.close();
			fw.close();
		} catch (Exception e) {
			System.err.println("[E] "+ e);
			System.exit(-1);
		}
	}

	/**
	 * 
	 * @param methodsToCallFromMainClass list of methods to call from the main class
	 */
	private void generateMainClass(List<String> methodsToCallFromMainClass) {
		FileWriter fw = null;
		PrintWriter pw = null;
		try {
			fw = new FileWriter(targetDir + mainClassName +".java");
			Debug.printDbg("[file] opening "+ targetDir + mainClassName +".java" );
			pw = new PrintWriter(fw);               
		} catch (Exception e) {
			System.err.println("[E] "+ e);
			System.exit(-1);
		}

		pw.println("public class "+ mainClassName +"{");
		pw.println("  public static void main (String[] args) {");

		if (Managers.canProcess) {
			pw.println("    ServicesInit.initServices();");
		}

		for (String s: methodsToCallFromMainClass) {
			pw.println("    Wrapper_"+ s +".test_"+ s +"();");
		}    

		pw.println("  }");
		pw.println("}");

		try {
			pw.close();
			fw.close();
		} catch (Exception e) {
			System.err.println("[E] "+ e);
			System.exit(-1);
		}
	}




}
