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

package lu.uni.fpc.spark;

// Statistics for Spark
public class SStats {

  private static SStats instance = null;
  
  private SStats() {};
  public static SStats v() {
    if (instance == null)
      instance = new SStats();
    return instance;
  }
  
  
  private long timeouts = 0;
  private long entrypoint = 0;
//  private long skipentrypoint = 0;
//  private long forceentrypoint = 0;
  private long timeoutresets = 0;
  private long avoidCycle = 0;
  private long alreadycomputedep = 0;
  private long mustSkipMethod = 0;
  private long notEntryPointWrapper = 0;
  private long notEntryPointMethod = 0;
  private long timedOutEp = 0;
  private long zeroPermMethod = 0;
  
  public void addTimeout() { timeouts++;}
  public void addEntryPoint() { entrypoint++;}
//  public void addSkipEntryPoint() { skipentrypoint++;}
//  public void addForceEntryPoint() { forceentrypoint++;}
  public void addTimeoutReset() { timeoutresets++;}
  public void addAvoidCycle() { avoidCycle++;}
  public void addAlreadyComputedEp() { alreadycomputedep++; }
  public void addMustSkipMethod() { mustSkipMethod++;}
  public void addNotEntryPointWrapper() { notEntryPointWrapper++; }
  public void addNotEntryPointMethod() { notEntryPointMethod++;}
  public void addTimedOutEp() { timedOutEp++;}
  public void addZeroPermMethod() { zeroPermMethod++;}
  
  public String toString() {
    String s = "\n";
    s += "Spark Stats:\n";
    s += "timeouts:             "+ timeouts +"\n";
    s += "timeout resets:       "+ timeoutresets +"\n";
    s += "entry points:         "+ entrypoint +"\n";
    s += "timed out ep:         "+ timedOutEp +"\n";
//    s += "skipentrypoint:       "+ skipentrypoint +"\n";
//    s += "forceentrypoint:      "+ forceentrypoint +"\n";
    s += "alreadycomputedep:    "+ alreadycomputedep +"\n";
    s += "avoidcycle:           "+ avoidCycle +"\n";
    s += "zeroPermMethod:       "+ zeroPermMethod + "\n";
    s += "mustskipmethod:       "+ mustSkipMethod +"\n";
    s += "notEntryPointWrapper: "+ notEntryPointWrapper +"\n";
    s += "notEntryPointMethod:  "+ notEntryPointMethod +"\n";
    s += "\n";
    return s;
  }

  
}
