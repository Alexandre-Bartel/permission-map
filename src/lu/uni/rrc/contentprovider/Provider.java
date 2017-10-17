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

package lu.uni.rrc.contentprovider;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lu.uni.rrc.services.GenerateServiceInit;
import soot.Body;
import soot.Local;
import soot.Scene;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.javaToJimple.LocalGenerator;
import soot.jimple.Jimple;
import soot.jimple.StringConstant;

public class Provider {
    public Provider() {
    }

    public String name;
    public String classname;
    public String readPermission = null;
    public String writePermission = null;
    public Set<String> permissions = new HashSet<String>();
    public Set<String> authorities = new HashSet<String>();

    // load info from content provider from file
    public static List<Provider> providers = new ArrayList<Provider>();
    private static Map<String, Provider> auth2cp = new HashMap<String, Provider>();

    private static void add(Provider p) {
        providers.add(p);
        for (String auth : p.authorities) {
            System.out.println("add '" + auth + "' to " + p.classname);
            auth2cp.put(auth, p);
        }
    }

    public static void createJimple(Body b, Unit uriUnit, SootMethod cpMethod, String uri) {

        if (uri == null) {
            System.out.println("warning: authority is null!!");
        }

        String auth = uri.replaceAll("content://", "");
        auth = auth.replaceAll("/.*", "");

        Provider cp = auth2cp.get(auth);
        if (cp == null) {
            System.out.println("warning: authority not found '" + auth + "'");
            return;
        }

        System.out.println("content provider: redirecting to " + cp.classname + " ("
                + cp.authorities);

        LocalGenerator lg = new LocalGenerator(b);

        List<Unit> newUnits = new ArrayList<Unit>();
        SootClass servicesInitClass = Scene.v().getSootClass(
                GenerateServiceInit.servicesInitClassName);
        SootField sf = servicesInitClass.getFieldByName("androidcontentContext");
        Value newR = Jimple.v().newStaticFieldRef(sf.makeRef());
        Local contextLocal = lg.generateLocal(sf.getType());
        Unit u0 = Jimple.v().newAssignStmt(contextLocal, newR);
        newUnits.add(u0);

        // call content provider onCreate()
        SootClass cpClass = Scene.v().getSootClass(cp.classname);
        Local cpLocal = lg.generateLocal(cpClass.getType());
        Unit u1 = Jimple.v().newAssignStmt(cpLocal, Jimple.v().newNewExpr(cpClass.getType()));

        boolean has = false;
        for (SootMethod sm : cpClass.getMethods()) {
            if (sm.getName().equals("onCreate")) {
                has = true;
            }
        }
        if (!has) {
            System.out.println("warning: no method onCreate found!");
            return;
        }

        Unit u2 = Jimple.v().newInvokeStmt(
                Jimple.v().newVirtualInvokeExpr(cpLocal,
                        Scene.v().getMethod("<" + cpClass + ": boolean onCreate()>").makeRef()));
        newUnits.add(u1);
        newUnits.add(u2);

        // call Context.checkPermission with
        // content provider's readPermission and/or writePermission
        SootMethod checkPermission = Scene.v().getMethod(
                "<android.content.Context: int checkCallingPermission(java.lang.String)>");
        if (cp.readPermission != null) {
            Unit ru = Jimple.v().newInvokeStmt(
                    Jimple.v().newVirtualInvokeExpr(contextLocal, checkPermission.makeRef(),
                            StringConstant.v(cp.readPermission)));
            newUnits.add(ru);
        }

        if (cp.writePermission != null) {
            Unit wu = Jimple.v().newInvokeStmt(
                    Jimple.v().newVirtualInvokeExpr(contextLocal, checkPermission.makeRef(),
                            StringConstant.v(cp.writePermission)));
            newUnits.add(wu);
        }

        for (Unit u : newUnits) {
            b.getUnits().insertBefore(u, uriUnit);
        }

        // call (redirect) call to content provider's cpMethod
        // TODO
    }

    public static void loadCP(String cpFilePath) {
        String PROVIDER = "  p           : ";
        String NAME = "  name        : ";
        String CLASSNAME = " class        : ";
        String READPERMISSION = "  read        :";
        String WRITEPERMISSION = "  write       :";
        String AUTHORITIES = "  authorities :";
        BufferedReader br;
        try {
            br = new BufferedReader(new InputStreamReader(new FileInputStream(cpFilePath)));
            String line;
            Provider p = null;
            while ((line = br.readLine()) != null) {
                if (line.startsWith(PROVIDER)) {
                    if (!(p == null)) {
                        Provider.add(p);
                    }
                    p = new Provider();
                } else if (line.startsWith(NAME)) {
                    p.name = line.split(":")[1].trim();
                } else if (line.startsWith(CLASSNAME)) {
                    p.classname = line.split(":")[1].trim();
                } else if (line.startsWith(READPERMISSION)) {
                    String perm = line.split(":")[1].trim();
                    if (perm.length() > 0) {
                        p.readPermission = perm;
                    }
                } else if (line.startsWith(WRITEPERMISSION)) {
                    String perm = line.split(":")[1].trim();
                    if (perm.length() > 0) {
                        p.writePermission = perm;
                    }
                } else if (line.startsWith(AUTHORITIES)) {
                    String authorities = line.split(":")[1].trim();
                    for (String a : authorities.split(";")) {
                        p.authorities.add(a);
                    }
                }
            }
            if (p != null) {
                Provider.add(p);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            throw new RuntimeException("error: " + e);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("error: " + e);
        }

        for (Provider p : providers) {
            System.out.println("added provider " + p.name);
            System.out.println("  class: " + p.classname);
            System.out.println("  rPerm: " + p.readPermission);
            System.out.println("  wPerm: " + p.writePermission);
            System.out.print("   authorities: ");
            for (String a : p.authorities) {
                System.out.print(a + "; ");
            }
            System.out.println();
            System.out.println();
        }

    }
}
