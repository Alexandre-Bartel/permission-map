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

import soot.Body;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;

public class HandleMessages {
	
	private static final Logger logger = LoggerFactory.getLogger(HandleMessages.class);
    
    public static String post = "<android.os.Handler: boolean post";
    public static String sendEmpty = "<android.os.Handler: boolean sendEmptyMessage";
    public static String sendMessage = "<android.os.Handler: boolean sendMessage";

    private Set<SootMethod> mPost;
    private Set<SootMethod> mSendEmpty;
    private Set<SootMethod> mSendMessage;

    public HandleMessages(Set<SootMethod> mPost, Set<SootMethod> mSendEmpty,
            Set<SootMethod> mSendMessage) {
        this.mPost = mPost;
        this.mSendEmpty = mSendEmpty;
        this.mSendMessage = mSendMessage;
    }

    public void doWork() {
        for (SootMethod sm : mPost) {
            Body b = sm.retrieveActiveBody();
            logger.debug("start message - (post): ", sm, "\n", b);
            replace(b, post);

        }

        for (SootMethod sm : mSendEmpty) {
            Body b = sm.retrieveActiveBody();
            logger.debug("start message - (sendEmpty): ", sm, "\n", b);
            replace(b, sendEmpty);
        }

        for (SootMethod sm : mSendMessage) {
            Body b = sm.retrieveActiveBody();
            logger.debug("start message - (sendMessage): ", sm, "\n", b);
            replace(b, sendMessage);
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
            logger.debug("removing unit: ", u);
            Util.removeUnit(b, u);
        }
    }
}
