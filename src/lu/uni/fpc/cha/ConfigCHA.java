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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ConfigCHA extends lu.uni.fpc.Config {
  
  private static ConfigCHA singleton = null;
  
  private boolean forceNaiveCallGraphSearch = false;
  public boolean isForceNaiveCallGraphSearch() { return forceNaiveCallGraphSearch; }
  
  private ConfigCHA(){};
  
  public static ConfigCHA v() {
    if (singleton == null)
      singleton = new ConfigCHA();
    return singleton;
  }
  
  public void loadConfig(String configFilename) throws IOException {

    Properties fpcConfigFile = new Properties();
    InputStream cis = new FileInputStream(new File(configFilename));
    fpcConfigFile.load(cis);
    super.loadConfig(fpcConfigFile); // loads generic options
    
    // loads CHA specific options
    
    // force naive call graph search
    String forceNaive = fpcConfigFile.getProperty("FORCE_NAIVE_SEARCH");
    System.out.println("debug: forcenaiveseach: '"+ forceNaive +"'");
    forceNaiveCallGraphSearch = Boolean.parseBoolean(forceNaive);
    if (forceNaiveCallGraphSearch) {
      System.out.println("[INFO] Forcing Naive Call graph search!!!");
    }
  }


}
