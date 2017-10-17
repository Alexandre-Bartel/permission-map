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


import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import soot.Body;
import soot.BodyTransformer;
import soot.PackManager;
import soot.Scene;
import soot.SceneTransformer;
import soot.SootClass;
import soot.SootMethod;
import soot.Transform;
import soot.Unit;
import soot.jimple.CaughtExceptionRef;
import soot.jimple.DefinitionStmt;
import soot.jimple.GotoStmt;
import soot.jimple.IfStmt;
import soot.jimple.ReturnStmt;
import soot.jimple.ReturnVoidStmt;
import soot.jimple.SwitchStmt;
import soot.jimple.toolkits.annotation.logic.Loop;
import soot.options.Options;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.graph.LoopNestTree;
import soot.toolkits.graph.UnitGraph;


public class MyTransformers {

  /**
   * 
   * @param args
   */
  public static void main(String[] args) {

//		PackManager.v().getPack("jtp").add(new Transform("jtp.myBodyTransformer", new BodyTransformer() {
//
//			protected void internalTransform(Body body, String phase, Map options) {
//				System.out.println("\n\n\ntransformer for method: " + body.getMethod());
//			}
//		}));
		
		PackManager.v().getPack("wjtp").add(new Transform("wjtp.mySceneTransformer", new SceneTransformer() {

			@Override
			protected void internalTransform(String phaseName,
					Map<String, String> options) {
				
				String outputFile = Options.v().output_dir() +"/classes.txt";
				
				System.out.println("dumping methods in file " + outputFile + "...");
				
				File f = new File(outputFile);
				
				try {
					FileWriter fw = new FileWriter(f);

					fw.write("# <method signature> \"method modifiers\" \n");
				
					for (SootClass sc: Scene.v().getApplicationClasses()) {
						for (SootMethod sm: sc.getMethods()) {
							fw.write(sm + " " + sm.getModifiers() + "\n");
						}
					}
					
					fw.close();
				
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} 
				
				System.out.println("done.");
				System.exit(-1);
				
			}
			
		}));

    soot.Main.main(args);
  }

}
