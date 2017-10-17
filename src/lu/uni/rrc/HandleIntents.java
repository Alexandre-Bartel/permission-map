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

import soot.Body;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;

public class HandleIntents {
	
	private static final Logger logger = LoggerFactory.getLogger(HandleIntents.class);

    public static String bindService = "<android.content.Context: void bindService";
    public static String startService = "<android.content.Context: android.content.ComponentName startService";
    public static String startActivity = "<android.content.Context: void startActivity";
    public static String startActivities = "<android.content.Context: void startActivities";
    // public static String sendBroadcast =
    // "<android.content.Context: void sendBroadcast";
    // public static String sendOrdered =
    // "<android.content.Context: void sendOrdered";
    // public static String sendSticky =
    // "<android.content.Context: void sendSticky";
    public static String startIntentSender = "<android.content.Context: void startIntentSender"; // API
    // 16
    public static List<String> sigList = new ArrayList<String>();
    private List<Set<SootMethod>> sList = new ArrayList<Set<SootMethod>>();
    {
        sigList.add(bindService);
        sigList.add(startService);
        sigList.add(startActivity);
        sigList.add(startActivities);
        // sigList.add(sendBroadcast);
        // sigList.add(sendOrdered);
        // sigList.add(sendSticky);
        sigList.add(startIntentSender);

        for (String s : sigList) {
            sList.add(new HashSet<SootMethod>());
        }
    }

    public HandleIntents(List<Set<SootMethod>> l) {

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
                logger.debug("intent - : " + sm + "\n" + b);
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
            // doWorkWithUnit(b, u);
            logger.debug("removing unit: " + u);
            Util.removeUnit(b, u);
        }
    }
}
