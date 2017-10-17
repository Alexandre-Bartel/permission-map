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
import java.util.List;
import java.util.Set;
import java.util.Stack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lu.uni.fpc.Util;



import soot.Body;
import soot.Local;
import soot.SootMethod;
import soot.SootMethodRef;
import soot.Unit;
import soot.Value;
import soot.jimple.ArrayRef;
import soot.jimple.AssignStmt;
import soot.jimple.IdentityStmt;
import soot.jimple.InvokeExpr;
import soot.jimple.NewArrayExpr;
import soot.jimple.NullConstant;
import soot.jimple.ParameterRef;
import soot.jimple.Stmt;
import soot.jimple.StringConstant;
import soot.jimple.toolkits.callgraph.Edge;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.graph.UnitGraph;



public class CheckForPermission {

  private static Logger logger =  LoggerFactory.getLogger(lu.uni.fpc.string.CheckForPermission.class);


  // from android.content.Context:
  //   public abstract int checkPermission(String permission, int pid, int uid);
  //   public abstract int checkCallingPermission(String permission);
  //   public abstract int checkCallingOrSelfPermission(String permission);
  //   public abstract void enforcePermission(String permission, int pid, int uid, String message); 
  //   public abstract void enforceCallingPermission(String permission, String message);
  //   public abstract void enforceCallingOrSelfPermission(String permission, String message);

  // NOTE: URI permission checks are not handled.

  private final static String[] targetCheckMethods = {   
    //      "validatePermission", //specific to MountService
    //      "checkBinderPermission", //specific to AccountManager

    //      "enforceCallingOrSelfPermission",
    //      "enforceCallingPermission", // Dec 19 sometimes does not take a perm string as parameter such as in BatteryStatService
    //      "enforcePermission",
    //      "checkCallingOrSelfPermission",
    //      "checkCallingPermission",
    //      "checkPermission"
    "int checkPermission(java.lang.String,int,int)>",
    "int checkCallingPermission(java.lang.String)>",
    "int checkCallingOrSelfPermission(java.lang.String)>",
    "void enforcePermission(java.lang.String,int,int,java.lang.String)>", 
    "void enforceCallingPermission(java.lang.String,java.lang.String)>",
    "void enforceCallingOrSelfPermission(java.lang.String,java.lang.String)>"

  };

  /** returns true if sm is a check perm method (ie in constant targetCheckMethods) */
  public static boolean isCheckPermissionMethod(SootMethod tgt) {
    // TODO better selection based on signature
    // <a.b.C: void method(int,a.b.D)>
    String methodName = tgt.toString();
    //String tgtS = methodName.split(" ")[2].split("\\(")[0];
    for (String s: targetCheckMethods) {
      //if (tgtS.equals(s)) {
      if (methodName.endsWith(s)) {
        return true;
      }
    }
    return false;
  }

  private static final String UNKNOWN_PERM = "UNKNOWN";
  private static final String URI_READ_PERM = "URI_READ_PERM";
  private static final String URI_WRITE_PERM = "URI_WRITE_PERM";
  private static final String READ_FROM_PARCEl = "READ_FROM_PARCEL";
  private static final String STRING_FROM_GENERATIONGG = "STRING_FROM_GENERATIONGG";
  



