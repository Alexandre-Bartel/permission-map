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

import java.io.IOException;
import java.util.Properties;


public abstract class Config {
  
  boolean skipInverseIdentity = false;
  boolean outputGraph = false;
  String logLevelStr = "";
  int outputGraphMaxDepth = Integer.MAX_VALUE;
  private boolean skipStubProxy = false;
  private boolean printLiveCallGraph = false;
  private boolean skipOnTransact = false;

  public boolean skipInverseIdentity() { return skipInverseIdentity; }
  public boolean doOuputGraph() { return outputGraph; }
  public int getGraphOutMaxDepth() { return outputGraphMaxDepth; }
  public boolean skipStubProxy() { return skipStubProxy; }
  public boolean printLiveCallGraph() { return printLiveCallGraph; }
  public boolean skipOnTransact() { return skipOnTransact; }
  
  
  public void loadConfig(Properties fpcConfigFile) throws IOException {

    if (null == fpcConfigFile.getProperty("SKIP_INVERSE_IDENTITY"))
      throw new RuntimeException("error: config file is missing SKIP_INVERSE_IDENTITY");
    boolean skip = Boolean.parseBoolean(fpcConfigFile.getProperty("SKIP_INVERSE_IDENTITY")); 
    skipInverseIdentity = skip;
    if (skipInverseIdentity)
      System.out.println("[INFO] Skipping Service Identity Inversion!");
    
    if (null == fpcConfigFile.getProperty("OUTPUT_GRAPH"))
      throw new RuntimeException("error: config file is missing OUTPUT_GRAPH");
    outputGraph = Boolean.parseBoolean(fpcConfigFile.getProperty("OUTPUT_GRAPH")); 
    if (outputGraph)
      System.out.println("[INFO] Write graph to file.");
    
    if (null == fpcConfigFile.getProperty("OUTPUT_GRAPH_MAX_DEPTH"))
      throw new RuntimeException("error: config file is missing OUTPUT_GRAPH_MAX_DEPTH");
    outputGraphMaxDepth = Integer.parseInt(fpcConfigFile.getProperty("OUTPUT_GRAPH_MAX_DEPTH")); 
    if (outputGraphMaxDepth < 0)
      outputGraphMaxDepth = Integer.MAX_VALUE;
    if (outputGraph)
      System.out.println("[INFO] Write graph to file: max depth "+ outputGraphMaxDepth);
    
    if (null == fpcConfigFile.getProperty("SKIP_STUB_AND_PROXY"))
      throw new RuntimeException("error: config file is missing SKIP_STUB_AND_PROXY");
    skipStubProxy = Boolean.parseBoolean(fpcConfigFile.getProperty("SKIP_STUB_AND_PROXY")); 
    System.out.println("[INFO] skip $Stub and $Stub$Proxy classes: "+ skipStubProxy);
    
    if (null == fpcConfigFile.getProperty("SKIP_ON_TRANSACT"))
        throw new RuntimeException("error: config file is missing SKIP_ON_TRANSACT");
      skipOnTransact = Boolean.parseBoolean(fpcConfigFile.getProperty("SKIP_ON_TRANSACT")); 
      System.out.println("[INFO] skip onTransact() methods: "+ skipOnTransact);
   
 
    if (null == fpcConfigFile.getProperty("PRINT_LIVE_CALL_GRAPH"))
      throw new RuntimeException("error: config file is missing PRINT_LIVE_CALL_GRAPH");
    printLiveCallGraph = Boolean.parseBoolean(fpcConfigFile.getProperty("PRINT_LIVE_CALL_GRAPH")); 
    System.out.println("[INFO] print live call graph: "+ printLiveCallGraph);
    
//    // force entry point list
//    String forceEp = fpcConfigFile.getProperty("FORCE_ENTRY_POINTS");
//    if (forceEp != null) {
//      System.out.println("[w] Forcing entry points from file '"+ forceEp +"'!");
//      FilterSpark.forceEntryPoints(forceEp);
//    }
  }

}
