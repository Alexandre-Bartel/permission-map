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

package lu.uni.fpc.cha;

import soot.SootMethod;


public class CycleKey {
  private SootMethod bottomMethod;
  private int depth; // the cycle must be resolved at a higher depth
  
  public SootMethod bottomMethod() {
    return bottomMethod;
  }

  public int depth() {
    return depth;
  }

  public CycleKey (SootMethod bottomMethod, int depth) {
    this.bottomMethod = bottomMethod;
    this.depth = depth;
  }
  
  @Override
  public int hashCode() {
    return bottomMethod.toString().hashCode();
  }

  @Override
  public boolean equals(Object o) {
    if (o==this) return true;
    if (o==null || !(o instanceof CycleKey)) return false;
    CycleKey c = CycleKey.class.cast(o);
    return this.bottomMethod.toString().equals(c.bottomMethod().toString());// && this.depth == c.depth;
  }
  
  @Override
  public String toString() {
    return "d "+ depth +" "+ bottomMethod.toString();
  }

  
}
