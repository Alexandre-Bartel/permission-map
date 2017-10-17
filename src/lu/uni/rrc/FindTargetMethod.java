//
// (c) 2014 TU Darmstadt
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

package lu.uni.rrc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import soot.Body;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.FieldRef;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;

/*
 * This class is used to find interesting methods for all different modules.
 * Instead of having all modules to loop through all classes and methods
 * of Soot's Scene, we do the loop only once and record for every
 * module their method of interest.
 */
public class FindTargetMethod {

    public static FindTargetMethod singleton = null;

    String cache_filename = "/tmp/interests.cache";
    boolean use_cache = false;

    public static FindTargetMethod v() {
        if (singleton == null)
            singleton = new FindTargetMethod();
        return singleton;
    }

    private FindTargetMethod() {
        File f = new File(cache_filename);
        if (f.exists()) {
            use_cache = true;
            try {
                readFromFile(cache_filename);
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException("error when reading cache file " + cache_filename);
            }
        }
    }

    // for methods
    Map<String, String> MinterestMap = new HashMap<String, String>();
    Map<String, Set<String>> MinterestResultMap = new HashMap<String, Set<String>>();

    // for methods (reverse string comparison)
    Map<String, String> rMinterestMap = new HashMap<String, String>();
    Map<String, Set<String>> rMinterestResultMap = new HashMap<String, Set<String>>();

    // for fields
    Map<String, String> FinterestMap = new HashMap<String, String>();
    Map<String, Set<String>> FinterestResultMap = new HashMap<String, Set<String>>();

    public void addInterest(String category, String name, String targetMS) {

        if (use_cache) {
            return;
        }

        Map<String, String> interestMap = null;
        Map<String, Set<String>> interestResultMap = null;
        if (category.equals("method")) {
            interestMap = MinterestMap;
            interestResultMap = MinterestResultMap;
        } else if (category.equals("r_method")) {
            interestMap = rMinterestMap;
            interestResultMap = rMinterestResultMap;
        } else if (category.equals("field")) {
            interestMap = FinterestMap;
            interestResultMap = FinterestResultMap;
        } else {
            throw new RuntimeException("error: invalid category '" + category + "'");
        }
        if (interestMap.containsKey(name))
            throw new RuntimeException("error: key exists '" + name + "'");
        System.out.println(" [-] add interest '" + name + "' for target '" + targetMS);
        interestMap.put(name, targetMS);
        interestResultMap.put(name, new HashSet<String>());
    }

    public void addInterest(String category, String name, List<String> sigList) {
        int i = 0;
        System.out.println("  [-] add list of " + sigList.size() + " signatures.");
        for (String sig : sigList) {
            addInterest(category, name + i++, sig);
        }
    }

    public void computeInterests() {

        // values already loaded when creating object
        if (use_cache) {
            return;
        }

        int totalSC = Scene.v().getClasses().size();
        int p1 = totalSC / 100;
        int count = 0;
        int i = 0;
        System.out.print("");
        for (SootClass sc : Scene.v().getClasses()) {
            count++;
            if (count >= p1) {
                count = 0;
                System.out.print("\r" + "Parsing code for interisting methods: " + ++i + "% completed");
            }
            for (SootMethod sm : sc.getMethods()) {
                if (!sm.isConcrete()) {
                    continue;
                }

                Body b = null;
                try {
                    b = sm.retrieveActiveBody();
                } catch (Exception e) {
                    System.out.println("[E] when retrieving body for method " + sm);
                    continue;
                }

                analyzeBody(sm, b);

            }
        }

        try {
            writeToFile(cache_filename);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("error when writting cache file " + cache_filename);
        }

    }

