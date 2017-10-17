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

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Stack;





import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import soot.Body;
import soot.Local;
import soot.Unit;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.LocalDefs;
import soot.toolkits.scalar.LocalUses;
import soot.toolkits.scalar.SimpleLiveLocals;
import soot.toolkits.scalar.SimpleLocalUses;
import soot.toolkits.scalar.SmartLocalDefs;
import soot.toolkits.scalar.UnitValueBoxPair;

public class LocalDefinition {

  private static Logger logger = LoggerFactory.getLogger(lu.uni.fpc.string.LocalDefinition.class);

  private LocalDefs localDefs = null;
  private LocalUses localUses = null;
  private Body body = null;
  private UnitGraph g = null;

  public LocalDefinition(Body b) {
    this.body = b;
    this.g = new ExceptionalUnitGraph(body);
    this.localDefs = new SmartLocalDefs(g, new SimpleLiveLocals(g));
    this.localUses = new SimpleLocalUses(g, localDefs);
  }

  /**
   * Collect definitions of l in body including the definitions of aliases of l.
   *                                                                                                                                                                                           
   * In this context an alias is a local that propagates its value to l.
   *
   * @param l the local whose definitions are to collect
   * @param localDefs the LocalDefs object
   * @param body the body that contains the local
   */
  protected List<Unit> collectDefinitionsWithAliases(Local l) {
    Set<Local> seenLocals = new HashSet<Local>();
    Stack<Local> newLocals = new Stack<Local>();
    List<Unit> defs = new LinkedList<Unit>();
    newLocals.push(l);

    while (!newLocals.empty()) {
      Local local = newLocals.pop();
      logger.debug("[null local] "+ local);
      if (seenLocals.contains(local))
        continue;
      for (Unit u : collectDefinitions(local)) {
        if (u instanceof AssignStmt) {
          Value r = (Value) ((AssignStmt) u).getRightOp();
          if (r instanceof Local && ! seenLocals.contains((Local) r)) 
            newLocals.push((Local) r); 
        }   
        defs.add(u);
        //  
        @SuppressWarnings("unchecked")
        List<UnitValueBoxPair> usesOf = (List<UnitValueBoxPair>) localUses.getUsesOf(u);
        for (UnitValueBoxPair pair : usesOf) {
          Unit unit = pair.getUnit();
          if (unit instanceof AssignStmt) {
            Value right = (Value) ((AssignStmt) unit).getRightOp();
            Value left = (Value) ((AssignStmt) unit).getLeftOp();
            if (right == local  && left instanceof Local && ! seenLocals.contains((Local) left))
              newLocals.push((Local) left);
          }   
        }   
        //  
      }   
      seenLocals.add(local);
    }   
    return defs;
  }

  /** 
   * Convenience method that collects all definitions of l.
   *
   * @param l the local whose definitions are to collect
   * @param localDefs the LocalDefs object
   * @param body the body that contains the local
   */
  private List<Unit> collectDefinitions(Local l) {
    List <Unit> defs = new LinkedList<Unit>();
    for (Unit u : body.getUnits()) {
      List<Unit> defsOf = localDefs.getDefsOfAt(l, u);
      if (defsOf != null)
        defs.addAll(defsOf);
    }
    for (Unit u: defs) {
      logger.debug("[add def] "+ u);
    }
    return defs;
  }

}
