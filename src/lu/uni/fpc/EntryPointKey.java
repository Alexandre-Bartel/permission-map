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

import soot.SootMethod;


public class EntryPointKey {

  private SootMethod wrapper;
  private SootMethod ep;
  private String wrapper_string;
  private String ep_string;
  
  public EntryPointKey (SootMethod wrapper, SootMethod ep) {
    this.wrapper = wrapper;
    this.ep = ep;
    this.wrapper_string = wrapper.toString();
    this.ep_string = ep.toString();
  }
  
  public EntryPointKey (String wrapper, String ep) {
    checkMethodString(wrapper);
    checkMethodString(ep);
    this.wrapper_string = wrapper;
    this.ep_string = ep;
  }
  
  private void checkMethodString(String m) {
    if (!(m.startsWith("<") && m.endsWith(">")))
      throw new RuntimeException("error: entry point key method string not valid: "+ m);
  }
  
  @Override
  public int hashCode() {
    return wrapper_string.hashCode() + 31*ep_string.hashCode();
  }

  @Override
  public boolean equals(Object o) {
    if (o==this) return true;
    if (o==null || !(o instanceof EntryPointKey)) return false;
    EntryPointKey epk = EntryPointKey.class.cast(o);
    return wrapper_string.equals(epk.wrapper_string) && ep_string.equals(epk.ep_string);
  }
  
  @Override
  public String toString() {
    return wrapper_string +" -> "+ ep_string;
  }

  public SootMethod getWrapper() {
    return wrapper;
  }
  
  public String getWrapperString() {
    return wrapper_string;
  }

  public void setWrapper(SootMethod wrapper) {
    this.wrapper = wrapper;
  }

  public SootMethod getEp() {
    return ep;
  }
  
  public String getEpString() {
    return ep_string;
  }

  public void setEp(SootMethod ep) {
    this.ep = ep;
  }

}
