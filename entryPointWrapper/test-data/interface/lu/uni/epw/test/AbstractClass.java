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

package lu.uni.epw.test;

public abstract class AbstractClass implements Interface {

  public AbstractClass(int a, int b) {
  }

  public abstract int abst1(); 

	public int conc1() {
		return 0;
	}

	public int conc2(int a, int b) {
		return a + b;
	}

	private int conc3(int a, int b) {
		return a * b;
	}

  // interface stuff

  public int interfaceMethod1(int a, AbstractClass ac) {
    return 0;
  }

  public void interfaceMethod2(Interface i) {
    System.out.println("toto");
  }

}