  // CHECK PERMISSION
  /* returns ture if the destination of the edge is a permCheck
   * 
   *  The permission string or array of string is always the first argument.

		 From Context.java
		 -----------------
		 enforceCallingOrSelfPermission		void enforceCallingOrSelfPermission(String permission, String message)
		 enforceCallingPermission 				void enforceCallingPermission(String permission, String message)
		 enforcePermission								void enforcePermission(String permission, int pid, int uid, String message)
		 checkCallingOrSelfPermission 		int checkCallingOrSelfPermission(String permission)
		 checkCallingPermission						int checkCallingPermission(String permission)
		 checkPermission 									int checkPermission(String permission, int pid, int uid)

		 From AccountManagerService [SPECIAL: array of strings]
		 --------------------------
		 checkBinderPermission 						void checkBinderPermission(String... permissions) [calls checkCallingOrSelfPermission(p)] // any permission in the list

		 From ContextImpl.java
		 ---------------------
		 checkPermission									int checkPermission(String permName, String pkgName)

		 From ActivityManagerService.java
		 --------------------------------
		 in class PermissionController:
		 checkPermission									boolean checkPermission(String permission, int pid, int uid)
   */
  public static boolean checkForPermission (Edge e, Set<String> mpSet, Stack<SootMethod> stack) {
    
    SootMethod src = e.getSrc().method(); // is in the stack
    SootMethod tgt = e.getTgt().method(); // is not in the stack

    String tgtS = tgt.getName();

    if (!isCheckPermissionMethod(tgt))
      return false;
    
    AnalysisType.v().start();
    
    logger.info("check for permission: "+ e);

    // At this point we are sure that the target of the edge is a method
    // which checks Android permissions

    getStringFromMethod (stack, stack.size()-1, tgt, 0, mpSet);
    
    int mpSetSize = mpSet.size();
    AnalysisType.v().cur().setNbrPermissions(mpSetSize); 
    if (mpSetSize == 0) {
      logger.warn("WWWWWWWWW we could not find a perm string in "+ src);
      logger.warn(Util.printStack(stack));
      // we add the UNKNOWN permisson
      mpSet.add(UNKNOWN_PERM);
    }
    
    AnalysisType.v().end();
    return true;
  } 



  private static List<MethodCall> getStringParameterInCall(SootMethod targetM, SootMethod callToSearchM, int paramPos, Set<String> mpSet, boolean isCheckMethod) {
    List<MethodCall> methodCallsList = new ArrayList<MethodCall>();

    Body b = targetM.getActiveBody();
    for( Unit u: b.getUnits()) {
      MethodCall currMC = null;

      Stmt s = (Stmt)u;
      if (!s.containsInvokeExpr())
        continue;
      InvokeExpr invokeExpr = s.getInvokeExpr();

      boolean condition = false;
      if (isCheckMethod)
        condition = isCheckPermissionMethod(invokeExpr.getMethod());
      else
        condition = invokeExpr.getMethod().toString().split(":")[1].equals(callToSearchM.toString().split(":")[1]); // TODO: should check all out edges
      if (condition) {
        logger.info("Target method '"+ invokeExpr.getMethod() +"' found! Source method: "+ targetM);
        methodCallsList.add(new MethodCall(invokeExpr.getMethod(), u));
        currMC = methodCallsList.get(0);
      }

      if (currMC == null) {
        continue;
      }
      // at this point we know that we are dealing with a permission check method call unit

      // retrieve first argument which points to the permission string
      Value vParam = null;
      int count = invokeExpr.getArgCount();
      if (count <= 0)
        throw new RuntimeException("Method has 0 arguments! "+ s);
      vParam = invokeExpr.getArg(paramPos);

      // the first argument is a StringConstant whose value
      // is the permission string
      if (vParam instanceof StringConstant) { 
        AnalysisType.v().cur().case1_direct_string_passed_as_parameter();      
        currMC.paramConstantString = vParam.toString();
        logger.info("[F] "+ currMC.paramConstantString);
        mpSet.add (currMC.paramConstantString); // is String constant
        continue;
      } 

      if (vParam instanceof NullConstant) {
        logger.info("[F] NullConstant!");
        //mpSet.add("NULL_CONSTANT");
        continue;
      }
      
      // the first argument must be a local at this point
      if (!(vParam instanceof Local))
        throw new RuntimeException("vParam is not instance of Local! -> '"+ (vParam==null?"is null":vParam.getClass()) +"'"); 

      // store info about this local for step2 below
      currMC.paramName = ((Local)vParam).getName();
      currMC.paramLocal = (Local)vParam;
      currMC.paramNameUnit = u;
    }
    return methodCallsList;
  }


