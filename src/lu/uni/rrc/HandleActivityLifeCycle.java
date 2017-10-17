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
import java.util.List;

import soot.ArrayType;
import soot.Body;
import soot.BooleanType;
import soot.ByteType;
import soot.CharType;
import soot.DoubleType;
import soot.FloatType;
import soot.IntType;
import soot.Local;
import soot.LongType;
import soot.RefType;
import soot.Scene;
import soot.ShortType;
import soot.SootClass;
import soot.SootMethod;
import soot.Type;
import soot.Unit;
import soot.VoidType;
import soot.javaToJimple.LocalGenerator;
import soot.jimple.AssignStmt;
import soot.jimple.DoubleConstant;
import soot.jimple.FloatConstant;
import soot.jimple.IdentityStmt;
import soot.jimple.IntConstant;
import soot.jimple.Jimple;
import soot.jimple.LongConstant;
import soot.jimple.NullConstant;
import soot.jimple.ParameterRef;
import soot.jimple.ThisRef;
import soot.toolkits.scalar.UnusedLocalEliminator;

public class HandleActivityLifeCycle {

    public static void empty() {
        SootClass sc = Scene.v().getSootClass("com.android.server.am.ActivityManagerService");
        for (SootMethod sm : sc.getMethods()) {
            if (sm.getName().toLowerCase().contains("activity")) {
                if (sm.isConcrete()) {
                    System.out.println("empty method: " + sm);
                    emptyBody(sm.retrieveActiveBody());
                }
            }
        }

    }

    public static void emptyBody(Body jBody) {

        LocalGenerator lg = new LocalGenerator(jBody);

        // identity statements
        List<Unit> idStmts = new ArrayList<Unit>();
        for (Unit u : jBody.getUnits()) {
            if (u instanceof IdentityStmt) {
                IdentityStmt i = (IdentityStmt) u;
                if (i.getRightOp() instanceof ParameterRef || i.getRightOp() instanceof ThisRef)
                    idStmts.add(u);
            }
        }

        jBody.getUnits().clear();
        jBody.getTraps().clear();

        for (Unit u : idStmts)
            jBody.getUnits().add(u);
        Type rType = jBody.getMethod().getReturnType();

        jBody.getUnits().add(Jimple.v().newNopStmt());

        if (rType instanceof VoidType) {
            jBody.getUnits().add(Jimple.v().newReturnVoidStmt());
        } else {
            Type t = jBody.getMethod().getReturnType();

            UnusedLocalEliminator.v().transform(jBody);

            Local l = lg.generateLocal(t);
            l.setType(t);

            AssignStmt ass = null;
            if (t instanceof RefType || t instanceof ArrayType) {
                ass = Jimple.v().newAssignStmt(l, NullConstant.v());
            } else if (t instanceof LongType) {
                ass = Jimple.v().newAssignStmt(l, LongConstant.v(0));
            } else if (t instanceof FloatType) {
                ass = Jimple.v().newAssignStmt(l, FloatConstant.v(0.0f));
            } else if (t instanceof IntType) {
                ass = Jimple.v().newAssignStmt(l, IntConstant.v(0));
            } else if (t instanceof DoubleType) {
                ass = Jimple.v().newAssignStmt(l, DoubleConstant.v(0));
            } else if (t instanceof BooleanType || t instanceof ByteType || t instanceof CharType
                    || t instanceof ShortType) {
                ass = Jimple.v().newAssignStmt(l, IntConstant.v(0));
            } else {
                throw new RuntimeException("error: return type unknown: " + t + " class: "
                        + t.getClass());
            }
            jBody.getUnits().add(ass);
            jBody.getUnits().add(Jimple.v().newReturnStmt(l));
        }
    }

}
