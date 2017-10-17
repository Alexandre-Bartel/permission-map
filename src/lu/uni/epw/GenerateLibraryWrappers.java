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
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lu.uni.epw.gent.GatherTypes;
import lu.uni.epw.gent.GenerateTypes;
import soot.G;
import soot.PackManager;
import soot.SceneTransformer;
import soot.SootClass;
import soot.Transform;
import soot.options.Options;
import soot.util.JasminOutputStream;

/**
 * 
 * @author alex
 *
 */
public class GenerateLibraryWrappers
{

    public static String targetDir = null;
    public static String typesFilename = "GenerationGG";

    public static void usage() {
        System.out.println("Usage: java GenerateLibrayWrappers -config <path/to/config/file> <soot args>");
    }

    public static void main(String[] args) {

        List<String> argsRaw = new ArrayList<String>(Arrays.asList(args));
        List<String> argsList = new ArrayList<String>();

        // parse non-Soot parameters
        int SKIP_ARGS = 8;
        try {
            String configFileFlag = argsRaw.get(0);
            String configFilePath = argsRaw.get(1);
            String managerFileFlag = argsRaw.get(2);
            String managerFilePath = argsRaw.get(3);
            String managerClassesFlag = argsRaw.get(4);
            String managerClasses = argsRaw.get(5);
            String generateTypesFlag = argsRaw.get(6);
            String generateTypes = argsRaw.get(7);
            
            if (!configFileFlag.equals("-config"))
                throw new Exception("Expected -config flag! not '"+ configFileFlag +"'");
            File configFile = new File(configFilePath);
            if (!configFile.exists())
                throw new Exception("Config file does not exists!");
            Config.v().loadConfig(configFile);

            if (!managerFileFlag.equals("-manager"))
                new Exception("Expected -manager flag! not '"+ managerFileFlag +"'");
            if (! managerFilePath.equals("none")) {
                File managerFile = new File(managerFilePath);
                if (!managerFile.exists()) {
                    throw new Exception("Manager file does not exists!");
                }
                Managers.loadManagers(managerFilePath);
            }
            
            if (!managerClassesFlag.equals("-manager-classes"))
            	throw new RuntimeException("unknown option "+ managerClassesFlag);
            File managerClassesFile = new File(managerClasses);
            if (!managerClassesFile.exists())
            	throw new RuntimeException("file does not exist: "+ managerClassesFile);
            Config.v().loadFrameworkClasses(managerClassesFile);

            if (!generateTypesFlag.equals("-generateTypes"))
                new RuntimeException("error: Expected -generateTypes flag! not '"+ generateTypesFlag +"'");
            boolean generateTypesBoolean = Boolean.parseBoolean(generateTypes);
            Config.v().setGenerateType(generateTypesBoolean);
            System.out.println("[info] generating types: "+ (Config.v().getGenerateType() == true ? "true" : "false"));


        } catch (Throwable e) {
            System.out.println("error: "+ e.getMessage());
            usage();
            System.exit(-1);
        }

        // continue with Soot parameters
        for (int i=SKIP_ARGS; i<argsRaw.size(); i++) {
            argsList.add( argsRaw.get(i) );
        }

        PackManager.v().getPack("wjtp").add(new Transform("wjtp.myTrans", new SceneTransformer() {

            @Override
            protected void internalTransform(String phaseName, Map options) {

                Debug.setDebug(Options.v().verbose());

                // set output directory
                targetDir = Options.v().output_dir();
                File f = new File (targetDir);
                if (!f.exists())
                    f.mkdir();

                // Step 1: generate a wrapper file for each class
                System.out.println("Step 1: generating wrapper class for each library class...");
                Wrappers w = new Wrappers();
                Map<String, Set<SootClass>> typesToGenerate = null;
                try {
                    typesToGenerate = w.generateWrappers(targetDir);
                } catch (IOException e) {
                    e.printStackTrace();
                    System.err.println("io error...exiting...");
                    System.exit(-1);
                }

                if (Config.v().getGenerateType()) {
                    if (Debug.DEBUG) {
                        System.out.println("\n*** interface types to generate:");
                        for (SootClass sc: typesToGenerate.get("interface")) {
                            System.out.println("sc "+ sc);
                        }
                        System.out.println("\n*** abstract types to generate:");
                        for (SootClass sc: typesToGenerate.get("abstract")) {
                            System.out.println("sc "+ sc);
                        }
                        System.out.println("\n*** concrete types to generate:");
                        for (SootClass sc: typesToGenerate.get("concrete")) {
                            System.out.println("sc "+ sc);
                        }
                    }
                    System.out.println("Step 2.1: ... gathering types ...");
                    //GatherTypes.gatherTypes(typesToGenerate);
                    
//                    // remove excluded types:
//                    List<String> toRemove = new ArrayList<String>();
//                    for (String t: typesToGenerate.keySet()) {
//                    	if(!Config.v().isTargetClass(t)) {
//                    		toRemove.add(t);
//                    	}
//                    }
//                    for (String tr: toRemove) {
//                    	System.out.println("removing excluded type "+ tr);
//                    	typesToGenerate.remove(tr);
//                    }


                    System.out.println("Step 2.1: ...done.");

                    // Step 2.2: generate methods (which return initialized types)
                    System.out.println("Step 2.2: generating methods to initialize types in "+ targetDir +"/"+ typesFilename +"...");
                    GenerateTypes gent = new GenerateTypes();
                    gent.generateTypeMethods(targetDir, typesFilename, typesToGenerate);
                    System.out.println("Step 2.2: ...done.");
                } else {
                    System.out.println("Skipping type generationg.");
                }

                System.out.println("Done.");

            }
        }));

        // enable whole program mode
        argsList.add("-w");
        //argsList.add("-full-resolver");

        //
        argsList.add("-p");
        argsList.add("jb");
        argsList.add("enabled:false");

        //
        argsList.add("-p");
        argsList.add("wjtp");
        argsList.add("enabled:true");

        //
        argsList.add("-p");
        argsList.add("wjap");
        argsList.add("enabled:false");

        //
        argsList.add("-p");
        argsList.add("wjop");
        argsList.add("enabled:false");

        //
        argsList.add("-p");
        argsList.add("cg");
        argsList.add("enabled:false");

        args = argsList.toArray(new String[0]);

        soot.Main.main(args);
    }

    private static void dumpClass(SootClass c) {

        OutputStream streamOut = null;
        PrintWriter writerOut = null;

        // build name       
        StringBuffer b = new StringBuffer();
        b.append(Options.v().output_dir());
        if ((b.length() > 0) && (b.charAt(b.length() - 1) != File.separatorChar))
            b.append(File.separatorChar);
        b.append(c.getName().replace('.', File.separatorChar));
        b.append(".class");
        String fileName = b.toString();

        new File(fileName).getParentFile().mkdirs();
        streamOut = new JasminOutputStream(streamOut);         
        writerOut = new PrintWriter(new OutputStreamWriter(streamOut));
        G.v().out.println( "Writing to "+fileName );

        if (c.containsBafBody())
            new soot.baf.JasminClass(c).print(writerOut);
        else
            new soot.jimple.JasminClass(c).print(writerOut);

    }
}
