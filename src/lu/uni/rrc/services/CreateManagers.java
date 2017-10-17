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

package lu.uni.rrc.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import soot.Body;
import soot.Local;
import soot.Modifier;
import soot.RefType;
import soot.Scene;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.VoidType;
import soot.javaToJimple.LocalGenerator;
import soot.jimple.AssignStmt;
import soot.jimple.IdentityStmt;
import soot.jimple.InvokeExpr;
import soot.jimple.Jimple;
import soot.jimple.NewExpr;
import soot.jimple.NullConstant;

public class CreateManagers {
    
    String[] managerNames = {
            "window", 
            "layout_inflater", 
            "activity", 
            "input_method",                                                                                                                                                                                                        
            "alarm", 
            "account", 
            "power", 
            "connectivity", 
            "throttle", 
            "wifi", 
            "notification", 
            "keyguard", 
            "accessibility", 
            "location", 
            "search", 
            "sensor", 
            "storage", 
            "vibrator", 
            "statusbar", 
            "audio", 
            "phone", 
            "clipboard", 
            "wallpaper", 
            "dropbox", 
            "device_policy", 
            "uimode"    
    };
    
    String[] managerClasses = {
            "android.view.WindowManagerImpl",                                                                                                                                                                                      
            "android.view.LayoutInflater", 
            "android.app.ActivityManager", 
            "android.view.inputmethod.InputMethodManager", 
            "android.app.AlarmManager", 
            "android.accounts.AccountManager", 
            "android.os.PowerManager", 
            "android.net.ConnectivityManager", 
            "android.net.ThrottleManager", 
            "android.net.wifi.WifiManager", 
            "android.app.NotificationManager", 
            "android.app.KeyguardManager", 
            "android.view.accessibility.AccessibilityManager", 
            "android.location.LocationManager", 
            "android.app.SearchManager", 
            "android.hardware.SensorManager", 
            "android.os.storage.StorageManager", 
            "android.os.Vibrator", 
            "android.app.StatusBarManager", 
            "android.media.AudioManager", 
            "android.telephony.TelephonyManager", 
            "android.text.ClipboardManager", 
            "android.app.WallpaperManager", 
            "android.os.DropBoxManager", 
            "android.app.admin.DevicePolicyManager", 
            "android.app.UiModeManager"
    };
    
    String[] initMethods = {
            "<android.view.WindowManagerImpl: void <init>()>",
            "<com.android.internal.policy.PolicyManager: android.view.LayoutInflater makeNewLayoutInflater(android.content.Context)>", 
            "<android.app.ContextImpl: android.app.ActivityManager getActivityManager()>", 
            "<android.view.inputmethod.InputMethodManager: android.view.inputmethod.InputMethodManager getInstance(android.content.Context)>", 
            "<android.app.ContextImpl: android.app.AlarmManager getAlarmManager()>", 
            "<android.app.ContextImpl: android.accounts.AccountManager getAccountManager()>", 
            "<android.app.ContextImpl: android.os.PowerManager getPowerManager()>", 
            "<android.app.ContextImpl: android.net.ConnectivityManager getConnectivityManager()>", 
            "<android.app.ContextImpl: android.net.ThrottleManager getThrottleManager()>", 
            "<android.app.ContextImpl: android.net.wifi.WifiManager getWifiManager()>", 
            "<android.app.ContextImpl: android.app.NotificationManager getNotificationManager()>", 
            "<android.app.KeyguardManager: void <init>()>", 
            "<android.view.accessibility.AccessibilityManager: android.view.accessibility.AccessibilityManager getInstance(android.content.Context)>", 
            "<android.app.ContextImpl: android.location.LocationManager getLocationManager()>", 
            "<android.app.ContextImpl: android.app.SearchManager getSearchManager()>", 
            "<android.app.ContextImpl: android.hardware.SensorManager getSensorManager()>", 
            "<android.app.ContextImpl: android.os.storage.StorageManager getStorageManager()>", 
            "<android.app.ContextImpl: android.os.Vibrator getVibrator()>", 
            "<android.app.StatusBarManager: void <init>(android.content.Context)>", 
            "<android.app.ContextImpl: android.media.AudioManager getAudioManager()>", 
            "<android.app.ContextImpl: android.telephony.TelephonyManager getTelephonyManager()>", 
            "<android.app.ContextImpl: android.text.ClipboardManager getClipboardManager()>", 
            "<android.app.ContextImpl: android.app.WallpaperManager getWallpaperManager()>", 
            "<android.app.ContextImpl: android.os.DropBoxManager getDropBoxManager()>", 
            "<android.app.ContextImpl: android.app.admin.DevicePolicyManager getDevicePolicyManager()>", 
            "<android.app.ContextImpl: android.app.UiModeManager getUiModeManager()>"   
    };
    
