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

package lu.uni.fpc;

import junit.framework.TestCase;
import lu.uni.fpc.MethodSearchTree;

public class MethodSearchTreeTest extends TestCase {
  
  MethodSearchTree b = null;
  
  protected void setUp() throws Exception {
    super.setUp();   
    b = new MethodSearchTree();
    b.addMethod("<GenerationGG: android.animation.ArgbEvaluator get_androidanimationArgbEvaluator()>");
  }
  
  public void testHasClass() {   
    boolean r = b.hasMethod("<GenerationGG:");
    assertEquals(false, r);
  }
  
  public void testHasReturnValue() {
    boolean r = b.hasMethod("<GenerationGG: android.animation.ArgbEvaluator");
    assertEquals(false, r);
  }
  
  
  public void testHasMethod() {
    boolean r = b.hasMethod("GenerationGG: android.animation.ArgbEvaluator get_androidanimationArgbEvaluator()");
    assertEquals(true, r);
  }
  
  public void testHasClass2() {
    b.addMethod("<toto ");
    boolean r = b.hasMethod("<toto");
    assertEquals(true, r);
  }
  
}
