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

package lu.uni.fpc.string;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public class AnalysisType {
  
  private static Logger logger = LoggerFactory.getLogger(lu.uni.fpc.string.AnalysisType.class);
  
  private static AnalysisType instance = null;
  
  private List<PAnalysis> analyses = new ArrayList<PAnalysis>();
  PAnalysis currentPA = null;
  
  public PAnalysis cur() {
    if (currentPA == null)
      throw new RuntimeException("error: current is 'null' did you forget to call the start() method?");
    return currentPA;
  }
  
  public class PAnalysis {
    
    private int case1_direct_string_passed_as_parameter = 0;
    private int case2_string_after_direct_assignment = 0;
    private int case3_string_after_flow_analysis = 0;
    private int case4_string_in_array = 0;
    private int case5_uri_read_permission = 0;
    private int case6_uri_write_permission = 0;
    private int case7_read_from_parcel = 0;
    private int case8_string_from_generationGG = 0;
    private int case_strange = 0;
    private int nbrPermissions = 0;
    
    public int getCase1_direct_string_passed_as_parameter() {
      return case1_direct_string_passed_as_parameter;
    }
    public void case1_direct_string_passed_as_parameter() {
      this.case1_direct_string_passed_as_parameter++;
    }
    public int getCase2_string_after_direct_assignment() {
      return case2_string_after_direct_assignment;
    }
    public void case2_string_after_direct_assignment() {
      this.case2_string_after_direct_assignment++;
    }
    public int getCase3_string_after_flow_analysis() {
      return case3_string_after_flow_analysis;
    }
    public void case3_string_after_flow_analysis() {
      this.case3_string_after_flow_analysis++;
    }
    public int getCase4_string_in_array() {
      return case4_string_in_array;
    }
    public void case4_string_in_array() {
      this.case4_string_in_array++;
    }
    public int getCase5_uri_read_permission() {
      return case5_uri_read_permission;
    }
    public void case5_uri_read_permission() {
      this.case5_uri_read_permission++;
    }
    public int getCase6_uri_write_permission() {
      return case6_uri_write_permission;
    }
    public void case6_uri_write_permission() {
      this.case6_uri_write_permission++;
    }
    public int getCase7_read_from_parcel() {
      return case7_read_from_parcel;
    }
    public void case7_read_from_parcel() {
      this.case7_read_from_parcel++;
    }
    public int getCase8_string_from_generationGG() {
      return case8_string_from_generationGG;
    }
    public void case8_string_from_generationGG() {
      this.case8_string_from_generationGG++;
    }
    public int getCase_strange() {
      return case_strange;
    }
    public void case_strange() {
      this.case_strange++;
    }
    public int getNbrPermissions() {
      return nbrPermissions;
    }
    public void setNbrPermissions(int n) {
      this.nbrPermissions = n;
    }   
    public String toString() {
      String s = "";
      s += ("1. Direct String                 : " + case1_direct_string_passed_as_parameter );
      s += ("2. String after direct assignment: " + case2_string_after_direct_assignment );
      s += ("3. String after flow analysis    : " + case3_string_after_flow_analysis );
      s += ("4. String is in array            : " + case4_string_in_array );
      s += ("5. Uri read permission           : " + case5_uri_read_permission );
      s += ("6. Uri write permission          : " + case6_uri_write_permission );
      s += ("7. Read Permission from parcel   : " + case7_read_from_parcel );
      s += ("8. String from GenerationGG      : " + case8_string_from_generationGG );
      s += (" . Strange cases (see logs)      : " + case_strange);
      s += (" . Permission #                  : " + nbrPermissions );
      return s;
    }
  }
  
  private static Map<Integer, Integer> nbrPermsPerCheck = new HashMap<Integer, Integer>();

  public void printLogValue() {
    // reset
    int direct = 0;
    int flow = 0;
    int array = 0;
    int uriread = 0;
    int uriwrite = 0;
    int readfromparcel = 0;
    int generationgg = 0;
    int strange = 0;
    
    // compute
    for (PAnalysis pa: analyses) {
      // # permissions per analysis
      int mpSetSize = pa.getNbrPermissions();
      if (nbrPermsPerCheck.containsKey(mpSetSize)) {
        nbrPermsPerCheck.put(mpSetSize, new Integer(nbrPermsPerCheck.get(mpSetSize).intValue() + 1));
      } else {
        nbrPermsPerCheck.put(mpSetSize, new Integer(1));
      }
      // only direct string
      if (pa.getCase1_direct_string_passed_as_parameter() > 0 
          && pa.getCase2_string_after_direct_assignment() == 0
          && pa.getCase3_string_after_flow_analysis() == 0
          && pa.getCase4_string_in_array() == 0
          && pa.getCase5_uri_read_permission() == 0
          && pa.getCase6_uri_write_permission() == 0
          && pa.getCase7_read_from_parcel() == 0
          && pa.getCase8_string_from_generationGG() ==0) {
        direct++;
      }
      // has flow analysis
      if (pa.getCase3_string_after_flow_analysis() > 0) {
        flow++;
      }
      // has array
      if (pa.getCase4_string_in_array() > 0) {
        array++;
      }
      // uri
      if (pa.getCase5_uri_read_permission() > 0) {
        uriread++;
      }
      if (pa.getCase6_uri_write_permission() > 0) {
        uriwrite++;
      }
      // parcel
      if (pa.getCase7_read_from_parcel() > 0) {
        readfromparcel++;
      }
      // GenerationGG
      if (pa.getCase8_string_from_generationGG() > 0) {
        generationgg++;
      }
      // strange
      if (pa.getCase_strange() > 0) {
        strange++;
      }
    }
    
    // print
    System.out.println("");
    System.out.println("Total # analyses                   : "+ analyses.size());
    for (Integer k: nbrPermsPerCheck.keySet()) {
      System.out.println("|-- checks with "+ k +" permissions      : "+ nbrPermsPerCheck.get(k));
    }
    System.out.println("|-- checks with only direct strings: "+ direct);
    System.out.println("|-- checks with flow analysis      : "+ flow);
    System.out.println("|-- checks with strings in array   : "+ array);
    System.out.println("|-- checks with uri read perm.     : "+ uriread);
    System.out.println("|-- checks with uri write perm.    : "+ uriwrite);
    System.out.println("|-- checks with read from parcel   : "+ readfromparcel);
    System.out.println("|-- checks with GenerationGG       : "+ generationgg);
    System.out.println("|-- stronge check(s) (see logs)    : "+ strange);
    System.out.println("");
    
  }
  
  // Singleton
  private AnalysisType() {
  } 
  public static AnalysisType v() {
    if (instance == null)
      instance = new AnalysisType();
    return instance;
  }
  
  public void start() {
    PAnalysis pa = new PAnalysis();
    analyses.add(pa);
    currentPA = pa;
  }
  
  public void end() {
    currentPA = null;
  }

  
}
