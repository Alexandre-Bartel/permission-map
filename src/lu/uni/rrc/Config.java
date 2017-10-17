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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Config {

  private static boolean doServices = false;
  private static boolean doIntents = false;
  private static boolean doContentProviders = false;
  
  public void loadConfig(String cfgFilePath) throws IOException {
    Properties cfg = new Properties();
    InputStream cis = new FileInputStream(new File(cfgFilePath));
    cfg.load(cis);
    
    if (null == cfg.getProperty("SERVICES"))
      throw new RuntimeException("error: config file is missing SERVICES");
    doServices = Boolean.parseBoolean(cfg.getProperty("SERVICES")); 
    if (doServices)
      System.out.println("[INFO] Handling services.");
    
    if (null == cfg.getProperty("INTENTS"))
      throw new RuntimeException("error: config file is missing INTENTS");
    doIntents = Boolean.parseBoolean(cfg.getProperty("INTENTS")); 
    if (doIntents)
      System.out.println("[INFO] Handling intents.");
    
    if (null == cfg.getProperty("CONTENT_PROVIDERS"))
      throw new RuntimeException("error: config file is missing CONTENT_PROVIDERS");
    doContentProviders = Boolean.parseBoolean(cfg.getProperty("CONTENT_PROVIDERS")); 
    if (doContentProviders)
      System.out.println("[INFO] Handling content providers.");
    
  }

  public static boolean doRedirectService() {
    return doServices;
  }

  public static boolean doRedirectContentProviders() {
    return doContentProviders;
  }

  public static boolean doRedirectIntents() {
    return doIntents;
  }

}
