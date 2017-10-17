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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lu.uni.rrc.Config;
import lu.uni.rrc.HandleC;
import lu.uni.rrc.HandleIntents;
import lu.uni.rrc.Util;
import soot.Body;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;

/**
 * 
 * 
 * SystemService publishBinderService(java.lang.String, android.os.IBinder)
 *  --> SystemService publishBinderService(java.lang.String, android.os.IBinder, boolean) 
 *   --> ServiceManager addService(java.lang.String,android.os.IBinder,boolean)           <<<  ServiceManager addService(java.lang.String,android.os.IBinder)
 *    --> IServiceManager addService(java.lang.String,android.os.IBinder,boolean)
 *     --> ServiceManagerProxy addService(java.lang.String,android.os.IBinder,boolean)
 *      --> Parcel: void writeStrongBinder(android.os.IBinder)
 *      
 * @author alex
 *
 */
public class HandleServices extends HandleC {

	private static final Logger logger = LoggerFactory.getLogger(HandleServices.class);
	
	// add ibinder service
    public static String addService1Sig = "<android.os.ServiceManager: void addService(java.lang.String,android.os.IBinder)>";
    public static String addService2Sig = "<android.os.ServiceManager: void addService(java.lang.String,android.os.IBinder,boolean)>";
    public static String r_publishService1Sig = "void publishBinderService(java.lang.String,android.os.IBinder)>";
    public static String r_publishService2Sig = "void publishBinderService(java.lang.String,android.os.IBinder,boolean)>";
    // get service?
    public static String getServiceSig = "<android.os.ServiceManager: android.os.IBinder getService(java.lang.String)>";
    public static String r_asInterfaceSig = " asInterface(android.os.IBinder)>";
    public static String startSystemServiceSig = "<com.android.server.SystemServiceManager: com.android.server.SystemService startService(java.lang.Class)>";
    public static String r_getSystemServiceSig = ": java.lang.Object getSystemService(java.lang.String)>";
    
    private Set<SootMethod> publishSystemService1SMSet = null;
    private Set<SootMethod> publishSystemService2SMSet = null;
    private Set<SootMethod> addService1SMSet = null;
    private Set<SootMethod> addService2SMSet = null;
    //
    private Set<SootMethod> startSystemServiceSMSet = null;
    private Set<SootMethod> getServiceSMSet = null;
    private Set<SootMethod> asInterfaceSMSet = null;
	private Set<SootMethod> getSystemServiceSMSet = null;
	

    static Map<String, SootClass> serviceMap = new HashMap<String, SootClass>();

	private Map<String, SootClass> publishSystemService1_name2ibinder = new HashMap<String, SootClass>();
	private Map<String, SootClass> publishSystemService2_name2ibinder = new HashMap<String, SootClass>();
    private List<Map<String, SootClass>> addService1_name2ibinder = new ArrayList<Map<String, SootClass>>();
    private List<Map<String, SootClass>> addService2_name2ibinder = new ArrayList<Map<String, SootClass>>();
    //
    private Map<SootMethod, Map<Unit, String>> servicesGet = new HashMap<SootMethod, Map<Unit, String>>();
    private Map<SootMethod, Map<Unit, String>> systemServicesGet = new HashMap<SootMethod, Map<Unit, String>>();
	private List<String> systemServicesRegistered = new ArrayList<String>();


    public HandleServices(
    		Set<SootMethod> publishSystemService1SMSet,
    		Set<SootMethod> publishSystemService2SMSet,
    		Set<SootMethod> addService1SMSet,
    		Set<SootMethod> addService2SMSet,
    		Set<SootMethod> startSystemServiceSMSet,
    		Set<SootMethod> getServiceSMSet, 
    		Set<SootMethod> getSystemServiceSMSet,
    		Set<SootMethod> asInterfaceSMSet
    		) {
    	this.publishSystemService1SMSet = publishSystemService1SMSet;
    	this.publishSystemService2SMSet = publishSystemService2SMSet;
        this.addService1SMSet = addService1SMSet;
        this.addService2SMSet = addService2SMSet;
        //
        this.startSystemServiceSMSet = startSystemServiceSMSet;
        this.getServiceSMSet = getServiceSMSet;
        this.getSystemServiceSMSet = getSystemServiceSMSet;
        this.asInterfaceSMSet = asInterfaceSMSet;
    }

