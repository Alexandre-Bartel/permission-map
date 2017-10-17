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

package lu.uni.rrc;

import java.util.HashSet;
import java.util.Set;

public abstract class HandleC {

    protected Set<String> init = new HashSet<String>();
    protected Set<String> called = new HashSet<String>();
    protected Set<String> sscalled = new HashSet<String>();

    public abstract void doWork();

    public void checkAllCalledAreInit(String name) {
        for (String c : called) {
            System.out.print("< " + name + " " + c + "> ");
            if (init.contains(c)) {
                System.out.println(" is init!");
            } else {
                System.out.println(" is NOT init!!!");
            }
        }
    }

}