    private void analyzeBody(SootMethod sm, Body b) {
        for (Unit u : b.getUnits()) {
            Stmt s = (Stmt) u;
            // methods
            if (s.containsInvokeExpr()) {
                InvokeExpr ie = s.getInvokeExpr();
                String ieSig = ie.getMethodRef().getSignature(); // //ie.getMethod().getSignature();

                for (String k : MinterestMap.keySet()) {
                    String targetSig = MinterestMap.get(k);
                    if (ieSig.startsWith(targetSig)) {
                        MinterestResultMap.get(k).add(sm.getSignature());
                    }
                }
                for (String k : rMinterestMap.keySet()) {
                    String targetSig = rMinterestMap.get(k);
                    if (ieSig.endsWith(targetSig)) {
                        rMinterestResultMap.get(k).add(sm.getSignature());
                    }
                }
            }
            // fields
            else if (s.containsFieldRef()) {

                FieldRef fr = s.getFieldRef();
                String fType = fr.getType().toString();

                for (String k : FinterestMap.keySet()) {
                    String targetFType = FinterestMap.get(k);
                    if (fType.startsWith(targetFType)) {
                        FinterestResultMap.get(k).add(sm.getSignature());
                    }
                }
            }
        }

    }

    public Set<SootMethod> getInterest(String category, String name) {
        Map<String, Set<String>> interestResultMap = null;
        if (category.equals("method")) {
            interestResultMap = MinterestResultMap;
        } else if (category.equals("r_method")) {
            interestResultMap = rMinterestResultMap;
        } else if (category.equals("field")) {
            interestResultMap = FinterestResultMap;
        } else {
            throw new RuntimeException("error: invalid category '" + category + "'");
        }

        if (!interestResultMap.containsKey(name)) {
        	throw new RuntimeException("key '"+ name +"' not found: "+ interestResultMap.keySet());
        }
        
        return makeSootMethodSet(interestResultMap.get(name));
    }

    public List<Set<SootMethod>> getInterest(String category, String name, List<String> sigList) {
        List<Set<SootMethod>> set = new ArrayList<Set<SootMethod>>();

        Map<String, Set<String>> interestResultMap = null;
        if (category.equals("method")) {
            interestResultMap = MinterestResultMap;
        } else if (category.equals("r_method")) {
            interestResultMap = rMinterestResultMap;
        } else if (category.equals("field")) {
            interestResultMap = FinterestResultMap;
        } else {
            throw new RuntimeException("error: invalid category '" + category + "'");
        }

        for (int i = 0; i < sigList.size(); i++) {
            set.add(makeSootMethodSet(interestResultMap.get(name + i)));
        }

        return set;
    }

    private Set<SootMethod> makeSootMethodSet(Set<String> ss) {
        Set<SootMethod> smSet = new HashSet<SootMethod>();
        for (String s : ss) {
        	if (!Scene.v().containsMethod(s)) {
        		System.err.println("Warning: method not found in Scene: "+ s);
        		continue;
        	}
            SootMethod sm = Scene.v().getMethod(s);
            smSet.add(sm);
        }
        return smSet;
    }

    private void writeToFile(String fn) throws IOException {
        File file = new File(fn);
        FileOutputStream f = new FileOutputStream(file);
        ObjectOutputStream s = new ObjectOutputStream(f);
        s.writeObject(MinterestMap);
        s.writeObject(MinterestResultMap);
        s.writeObject(rMinterestMap);
        s.writeObject(rMinterestResultMap);
        s.writeObject(FinterestMap);
        s.writeObject(FinterestResultMap);
        s.close();
    }

    private void readFromFile(String fn) throws IOException, ClassNotFoundException {
        File file = new File(fn);
        FileInputStream f = new FileInputStream(file);
        ObjectInputStream s = new ObjectInputStream(f);
        MinterestMap = (HashMap<String, String>) s.readObject();
        MinterestResultMap = (HashMap<String, Set<String>>) s.readObject();
        rMinterestMap = (HashMap<String, String>) s.readObject();
        rMinterestResultMap = (HashMap<String, Set<String>>) s.readObject();
        FinterestMap = (HashMap<String, String>) s.readObject();
        FinterestResultMap = (HashMap<String, Set<String>>) s.readObject();
        s.close();
    }


}
