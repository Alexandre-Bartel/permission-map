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

package lu.uni;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {
  
  public static Test suite() {

    TestSuite suite = new TestSuite("Test for lu.uni.fpc (Find Permission Checks)");

    suite.addTest(new TestSuite(lu.uni.fpc.MethodSearchTreeTest.class));
    suite.addTest(new TestSuite(lu.uni.fpc.OutputTest.class));
    //suite.addTest(new TestSuite(lu.uni.fpc.SparkFindPermissionChecksTest.class));
    suite.addTest(new TestSuite(lu.uni.fpc.CHAFindPermissionChecksTest.class));
    suite.addTest(new TestSuite(lu.uni.fpc.CHACycleTest.class));
    //suite.addTest(new TestSuite(lu.uni.fpc.TimeOutTest.class));

    return suite;
  }
  
}
