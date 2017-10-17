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

package lu.uni.rrc.contentprovider;

import java.util.List;
import java.util.Set;

import lu.uni.rrc.Util;
import soot.Body;
import soot.PatchingChain;
import soot.Type;
import soot.Unit;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;

public class RedirectContentProvider {

  public void locateAndRedirectContentProvider(Body b) {
    PatchingChain<Unit> units = b.getUnits();
    for (Unit u: units) {
      Stmt s = (Stmt)u;
      if (!s.containsInvokeExpr())
        continue;
      InvokeExpr ie = s.getInvokeExpr();
      String name = ie.getMethodRef().toString().split(": ")[0].replaceAll("<", "");
      String methodSig = ie.getMethod().toString();
//      if (name.equals("android.content.ContentResolver"))
//        continue;
      if (methodSig.startsWith("<android.content.")) {
        System.out.println("skipping cp redirection in "+ methodSig);
        continue;
      }
      int paramNbr = -1;
      for (Type t: (List<Type>)ie.getMethodRef().parameterTypes()) {
        paramNbr ++;
        if (!t.toString().equals("android.net.Uri"))
          continue;
        String uri = Util.getUriString(b, ie.getArg(paramNbr));
        System.out.println("should redirect method "+ ie +" with URI: "+ uri);
        System.out.println("body from method: "+ b.getMethod());
        System.out.println("body: "+ b);
        Set<String> uris = UriStringValues.findStringForUriAt(b, u);
        for (String str: uris){
          System.out.println("found uri:"+ str);
        }
        break;        
      }
    }
  }


  public void redirectContentProvider(Body b, List<Unit> getContentProvider) {
    
    // First step: compute URI
    for (Unit u: getContentProvider) {
      
    }
    
    // Second step: redirect to static content provider
    
  }

}
