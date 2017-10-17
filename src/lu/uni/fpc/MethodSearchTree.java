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

import java.util.HashMap;
import java.util.Map;

public class MethodSearchTree {

  private Map<String, Map> root = new HashMap<String, Map>();
  
  private String[] split(String s) {
    // string ~ <a.b.c: void name(p1,p2,p3)>
    s = s.replaceAll("<", "").replaceAll(">", "");
    return s.split(" ");
  }
  
  public void addMethod (String s) {
    
    String sp[] = split(s);

    Map<String, Map>current = root;
    Map<String, Map>previous = null;
    String k = null;
    for (int i=0; i<sp.length; i++) {
      k = sp[i];
      if (!current.containsKey(k)) {
        current.put(k, new HashMap<String, Map>());
      }
      previous = current;
      current = current.get(k);
      if (current == null) // we could have "<android.accounts" and "<android.accounts.abc: toto()>"
        break;
    }
    previous.put(k, null);
  }
  
  public boolean hasMethod (String s) {
    String sp[] = split(s);
    Map<String, Map> current = root;
    String k = null;
    for (int i=0; i< sp.length; i++) {      
      if (current == null)
        return true;
      k = sp[i];
      if (current.containsKey(k)) {
        current = current.get(k);
        continue;
      } else {
        return false;  
      }
    }
    if (current == null) {
      return true;
    }
    return false;
  }
  
  public void printTree() {
    printTree(this.root, 0);
  }
  private void printTree(Map<String, Map> n, int depth) {
    for (String k: n.keySet()){ 
      for (int i=0; i<depth; i++)
        System.out.print(" ");
      System.out.println("k: "+ k);
      Map m = n.get(k);
      if (m == null) {
        for (int i=0; i<depth; i++)
          System.out.print(" ");
        System.out.println(" null");
      } else {
        printTree(m, depth+1);
      }
        
    }
  }
  
}