    public void doWork() {

    	// add services
        for (SootMethod sm : addService1SMSet) {
            Body b = sm.retrieveActiveBody();
            System.out.println("add service1 - : " + sm);
            logger.debug("", b);
            addService1_name2ibinder.addAll(Util.getStringClassMappingForMethod(sm, addService1Sig));
        }
        for (SootMethod sm : addService2SMSet) {
            Body b = sm.retrieveActiveBody();
            System.out.println("add service2 - : " + sm);
            logger.debug("", b);
            addService2_name2ibinder.addAll(Util.getStringClassMappingForMethod(sm, addService2Sig));
        }
        //mapName2IBinder()
        for (SootMethod sm: publishSystemService1SMSet) {
        	Map<Unit, String> u2str = Util.getGetUnitStringMap(sm, r_publishService1Sig);
        	if (u2str.size() != 1) {
        		throw new RuntimeException("error: expected a single string. Got "+ u2str);
        	}
        	String name = null;
        	for (Unit u: u2str.keySet()) {
        		name = u2str.get(u);
        		if (name == null) {
        			throw new RuntimeException("error: name is null. "+ sm.retrieveActiveBody());
        		}
        	}
        	publishSystemService1_name2ibinder.put(name, sm.getDeclaringClass());
        }
        
        
        
        
        for (SootMethod sm : getServiceSMSet) {
            Body b = sm.retrieveActiveBody();
            System.out.println("get service - : " + sm + "\n" + b);
            // replace(b, sigList.get(i));
            Map<Unit, String> m = Util.getGetUnitStringMap(sm, getServiceSig);
            // check
            for (Unit k: m.keySet()) {
            	if (m.get(k) == null) {
            		throw new RuntimeException("error: name is null. "+ sm.retrieveActiveBody());
            	}
            }
            servicesGet.put(sm, m);

        }
        
        // system services
        for (SootMethod sm : startSystemServiceSMSet) {
        	
        	// skip the system method to start services
        	if (sm.getName().equals("startService") && sm.getDeclaringClass().getName().endsWith("ServiceManager")) {
        		continue;
        	}
        	
            Body b = sm.retrieveActiveBody();
            System.out.println("add system service - : " + sm);
            logger.debug("", b);
            // replace(b, sigList.get(i));
            Map<Unit, String> startList = Util.getGetUnitStringMap(sm, startSystemServiceSig);
            for (Unit k: startList.keySet()) {
            	String classname = startList.get(k);
            	if (classname == null) {
        			throw new RuntimeException("error: name is null. "+ sm.retrieveActiveBody());
        		}
            	systemServicesRegistered .add(classname);
            }

        }
        for (SootMethod sm: getSystemServiceSMSet) {
        	systemServicesGet.put(sm, Util.getGetUnitStringMap(sm, r_getSystemServiceSig));
        }


        
        // print stats
        for (Map<String, SootClass> s : addService1_name2ibinder) {
            for (String k : Util.asSortedList(s.keySet())) {
                init.add(k);
                serviceMap.put(k, s.get(k));
                System.out.println(" <service init> " + k + " : " + s.get(k));
            }
        }
        for (String ssclassname: systemServicesRegistered) {
        	System.out.println("<system service registered> "+ ssclassname );
        }
        for (String ssname : publishSystemService1_name2ibinder.keySet()) {
        	System.out.println("<system service init> "+ ssname +" -> "+ publishSystemService1_name2ibinder.get(ssname));
        }

        for (SootMethod sm : asInterfaceSMSet) {
            System.out.println("<as interface> " + sm);
        }

        for (SootMethod k : servicesGet.keySet()) {
            Map<Unit, String> m = servicesGet.get(k);
            for (Unit u : m.keySet()) {
                called.add(m.get(u));
            }
        }
        for (String str : Util.asSortedList(called)) {
            System.out.println(" <service called> " + str);
        }
        //
        for (SootMethod k : systemServicesGet.keySet()) {
            Map<Unit, String> m = systemServicesGet.get(k);
            for (Unit u : m.keySet()) {
                sscalled.add(m.get(u));
            }
        }
        for (String str : Util.asSortedList(sscalled)) {
            System.out.println(" <system service called> " + str);
        }

        checkAllCalledAreInit("service");

    }

    public static Map<String, SootClass> getInitServicesMap() {
        return serviceMap;
    }

    public void redirect(SootClass servicesInitClass) {
        Set<SootMethod> smList = new HashSet<SootMethod>(getServiceSMSet);
        // add methods from asInterface
        smList.addAll(asInterfaceSMSet);
        // add method from service init class
        for (SootMethod sm : servicesInitClass.getMethods()) {
            if (sm.getName().startsWith("getManager_")) {
                smList.add(sm);
            }
        }
        for (SootMethod sm : getServiceSMSet) {
            Body b = sm.retrieveActiveBody();

            // redirect getService methods
            RedirectService rs = new RedirectService();
            List<Unit> getSystemService = rs.hasCallToGetSystem(b);
            if (getSystemService.size() != 0) {
                System.out.println("Redirecting getService in method " + sm);
                List<String> servicesCalled = ServiceMapper.v()
                        .getServiceNames(b, getSystemService);
                rs.redirectGetSystemServiceCalls(b, getSystemService, servicesCalled,
                        servicesInitClass);
            }

            RedirectServiceManager rsm = new RedirectServiceManager();
            List<Unit> getSystemServiceList = rsm.hasCallToSystemServices(b);
            List<Unit> getServiceList = rsm.hasCallToService(b);
            // redirect services
            if (Config.doRedirectService()) {
                rsm.redirectInstanceOf(b);
                List<Unit> getSystemService2 = rsm.hasCallToSystemServices(b);
                if (getSystemService2.size() != 0) {
                    List<String> servicesCalled = ServiceMapper.v().getServiceNames(b,
                            getSystemService2);
                    System.out.println("Redirecting to managers in method " + sm);
                    rsm.redirectGetSystemServiceCalls(sm.getActiveBody(), getSystemService2,
                            servicesCalled, servicesInitClass);
                }
            }

        }
    }

}
