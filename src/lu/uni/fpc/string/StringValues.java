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

package lu.uni.fpc.string;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import soot.SootMethod;
import soot.Unit;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.FlowSet;

/**
  * Find and propagate all strings assignments
  *
  * @author Alexandre Bartel
  **/
public class StringValues  {

    protected final Map<Unit, List> unitToStringAnalysis;

    public StringValues(UnitGraph graph, Stack<SootMethod> stack)
    {

        StringAnalysis analysis = new StringAnalysis(graph, stack);

        // build map
        {
            unitToStringAnalysis = new HashMap<Unit, List>(graph.size() * 2 + 1, 0.7f);
            Iterator unitIt = graph.iterator();

			while (unitIt.hasNext()) {
				Unit s = (Unit) unitIt.next();
				FlowSet setB = (FlowSet) analysis.getFlowBefore(s);
				FlowSet setA = (FlowSet) analysis.getFlowAfter(s);
				List listB = setB.toList();
				List listA = setA.toList();
				List unionList = new ArrayList();
				unionList.addAll(listA);
				unionList.addAll(listB);
				unitToStringAnalysis.put(s,Collections.unmodifiableList((unionList)));
			}
        }
    }

    /**
     * Returns a list of locals guaranteed to be defined at (just
     * before) program point <tt>s</tt>.
     **/
    public List getStringAnalysis(Unit s)
    {
				for (Unit k: unitToStringAnalysis.keySet()) {
					System.out.println("key :"+ k +"  val: "+ unitToStringAnalysis.get(k));
				}
        return unitToStringAnalysis.get(s);
    }
}