  /**
   * 
   * @param stack
   * @param curDepth
   * @param callToSearchM
   * @param paramPos
   * @param mpSet
   */
  private static void getStringFromMethod (Stack<SootMethod> stack, int curDepth, SootMethod callToSearchM, int paramPos, Set<String> mpSet) {
    
    if (curDepth < 0 || curDepth >= stack.size())
      throw new RuntimeException("error: depth not valid for current stack. depth < 0 || depth >= stack.size() ("+ curDepth +")");
    
    SootMethod targetM = stack.elementAt(curDepth);
    logger.info("(getStringFromMethod) target: "+ targetM);
    
    Body b = targetM.getActiveBody();
    UnitGraph eug = new ExceptionalUnitGraph( b );
    
    System.out.println("(getStringFromMethod) body: "+ b);
    
    // STEP 1/2: find the unit corresponding to this edge and the name of parameter
    List<MethodCall> methodCallsList = getStringParameterInCall(targetM, callToSearchM, paramPos, mpSet, (curDepth == stack.size() - 1 ?true:false));
    
    if (methodCallsList.size() == 0)
      throw new RuntimeException("error: did not find any call of method '"+ callToSearchM +"' in body of method '"+ targetM +"'");
    
    // At this point we have all permission check method call units  
    // whose source is the current edge source method
    //
    // STEP 2/2: we have to find where the parameter is created
    // we go backwards in the unit graph starting from "paramNameUnit" 
    for (MethodCall currMC: methodCallsList) {

      // If the permission has already been found, skip.
      if (currMC.paramConstantString != null)
        continue;

      logger.info("(getStringFromMethod) Analyzing method call '"+ currMC +"' param: '"+ currMC.paramName +"'");

      LocalDefinition ld = new LocalDefinition(b);
      Local l = currMC.paramLocal;
      List<Unit> defs = ld.collectDefinitionsWithAliases(l);

      for (Unit def: defs) {
        if (def instanceof AssignStmt) {
          AssignStmt ass = (AssignStmt)def;
          Value left = ass.getLeftOp();
          Value right = ass.getRightOp();
          if (right instanceof StringConstant) {
            AnalysisType.v().cur().case2_string_after_direct_assignment();
            logger.info("case2: AssignStmt: StringConstant "+ ass);
          } else if (right instanceof ArrayRef){
            AnalysisType.v().cur().case4_string_in_array();
            logger.info("case4: AssignStmt: ArrayRef "+ ass);
            ArrayRef ar = (ArrayRef)right;
            getArrayFromMethod (stack, curDepth, ar, mpSet);
          } else if (right instanceof NewArrayExpr) {
            logger.info("right is NewArray");
            // get the unit array[i] = str
            // TODO: Limitation: we suppose the array is initialized in the body where it is created
            // and that it is not aliased
            Local arrayLocal = (Local)ass.getLeftOp();
            List<Value> arrayInitValues = retrieveArrayInitStmts (b, arrayLocal);
            for (Value v: arrayInitValues) {
              if (v instanceof StringConstant) {
                StringConstant sc = (StringConstant)v;
                String p = sc.value;
                mpSet.add(p);
                logger.info("add perm from array inif: "+ p);
              } else {
                logger.warn("not handling this value for array init: "+ v);
              }
            }
          } else if (right instanceof InvokeExpr) {
            InvokeExpr ie = (InvokeExpr)right;
            SootMethodRef smr = ie.getMethodRef();
            String ms = smr.toString();
            if (ms.contains(": java.lang.String getReadPermission()>")) {
              AnalysisType.v().cur().case5_uri_read_permission();
              mpSet.add(URI_READ_PERM);
              logger.info("case6: uri read permission");
            } else if (ms.contains(": java.lang.String getWritePermission()>")) {
              AnalysisType.v().cur().case6_uri_write_permission();
              mpSet.add(URI_WRITE_PERM);
              logger.info("case7: uri write permission");
            } else if (ms.contains("<android.os.Parcel: java.lang.String readString()>")) {
              AnalysisType.v().cur().case7_read_from_parcel();
              mpSet.add(READ_FROM_PARCEl);
              logger.info("case8: read permission from parcel");
            } else if (ms.contains("<GenerationGG:")) {
              AnalysisType.v().cur().case8_string_from_generationGG();
              mpSet.add(STRING_FROM_GENERATIONGG);
            } else {
              AnalysisType.v().cur().case_strange();
              logger.warn("Strange assignment method invoke: "+ass);
            }
          } else {
            AnalysisType.v().cur().case_strange();
            logger.warn("Strange assignment: "+ ass);
          }      
        } else if (def instanceof IdentityStmt) {
          AnalysisType.v().cur().case3_string_after_flow_analysis();
          logger.info("IdentityStmt: must perform a backward analysis of methods in the stack.");
          
          IdentityStmt ids = (IdentityStmt)def;
          ParameterRef pref = (ParameterRef)ids.getRightOp();
          int paramPosition = pref.getIndex();
          logger.info("index of array parameter: "+ paramPosition); // TODO: should be debug
          getStringFromMethod (stack, curDepth-1, targetM, paramPosition, mpSet);
        } else {
          throw new RuntimeException("local definition not instance of AssignStmt nor IdentityStmt! "+ def +"("+ def.getClass() +") from local "+ l +" in body: "+ b);
        }
      }
    } // for(MethodCall in methodCallsList )
  }