    Map<String, String> createManagersFromGetSystemService(SootClass sc) {
        Map<String, String> managerType2getMethod = new HashMap<String, String>();
        for (int i=0; i < managerNames.length; i++) {
            String name = managerNames[i];
            
            if (!Scene.v().containsClass(name)) {
            	continue;
            }
            
            String returnType = managerClasses[i];
            makeGetManagerMethod(i, sc);
            managerType2getMethod.put(returnType, name);
        }
        return managerType2getMethod;
    }
    
    private void makeGetManagerMethod(int i, SootClass sc) {
       
        // create method to get manager
        String managerName = managerNames[i];
        String name = "getManager_"+ managerName;
        System.out.println(" [*] get manager method name '"+ name +"'");
        List<Type> args = new ArrayList<Type>();
        Type returnType = Scene.v().getSootClass(managerClasses[i]).getType();
        SootMethod sm = new SootMethod(name, args, returnType);
        
        // create body
        String mName = initMethods[i];
        if (Scene.v().containsClass("android.app.ApplicationContext")) {
        	// android api3
        	mName = mName.replace("<android.app.ContextImpl", "<android.app.ApplicationContext");
        }
        SootMethod initMethod = Scene.v().getMethod(mName);
        Body b = generateBody(initMethod);
        b.setMethod(sm); 
        sm.setActiveBody(b);
        sm.setModifiers(Modifier.STATIC | Modifier.PUBLIC);

        boolean cont = false;
        for (SootMethod sm2: sc.getMethods()) {
            if (sm.getName().equals(sm2.getName())) {
                System.out.println("warning: method already exists in ServiceInit class!!!");
                cont = true;
                break;
            }
        }
        if (cont)
            return;

        // we do this here since we have to know the real method signature
        // which is not available in updateNewBody()
        GenerateServiceInit.redirectContextFieldAssignments(b);

        sm.setDeclaringClass(sc);
        sc.addMethod(sm);
    }
    
    private Body generateBody(SootMethod initMethod) {
        String mName = initMethod.getName();
        Body newBody = Jimple.v().newBody();
        
        SootMethod tmpSM = new SootMethod("toto", new ArrayList<Type>(), VoidType.v());
        
        if (mName.equals("<init>")) {
            // first case: create new object
            newBody = createNewInstance(initMethod);
            // the following statement is temporary otherwise an exception 
            // will be raised during the unit graph construction
            newBody.setMethod(tmpSM); 
            System.out.println("add body 1:\n" + newBody);
            updateNewBody(newBody);
            System.out.println("add body 2:\n" + newBody);
        } else {
            // case 1: if (initMethod.getDeclaringClass().toString().equals("android.app.ContextImpl") 
            //         && mName.startsWith("get"))
            // case 2: static method to create manager
            //
            Body b = initMethod.retrieveActiveBody();
            newBody.importBodyContentsFrom(b);
            // the following statement is temporary otherwise an exception 
            // will be raised during the unit graph construction
            newBody.setMethod(tmpSM); 
            updateNewBody (newBody);
        } 
        return newBody;
      }
    
