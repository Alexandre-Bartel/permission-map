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

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import soot.Body;
import soot.Scene;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.Stmt;
import soot.jimple.StringConstant;
import soot.jimple.internal.JAssignStmt;
import soot.jimple.internal.JIdentityStmt;
import soot.jimple.internal.JInvokeStmt;
import soot.toolkits.graph.DominatorsFinder;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.graph.MHGDominatorsFinder;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.ArraySparseSet;
import soot.toolkits.scalar.FlowSet;
import soot.toolkits.scalar.ForwardFlowAnalysis;




/**
 * Flow analysis to determine all locals guaranteed to be defined at a
 * given program point.
 **/
class StringAnalysis extends ForwardFlowAnalysis
{
    FlowSet emptySet = new ArraySparseSet();
    Map<Unit, FlowSet> unitToGenerateSet;

    StringAnalysis(UnitGraph graph, Stack<SootMethod> stack)
    {
        super(graph);
        DominatorsFinder df = new MHGDominatorsFinder(graph);
        unitToGenerateSet = new HashMap<Unit, FlowSet>(graph.size() * 2 + 1, 0.7f);

        // pre-compute generate sets
        for(Iterator unitIt = graph.iterator(); unitIt.hasNext();){
            Unit s = (Unit) unitIt.next();
            FlowSet genSet = emptySet.clone();
            
            /*for(Iterator domsIt = df.getDominators(s).iterator(); domsIt.hasNext();){
                Unit dom = (Unit) domsIt.next();
                for(Iterator boxIt = dom.getDefBoxes().iterator(); boxIt.hasNext();){
                    ValueBox box = (ValueBox) boxIt.next();
                    if(box.getValue() instanceof Local)
                        genSet.add(box.getValue(), genSet);
										System.out.println("boxval : "+ box.getValue().getClass());
                }
            }*/
						System.out.println("s: "+s);
						if (s instanceof JInvokeStmt) {
							System.out.println("o: jassignstmt");
							List<Value> vList = ((JInvokeStmt)s).getInvokeExpr().getArgs();	
							for (Value v: vList) {
								if (v instanceof StringConstant) {
									System.out.println("oho: strconstant in invoke param! "+ v);
									genSet.add (v, genSet);
								}
							}
						} else if (s instanceof JAssignStmt) {
							System.out.println("o: jassignstmt");
							Value vl = ((AssignStmt)s).getLeftOp();          
							Value vr = ((AssignStmt)s).getRightOp();
							if (vr instanceof StringConstant) {
								System.out.println("o: strconstant");
								genSet.add (vr, genSet);
							} /*else if ( vr instanceof StaticFieldRef) {
								StaticFieldRef ref = (StaticFieldRef)vr;
								Type refType = ref.getType();
								System.out.println("o: not strconstant, but '"+ vr.getClass());
								System.out.println(" ref type: "+ refType);
								System.out.println(" ref="+ ref);
							}*/
						} else if (s instanceof JIdentityStmt) {
							System.out.println("o: jidentitystmt");
							Value vl = ((JIdentityStmt)s).getLeftOp();          
							Value vr = ((JIdentityStmt)s).getRightOp();
							if (vr.toString().equals("@parameter0: java.lang.String")) {
								System.out.println("type: "+ vr.getClass());
								System.out.println("oho: java.lang.String");
								String thisMethod = stack.elementAt(stack.size()-1).toString();
								String previousMethod = stack.elementAt(stack.size()-2).toString();
							
								SootMethod thisSM = Scene.v().getMethod(thisMethod);
								SootMethod prevSM = Scene.v().getMethod(previousMethod);

								System.out.println("thisSM: "+ thisSM);
								System.out.println("prevSM: "+ prevSM);
								if (prevSM.hasActiveBody()) {
								
									Body b = prevSM.getActiveBody();
									UnitGraph ug = new ExceptionalUnitGraph( b ); 
									Unit targetU = null;
									for( Unit u: b.getUnits()) {
										Stmt s2 = (Stmt)u;
										if (s2.containsInvokeExpr()) {
											SootMethod invkSM = s2.getInvokeExpr().getMethod();
											if (invkSM.toString().equals(thisSM.toString())) {
												targetU = u;
												System.out.println("unit target: "+ targetU);
												break;
											}
										}
									}
									if (targetU != null && (thisSM.toString().contains("validate") || thisSM.toString().contains("check"))) {
										Stack<SootMethod> popedStack = (Stack<SootMethod>)stack.clone();
										popedStack.pop();
										System.out.println(" [[[[ -> start StringValues on "+ prevSM);
										StringValues sv = new StringValues (ug, popedStack);
										List l = sv.getStringAnalysis (targetU);
										for (StringConstant scst :(List<StringConstant>)l) {
											System.out.println(" [[[[ -> add "+ scst);
											genSet.add (scst, genSet);
										}
										System.out.println(" [[[[ -> end StringValues on "+ prevSM);
									}
								} // if has active body

							} else if (vr instanceof StringConstant) {
								System.out.println("oho: strconstant"+ vr);
							} else {
								System.out.println("oho: not strconstant"+ vr);
							}

						}

            
            unitToGenerateSet.put(s, genSet);
        }

        doAnalysis();
    }

    /**
     * All INs are initialized to the empty set.
     **/
    protected Object newInitialFlow()
    {
        return emptySet.clone();
    }

    /**
     * IN(Start) is the empty set
     **/
    protected Object entryInitialFlow()
    {
        return emptySet.clone();
    }

    /**
     * OUT is the same as IN plus the genSet.
     **/
    protected void flowThrough(Object inValue, Object unit, Object outValue)
    {
        FlowSet in = (FlowSet) inValue;
        FlowSet out = (FlowSet) outValue;

        // perform generation (kill set is empty)
        in.union(unitToGenerateSet.get(unit), out);
    }

    /**
     * All paths == Intersection.
     **/
    protected void merge(Object in1, Object in2, Object out)
    {
        FlowSet inSet1 = (FlowSet) in1;
        FlowSet inSet2 = (FlowSet) in2;
        FlowSet outSet = (FlowSet) out;

				//for (Object o: inSet1.toList()) { System.out.println("is1: "+ o); }
				//for (Object o: inSet2.toList()) { System.out.println("is2: "+ o); }
        inSet1.union (inSet2, outSet);
				//for (Object o: outSet.toList()) { System.out.println("os : "+ o); }
    }

    protected void copy(Object source, Object dest)
    {
        FlowSet sourceSet = (FlowSet) source;
        FlowSet destSet = (FlowSet) dest;

        sourceSet.copy(destSet);
    }
}
