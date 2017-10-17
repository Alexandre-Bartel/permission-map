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

package lu.uni.epw.gent;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.sun.corba.se.impl.javax.rmi.CORBA.Util;

import lu.uni.epw.Debug;
import lu.uni.epw.Wrappers;
import soot.FastHierarchy;
import soot.Scene;
import soot.SootClass;

public class GenerateTypes {
  
  String targetDir = null;
  String typesFilename = null;
  
  
  HashMap<String, String> generatedCC = new HashMap<String, String>(); // concrete type -> method to get type
  HashMap<String, String> generatedIC = new HashMap<String, String>(); // interface type -> method to get type
  HashMap<String, String> generatedAC = new HashMap<String, String>(); // abstract type -> method to get type
  
  /**
   * 
   * @param targetDirectory
   * @param typesFilename
   */
  public void generateTypeMethods (String targetDirectory, String typesFilename, Map<String, Set<SootClass>> typesToGenerate) {

    this.targetDir = targetDirectory;
    this.typesFilename = typesFilename;

//    // get classes present in Soot's Scene
//    Chain<SootClass> scChain = Scene.v().getClasses();
//
//    // generate static objects for all classes
//    Set<SootClass> abstractClasses = null;//new ArrayList<SootClass>();
//    Set<SootClass> interfaceClasses = null;//new ArrayList<SootClass>();
//    Set<SootClass> concreteClasses = null;//new ArrayList<SootClass>();
//    //List<SootClass> innerClass = new List<SootClass>();
//    
//    List<SootClass> targetClasses = new ArrayList<SootClass>();
//    
//    
//    
//    
//    for (SootClass sc: scChain) {
//      System.out.println("class: "+ sc);
//      String scName = sc.getName();
//      if (!sc.isPublic() || sc.isProtected() || sc.isPrivate()) {
//        System.out.println("W: do not handle non-public class '"+ sc +"' public: "+ sc.isPublic() +" private: "+ sc.isPrivate() +" protected: "+ sc.isProtected());
////        for (SootMethod sm: sc.getMethods()) {
////          System.out.println("   m:"+ sm);
////        }
//        continue;
//      }
////      if (!(scName.startsWith("android.") || scName.startsWith("com.android."))) {
////        Debug.printDbg("skipping class '"+ scName +"'");
////        continue;
////      }
//      // remove anonymous classes
//      if (scName.matches("\\$[0-9]*$")) {
//        Debug.printDbg("skipping anonymous: "+ scName);
//        continue;
//      } else if (scName.contains("$")) {
//        System.out.println("skipping inner class: "+ sc);
//        continue;
//      } else if (!Config.isTargetClass(scName)) { //scName.startsWith("java.") || scName.startsWith("sun.") || scName.startsWith("javax.") || scName.startsWith("com.sun.") || scName.startsWith("org.")) {
//        Debug.printDbg("skipping non target class: "+ scName);
//        continue;
//      }
//      if (sc.isInterface()) {
//        interfaceClasses.add (sc);
//        System.out.println("add class '"+ sc +"' as interface");
//      } else if (sc.isAbstract()) {
//        abstractClasses.add (sc);
//        System.out.println("add class '"+ sc +"' as abstract");
//      } else if (sc.isConcrete()) {
//        concreteClasses.add (sc);
//        System.out.println("add class '"+ sc +"' as concrete");
//      } else {
//        throw new RuntimeException("Error: class '"+ sc +"' is not interface/abstract/concrete");
//      }
//      System.out.println("add class: "+ sc);
//      targetClasses.add(sc);
//    }
//    
////    ConcreteClass.generate (concreteClasses);
////    AbstractClass.generate (abstractClasses);
////    InterfaceClass.generate (interfaceClasses);
////    generateOtherClasses ();
////    generateParameterClasses (targetClasses);
    
    FastHierarchy h = Scene.v().getOrMakeFastHierarchy();
    Set<SootClass> allClasses = new HashSet<SootClass>();
    allClasses.addAll(typesToGenerate.get("concrete"));
    allClasses.addAll(typesToGenerate.get("abstract"));
    allClasses.addAll(typesToGenerate.get("interface"));
    for (SootClass sc: allClasses) {
    	Set<SootClass> subclassesCollec = lu.uni.epw.Util.getAllSubclassesOf (sc);
    	for (SootClass toAdd: subclassesCollec) {
    		if (!toAdd.isPublic())
    			continue;
    		Wrappers.addClassToTTG(toAdd);
    	}
    }
    
    ConcreteClass.generate(typesToGenerate.get("concrete"), generatedCC);
    AbstractClass.generate(typesToGenerate.get("abstract"), generatedAC);
    InterfaceClass.generate(typesToGenerate.get("interface"), generatedIC);
    outputGeneratedClasses ();
    
  }
  
  
  /**
   * 
   */
  public void outputGeneratedClasses() {
    // output generated classes
    {
      FileWriter fw = null;
      PrintWriter pw = null; 
      try {
        fw = new FileWriter(targetDir + typesFilename +".java");
        Debug.printDbg("[I] opening file "+ targetDir + typesFilename +".java");
        pw = new PrintWriter(fw);               
      } catch (Exception e) {
        System.err.println("[E] "+ e);
        System.exit(-1);
      }

      pw.println("public class "+ typesFilename +" {");

      pw.println("");
      pw.println("////");
      pw.println("//// GENERATING CONCRETE CLASSES");
      pw.println("////");
      pw.println("");
      for (String k: generatedCC.keySet()) {
        pw.println("  // for k: "+ k);
        pw.println(generatedCC.get(k));
      }
      pw.println("");
      pw.println("////");
      pw.println("//// GENERATING ABSTRACT CLASSES");
      pw.println("////");
      pw.println("");
      for (String k: generatedAC.keySet()) {
        pw.println("  // for k: "+ k);
        pw.println(generatedAC.get(k));
      }
      pw.println("");
      pw.println("////");
      pw.println("//// GENERATING INTERFACES CLASSES");
      pw.println("////");
      pw.println("");
      for (String k: generatedIC.keySet()) {
        pw.println("  // for k: "+ k);
        pw.println(generatedIC.get(k));
      }
//      pw.println("");
//      pw.println("////");
//      pw.println("//// GENERATING NEEDED CLASSES");
//      pw.println("////");
//      pw.println("");
//      for (String k: generatedNC.keySet()) {
//        pw.println("  // for k: "+ k);
//        pw.println(generatedNC.get(k));
//      }
//      pw.println("");
//      pw.println("////");
//      pw.println("//// GENERATING PARAMETER CLASSES");
//      pw.println("////");
//      pw.println("");
//      for (String k: generatedParameterClass.keySet()) {
//        pw.println("  // for k: "+ k);
//        pw.println(generatedParameterClass.get(k));
//      }

      pw.println("");
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
  
}
