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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lu.uni.rrc.contentprovider.Provider;
import soot.Body;
import soot.Local;
import soot.Scene;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.FieldRef;
import soot.jimple.IdentityStmt;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.StaticInvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.StringConstant;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.SimpleLiveLocals;
import soot.toolkits.scalar.SimpleLocalUses;
import soot.toolkits.scalar.SmartLocalDefs;

public class HandleContentProviders {

	private static final Logger logger = LoggerFactory.getLogger(HandleContentProviders.class);

    public static List<String> sigList = new ArrayList<String>();
    private List<Set<SootMethod>> sList = new ArrayList<Set<SootMethod>>();
    {
        sigList.add("<android.content.ContentResolver: int bulkInsert");
        sigList.add("<android.content.ContentResolver: android.os.Bundle call");
        sigList.add("<android.content.ContentResolver: int delete");
        sigList.add("<android.content.ContentResolver: android.net.Uri insert");
        sigList.add("<android.content.ContentResolver: android.database.Cursor query");
        sigList.add("<android.content.ContentResolver: android.content.res.AssetFileDescriptor open");
        sigList.add("<android.content.ContentResolver: android.os.ParcelFileDescriptor open");
        sigList.add("<android.content.ContentResolver: java.io.InputStream open");
        sigList.add("<android.content.ContentResolver: java.io.OutputStream open");
        sigList.add("<android.content.ContentResolver: int update");

        for (String s : sigList) {
            sList.add(new HashSet<SootMethod>());
        }
    }



    public HandleContentProviders(List<Set<SootMethod>> l) {

        int i = 0;
        for (Set<SootMethod> s : l) {
            sList.get(i++).addAll(s);
        }
    }

    public void doWork() {
        int i = 0;
        for (Set<SootMethod> s : sList) {
            for (SootMethod sm : s) {
                Body b = sm.retrieveActiveBody();
                logger.debug("content provider - : ", sm, "\n", b);
                replace(b, sigList.get(i));

            }
            i++;
        }

    }

    private void replace(Body b, String targetMethod) {
        List<Unit> replaceList = new ArrayList<Unit>();
        for (Unit u : b.getUnits()) {
            Stmt s = (Stmt) u;
            if (s.containsInvokeExpr()) {
                InvokeExpr ie = s.getInvokeExpr();
                String ieSig = ie.getMethodRef().getSignature();
                if (ieSig.startsWith(targetMethod)) {
                    replaceList.add(u);
                }
            }
        }

        for (Unit u : replaceList) {
            checkUri(b, u);
            logger.debug("removing unit: ", u);
            Util.removeUnit(b, u);
        }
    }

    private void checkUri(Body b, Unit u) {
        // check if an argument is an Uri
        Value pUri = null;
        Stmt s = (Stmt) u;
        InvokeExpr ie = s.getInvokeExpr();
        InstanceInvokeExpr iie = (InstanceInvokeExpr) ie;
        logger.debug("same base for init...");
        int i = 0;
        for (Type pt : iie.getMethod().getParameterTypes()) {
            logger.debug("type: ", pt);
            if (pt.toString().equals("android.net.Uri")) {
                pUri = iie.getArg(i);
                break;
            }
            i++;
        }
        if (pUri == null) {
            System.out.println("warning: no Uri parameter");
        }
        if (pUri instanceof Local) {
            logger.debug("uri is local...");
            String auth = findUriDef(b, u, (Local) pUri);
            if (auth != null) {
                Provider.createJimple(b, u, ie.getMethod(), auth);
            }

        } else {
            throw new RuntimeException("error: not local referencing uri!");
        }

    }

    // return authority or null
    private String findUriDef(Body b, Unit u, Local l) {
        final UnitGraph g = new ExceptionalUnitGraph(b);
        final SmartLocalDefs localDefs = new SmartLocalDefs(g, new SimpleLiveLocals(g));
        final SimpleLocalUses localUses = new SimpleLocalUses(g, localDefs);

        List<Unit> defs = localDefs.getDefsOfAt((Local) l, u);
        if (defs.size() == 0) {
            System.out.println("warning: uri def empty!");
            return null;
        }
        Unit def = defs.get(0);
        logger.debug("uri def: " + def);
        if (def instanceof IdentityStmt) {
            System.out.println("warning: do not handle uri from identity stmt");
            return null;
        } else if (def instanceof AssignStmt) {
            AssignStmt ass = (AssignStmt) def;
            Value r = ass.getRightOp();
            if (r instanceof FieldRef) {
                FieldRef fr = (FieldRef) r;
                SootField sf = fr.getField();
                if (sf.getName().contains("URI")) {
                    String auth = getFieldFromClass(sf);
                    return auth;
                }
            } else {
                System.out.println("warning: uri: do not handle def '" + def + "'");
                return null;
            }
        }
        return null;
    }
    

    // return authority or null
    private String getFieldFromClass(SootField sf) {
        SootClass sc = sf.getDeclaringClass();
        if (!Scene.v().containsMethod("<"+ sc.getName() +": void <clinit>()>")) {
        	System.err.println("warning: class does not contain clinit: "+ sc);
        	return null;
        }
        SootMethod sm = sc.getMethodByName("<clinit>");
        Body b = sm.retrieveActiveBody();
        // System.out.println("bb: "+ b);
        for (Unit u : b.getUnits()) {
            Stmt s = (Stmt) u;
            if (s.containsInvokeExpr()) {
                InvokeExpr ie = s.getInvokeExpr();
                String name = ie.getMethod().getName();
                if (name.equals("parse")) {
                    StaticInvokeExpr sie = (StaticInvokeExpr) ie;
                    Value v = sie.getArg(0);
                    if (v instanceof StringConstant) {
                        StringConstant str = (StringConstant) v;
                        String authority = str.value;
                        logger.debug("constant: " + authority);
                        return authority;
                    }
                }
            }
        }
        return null;
    }



}
