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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ConfigSpark extends lu.uni.fpc.Config {
  
  private static ConfigSpark singleton = null;
  
//  private boolean zeroPermEp = false;
  private boolean zeroPermMethod = false;
  private boolean forceEntryPoints = false;
//  public boolean hasReadZeroPermEp() { return zeroPermEp; }
  public boolean hasReadZeroPermMethods() { return zeroPermMethod; }
  public boolean doForceEntryPoints() { return forceEntryPoints; }
  
  
  private ConfigSpark(){};
  
  public static ConfigSpark v() {
    if (singleton == null)
      singleton = new ConfigSpark();
    return singleton;
  }
  
  public void loadConfig(String configFilename) throws IOException {

    Properties fpcConfigFile = new Properties();
    File configFile = new File(configFilename).getAbsoluteFile();
    InputStream cis = new FileInputStream(configFile);
    fpcConfigFile.load(cis);
    super.loadConfig(fpcConfigFile); // loads generic options
    
    // loads Spark specific options


      if (null != fpcConfigFile.getProperty("TIMEOUT_DEPTH")) {
        int timeoutDepth = Integer.parseInt(fpcConfigFile.getProperty("TIMEOUT_DEPTH")); 
        SparkTimeOut.v().setTimeOutDepth(timeoutDepth);
      }
      if (null != fpcConfigFile.getProperty("TIMEOUT_TIME")) {
        int timeoutTime = Integer.parseInt(fpcConfigFile.getProperty("TIMEOUT_TIME"));
        SparkTimeOut.v().setTimeOutTime(timeoutTime);
      }
      

//      // read entry points with zero permissions (from CHA)
//      if (null == fpcConfigFile.getProperty("NO_PERM_ENTRY_POINTS"))
//        throw new RuntimeException("error: config file is missing NO_PERM_ENTRY_POINTS");
//      String zeroPermEpFilename = fpcConfigFile.getProperty("NO_PERM_ENTRY_POINTS"); 
//      if (!(zeroPermEpFilename.equals(""))) {   
//        this.zeroPermEp = true;
//        File zeroPermEpFile = new File(configFile.getParent(), zeroPermEpFilename);
//        long epnbr = FilterSpark.v().loadZeroPermEntryPoints(zeroPermEpFile.getPath());
//        System.out.println("[INFO] Reading "+ epnbr +" entry point(s) with zero permissions from file '"+ zeroPermEpFile +"'!");
//      } else {
//        System.out.println("[INFO] NOT reading entry points with zero permissions.");
//      }
      
      
      // read entry points to be analyzed. Other ep are skipped.
      if (null == fpcConfigFile.getProperty("FORCE_ENTRY_POINTS"))
        throw new RuntimeException("error: config file is missing FORCE_ENTRY_POINTS");
      String forceEntryPointsFilename = fpcConfigFile.getProperty("FORCE_ENTRY_POINTS"); 
      if (!(forceEntryPointsFilename.equals(""))) {   
        this.forceEntryPoints = true;
        File forceEntryPointsFile = new File(configFile.getParent(), forceEntryPointsFilename);
        long nbr = FilterSpark.v().forceEntryPoints(forceEntryPointsFile.getPath());
        System.out.println("[INFO] Reading "+ nbr +" forced entry point(s) from file '"+ forceEntryPointsFile +"'!");
      } else {
        System.out.println("[INFO] NOT forcing entry points.");
      }
      
      // read methods with zero permissions (from CHA)
      if (null == fpcConfigFile.getProperty("NO_PERM_METHODS"))
        throw new RuntimeException("error: config file is missing NO_PERM_METHODS");
      String zeroPermMethodFilename = fpcConfigFile.getProperty("NO_PERM_METHODS"); 
      if (!(zeroPermMethodFilename.equals(""))) {   
        this.zeroPermMethod = true;
        File zeroPermMethodFile = new File(configFile.getParent(), zeroPermMethodFilename);
        long epnbr = FilterSpark.v().loadZeroPermMethods(zeroPermMethodFile.getPath());
        System.out.println("[INFO] Reading "+ epnbr +" method(s) with zero permissions from file '"+ zeroPermMethodFile +"'!");
      } else {
        System.out.println("[INFO] NOT reading methods with zero permissions.");
      }
    

  }

}