    /**
     * Create getManager_ for managers created through <init> methods
     * @param initMethod
     * @return
     */
    private Body createNewInstance(SootMethod initMethod) {
        
        SootClass servicesInitClass = Scene.v().getSootClass(GenerateServiceInit.servicesInitClassName);
        
        RefType newType = initMethod.getDeclaringClass().getType();
        Body b = Jimple.v().newBody();
        LocalGenerator lg = new LocalGenerator(b);
        Local newLocal = lg.generateLocal(newType);
        
        SootField sf = servicesInitClass.getFieldByName("androidcontentContext");
        Value newR = Jimple.v().newStaticFieldRef(sf.makeRef());
        Local contextLocal = lg.generateLocal(sf.getType());
        Unit u0 = Jimple.v().newAssignStmt(contextLocal, newR);               
        boolean addU0 = false;    
        
        NewExpr n1 = Jimple.v().newNewExpr(newType);
        Unit u1 = Jimple.v().newAssignStmt(newLocal, n1);
        List<Value> args = new ArrayList<Value>();
        for (Type t : initMethod.getParameterTypes()) {
            if (t.toString().startsWith("android.content.Context")) {
                args.add(contextLocal);
                addU0 = true;
            } else {
                args.add(NullConstant.v());
            }
        }
        
        InvokeExpr n2 = Jimple.v().newSpecialInvokeExpr(newLocal, initMethod.makeRef(), args);
        Unit u2 = Jimple.v().newInvokeStmt(n2);
        Unit u3 = Jimple.v().newReturnStmt(newLocal);
        
        if (addU0)
            b.getUnits().addFirst(u0);
        else
            b.getLocals().remove(contextLocal);
        System.out.println("u1: "+ u1);
        System.out.println("u1: "+ u2);
        System.out.println("u1: "+ u3);
        if (addU0)
            b.getUnits().insertAfter(u1, u0);
        else
            b.getUnits().addFirst(u1);
        b.getUnits().insertAfter(u2, u1);
        b.getUnits().insertAfter(u3, u2);
        
        return b;
    }
    
    

      private void updateNewBody(Body b) {
        
        SootClass servicesInitClass = Scene.v().getSootClass(GenerateServiceInit.servicesInitClassName);
   
  // since method is static, just remove first statement if necessary
//        // change reference to @this type
//        Unit first = b.getUnits().getFirst();
//        if (!(first instanceof IdentityStmt))
//          throw new RuntimeException("error: first statement not instance of IdentityStmt: "+ first);
//        IdentityStmt idstmt = (IdentityStmt)first;
//        Local left = (Local) idstmt.getLeftOp();
//        Value right = idstmt.getRightOp();
//        RefType t = servicesInitClass.getType();
//        ThisRef newThisRef = Jimple.v().newThisRef(t);
//        left.setType(t);
//        Unit newIdentityStmt = Jimple.v().newIdentityStmt(left, newThisRef);
//        b.getUnits().swapWith(first, newIdentityStmt);
        Unit first = b.getUnits().getFirst();
        if (first instanceof IdentityStmt) {
            System.out.println("removing first statement: "+ first);
            IdentityStmt idstmt = (IdentityStmt)first;
            Local left = (Local) idstmt.getLeftOp();
            Type leftType = left.getType();
            if (!leftType.toString().contains("Context"))
                throw new RuntimeException("error: doest not contain Context: "+ first);
            SootField sf = servicesInitClass.getFieldByName("androidcontentContext");
            Value newR = Jimple.v().newStaticFieldRef(sf.makeRef());
            Unit newU = Jimple.v().newAssignStmt(left, newR);
            b.getUnits().swapWith(first, newU);
        }
  //
        
        // if context parameter is present redirect it
        Unit second = b.getUnits().getFirst(); //.getSuccOf(newIdentityStmt);
        if (second instanceof IdentityStmt) {
          IdentityStmt pStmt = (IdentityStmt)second;
          Local l = (Local) pStmt.getLeftOp();
          Value r = pStmt.getRightOp();
          SootField sf = servicesInitClass.getFieldByName("androidcontentContext");
          Value newR = Jimple.v().newStaticFieldRef(sf.makeRef());
          AssignStmt newStmt = Jimple.v().newAssignStmt(l, newR);
          b.getUnits().swapWith(pStmt, newStmt);
        }
        
        // // redirect getService methods
        // RedirectService rs = new RedirectService();
        // List<Unit> getSystemService = rs.hasCallToGetSystem(b);
        // if (getSystemService.size() != 0) {
        // System.out.println("redirecting getService for body");
        // List<String> servicesCalled =
        // GenerateServiceInit.smap.getServiceNames(b, getSystemService);
        // rs.redirectGetSystemServiceCalls (b, getSystemService,
        // servicesCalled, servicesInitClass);
        // }
        

      }

}
