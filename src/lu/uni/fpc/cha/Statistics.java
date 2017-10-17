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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lu.uni.fpc.EntryPointKey;
import lu.uni.fpc.string.CheckForPermission;
import soot.SootMethod;

public class Statistics {

  private static Logger logger = LoggerFactory.getLogger(Statistics.class);
  
  public static void entryPointsStats(Map<EntryPointKey, Set<String>> entryPoints2PermissionsMap) {
    // compute and print some stat for Entry Point
    logger.info("");
    logger.info("*************************");
    logger.info("Some Entry Point stats...");
    logger.info("*************************");
    Map<Integer, HashSet<EntryPointKey>> nbrPermissions2nbrEP = new HashMap<Integer, HashSet<EntryPointKey>>();
    for (EntryPointKey epk : entryPoints2PermissionsMap.keySet()) {
      Integer nbrPermissions = Integer.valueOf(entryPoints2PermissionsMap.get(epk).size());
      //logger.info("ep:" + epk + ":"+ nbrPermissions + " permissions");
      if (nbrPermissions2nbrEP.containsKey(nbrPermissions)) {
        nbrPermissions2nbrEP.get(nbrPermissions).add(epk);
      } else {
        HashSet<EntryPointKey> mSet = new HashSet<EntryPointKey>();
        mSet.add(epk);
        nbrPermissions2nbrEP.put(nbrPermissions, mSet);
      }
    }

    logger.info("> Total number of entry point methods: "+ entryPoints2PermissionsMap.keySet().size());
    logger.info("> Number of entry point methods for each permission set size:");
    for (Integer pnbr : nbrPermissions2nbrEP.keySet()) {
      logger.info("with " + pnbr
          + " permissions: "
          + nbrPermissions2nbrEP.get(pnbr).size());
    }
    logger.info("");
    
  }
  

  public static void permissionCheckMethodsStats() {
    CheckForPermission.printLogValue();
  }


  public static void printMethodsPerPermissionSetSize(Map<SootMethod, Set<String>> methodToPermissionSet) {
    // compute methods for each permission set size
    HashMap<Integer, HashSet<SootMethod>> nbrPermissions2nbrMethods = new HashMap<Integer, HashSet<SootMethod>>();
    for (SootMethod sm : methodToPermissionSet.keySet()) {
      Integer nbrPermissions = Integer
          .valueOf(methodToPermissionSet.get(sm)
              .size());
      if (nbrPermissions2nbrMethods
          .containsKey(nbrPermissions)) {
        HashSet<SootMethod> mSet = nbrPermissions2nbrMethods.get(nbrPermissions);
        mSet.add(sm);
        nbrPermissions2nbrMethods.put(nbrPermissions,
            mSet);
      } else {
        HashSet mSet = new HashSet<SootMethod>();
        mSet.add(sm);
        nbrPermissions2nbrMethods.put(nbrPermissions,
            mSet);
      }
    }
    logger.info("");

    // print "[permission set size] [# of methods with this
    // permission set size]
    logger.info("nbr of methods for each permission set size:");
    for (Integer pnbr : nbrPermissions2nbrMethods.keySet()) {
            logger.info("Number of Soot methods with "
          + pnbr
          + " permissions: "
          + nbrPermissions2nbrMethods.get(pnbr)
              .size());
    }
    logger.info("");
    
    // for each permission set size, print methods which
    // have this permission set size
    logger.info("");
    logger.info("Methods for each permission set size:");
    for (Integer pnbr : nbrPermissions2nbrMethods.keySet()) {
            logger.info("Soot method with " + pnbr
          + " permissions: ");
      for (SootMethod sm : nbrPermissions2nbrMethods
          .get(pnbr)) {
        logger.info("   " + sm + " ");
        List<String> pList = new ArrayList(
            methodToPermissionSet.get(sm));
        Collections.sort(pList);
        for (String p : pList) {
          logger.info("      " + p);
        }
        logger.info("");
      }
    }
    logger.info("");
  }


  public static void printMethodsAndTheirPermissions(
      Map<SootMethod, Set<String>> methodToPermissionSet) {
    // print methods and their permissions
    logger.info("");
    logger.info("Methods and their permissions:");
    logger.info("");
    for (SootMethod sm : methodToPermissionSet.keySet()) {
      logger.debug(sm + ": ");
      for (String p : methodToPermissionSet.get(sm)) {
        logger.debug(p + ", ");
      }
      logger.info("");
    }
  }
  
  
}