  /**
   * Get the definition of the Array starting from an ArrayRef.
   * Two possibilities:
   * 1) array = IdentityStmt in which case the previous method in the stack is analyzed
   * 2) array = newarray in which case all statements array[i] = r are analyzed
   * @param stack
   * @param curDepth
   * @param ar
   * @param mpSet
   */
  private static void getArrayFromMethod (Stack<SootMethod> stack, int curDepth, ArrayRef ar, Set<String> mpSet) {
    
    SootMethod targetM = stack.elementAt(curDepth);
    logger.info("getArrayFromMethod target: "+ targetM);
    
    Body b = targetM.getActiveBody();
    System.out.println("body: "+ b); // TODO: change to logger.debug
    UnitGraph eug = new ExceptionalUnitGraph( b ); 

    LocalDefinition ld = new LocalDefinition(b);
    Local l = (Local)ar.getBase();
  
    List<Unit> arrayDefs = ld.collectDefinitionsWithAliases(l);
    for (Unit arrayDef: arrayDefs) {
      if (arrayDef instanceof IdentityStmt) {
        logger.info("array: right is IdentityStmt");
        IdentityStmt ids = (IdentityStmt)arrayDef;
        ParameterRef pref = (ParameterRef)ids.getRightOp();
        int paramPos = pref.getIndex();
        logger.info("index of array parameter: "+ paramPos); // TODO: should be debug
//        List<MethodCall> methodCallsList = getStringParameterInCall(stack.elementAt(curDepth - 1), targetM, paramPos, mpSet);
//        getArrayFromMethod (stack, stack.size()-1, ar, mpSet);
        getStringFromMethod (stack, curDepth-1, targetM, paramPos, mpSet);
      } else if (arrayDef instanceof AssignStmt) {
        AssignStmt ass = (AssignStmt) arrayDef;
        Value right = ass.getRightOp();
        if (right instanceof NewArrayExpr) {
          logger.info("array: right is NewArray");
          // get the unit array[i] = str
          // TODO: Limitation: we suppose the array is initialized in the body where it is created
          // and that it is not aliased
          Local arrayLocal = (Local)ass.getLeftOp();
          List<Value> arrayInitValues = retrieveArrayInitStmts (b, arrayLocal);
          for (Value v: arrayInitValues) {
            if (v instanceof StringConstant) {
              StringConstant sc = (StringConstant)v;
              String p = sc.value;
              mpSet.add(p);
              logger.info("add perm from array inif: "+ p);
            } else {
              logger.warn("not handling this value for array init: "+ v);
            }
          }
        } else if (right instanceof Local){
          logger.info("alias "+ ass); // definitions *and* aliases are collected, so no need to handle them separately
        } else {
          throw new RuntimeException("error: right not instance of NewArrayExpr nor Local! "+ ass);
        }
      }
    }

  }
  
  private static List<Value> retrieveArrayInitStmts(Body b, Local arrayLocal) {
    List<Value> arrayInitValues = new ArrayList<Value>();
    for (Unit u: b.getUnits()) {
      if (u instanceof AssignStmt) {
        AssignStmt ass = (AssignStmt) u;
        if (ass.getLeftOp() instanceof ArrayRef) {
          ArrayRef ar = (ArrayRef)ass.getLeftOp();
          if (ar.getBase() == arrayLocal) {
            arrayInitValues.add(ass.getRightOp());
          }
        }
      }
    }
    return arrayInitValues;
  }



  public static void printLogValue() {
    AnalysisType.v().printLogValue();
  }

}
