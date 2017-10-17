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

package lu.uni.fpc.graph;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lu.uni.fpc.cha.ConfigCHA;



import soot.Scene;
import soot.SootMethod;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.options.Options;


/**
 * Output a CallGraph as an XML file readable by the Gephi software
 * https://gephi.org/
 * @author alex
 *
 */
public class OutputGraph {
  
  static Logger log = LoggerFactory.getLogger(OutputGraph.class);

  private CallGraph cg = null;
  private List<PrintWriter> out = new ArrayList<PrintWriter>();
  
  
  public OutputGraph(CallGraph cg) {
    this.cg = cg;
  }
  
  /**
   * Generate file representing the graph as comma separated edges.
   * @param outputName
   * @throws IOException 
   */
  public void generateCSV(String outputName) throws IOException {
    // create directory to write graphs
    File graphDir = new File(Options.v().output_dir() +"/"+ "graphs/");
    if (!graphDir.exists())
      graphDir.mkdir();
    for (int i=0; i < ConfigCHA.v().getGraphOutMaxDepth(); i++) {
      String name =  graphDir.getAbsolutePath() +"/"+ i + outputName;
      out.add(new PrintWriter(new BufferedWriter(new FileWriter(name))));
      log.info("Output CSV graph to '"+ name);
    }
    Set<SootMethod> alreadyVisited = new HashSet<SootMethod>();
    SootMethod main = Scene.v().getMainMethod();
    alreadyVisited.add(main);
    visit(alreadyVisited, main, 0);
    
    for (int i=0; i < ConfigCHA.v().getGraphOutMaxDepth(); i++)
      out.get(i).close();
    
  }
  
  private void visit(Set<SootMethod> alreadyVisited, SootMethod current, int depth) {
    if (depth >= ConfigCHA.v().getGraphOutMaxDepth())
      return;
    Iterator<Edge> it = cg.edgesOutOf(current);
    while (it.hasNext()) {
      Edge e = it.next();
      SootMethod tgt = e.tgt();
      for (int i=0; i < ConfigCHA.v().getGraphOutMaxDepth(); i++) {
        if (depth <= i)
          out.get(i).println(current+";"+tgt);
      }
      if (alreadyVisited.contains(tgt))
        continue;
      alreadyVisited.add(tgt);
      visit(alreadyVisited, tgt, depth+1);
    }
  }
  
}
