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
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lu.uni.rrc.services.GenerateServiceInit;
import soot.Body;
import soot.Scene;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.FieldRef;
import soot.jimple.Jimple;

public class HandleContext {
	
	private static final Logger logger = LoggerFactory.getLogger(HandleContext.class);

    Set<SootMethod> interest = null;
    public HandleContext(Set<SootMethod> interest) {
        this.interest = interest;
    }

    public void doWork() {
        for (SootMethod sm : interest) {
            if (sm.getDeclaringClass().toString().equals(GenerateServiceInit.servicesInitClassName)) {
                continue;
            }
            Body b = sm.retrieveActiveBody();
            List<Unit> unitsToRedirect = new ArrayList<Unit>();
            for (Unit u : b.getUnits()) {
                if (u instanceof AssignStmt) {
                    AssignStmt ass = (AssignStmt) u;
                    if (ass.getRightOp() instanceof FieldRef) {
                        FieldRef fr = (FieldRef) ass.getRightOp();
                        if (fr.getType().toString().equals("android.content.Context")) {
                            unitsToRedirect.add(u);
                        }
                    }
                }
            }
            for (Unit u : unitsToRedirect) {
                redirectToContext(b, u);
            }
        }
    }

    private void redirectToContext(Body b, Unit u) {

        SootClass servicesInitClass = Scene.v().getSootClass(
                GenerateServiceInit.servicesInitClassName);
        SootField sf = servicesInitClass.getFieldByName("androidcontentContext");

        Value newR = Jimple.v().newStaticFieldRef(sf.makeRef());

        logger.debug("replaced context '" + u + " by ");
        AssignStmt ass = (AssignStmt) u;
        ass.setRightOp(newR);
        logger.debug(" " + u);

    }

}
