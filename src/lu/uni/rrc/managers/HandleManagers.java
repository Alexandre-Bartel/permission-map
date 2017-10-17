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

package lu.uni.rrc.managers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lu.uni.rrc.Config;
import lu.uni.rrc.HandleC;
import lu.uni.rrc.Util;
import lu.uni.rrc.services.HandleServices;
import lu.uni.rrc.services.RedirectServiceManager;
import lu.uni.rrc.services.ServiceMapper;
import soot.Body;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;

public class HandleManagers extends HandleC {
	
	private static final Logger logger = LoggerFactory.getLogger(HandleManagers.class);

    // only in 4.0 not 2.2
    public static String registerManagerSig = "<android.app.ContextImpl: void registerService(java.lang.String,android.app.ContextImpl$ServiceFetcher)>";
    // always present
    public static String r_getManagerSig = "java.lang.Object getSystemService(java.lang.String)>";

    private Set<SootMethod> addManagerSMSet = null;
    private Set<SootMethod> getManagerSMSet = null;

    Map<String, SootClass> managers = new HashMap<String, SootClass>();

    private Map<String, SootClass> managersRegistered = new HashMap<String, SootClass>();
    Map<SootMethod, Map<Unit, String>> managersGet = new HashMap<SootMethod, Map<Unit, String>>();

    public HandleManagers(Set<SootMethod> s1, Set<SootMethod> s2) {
        addManagerSMSet = s1;
        getManagerSMSet = s2;
    }

    public void doWork() {

        System.out.println("add manager count: " + addManagerSMSet.size());
        System.out.println("get manager count: " + getManagerSMSet.size());

        for (SootMethod sm : addManagerSMSet) {
            Body b = sm.retrieveActiveBody();
            System.out.println("add Manager - : " + sm + "\n" + b);
            // replace(b, sigList.get(i));
            List<Map<String, SootClass>> m = Util.getStringClassMappingForMethod(sm, registerManagerSig);
            for (Map<String, SootClass> map: m) {
            	for (String k: map.keySet()) {
            		if (managersRegistered.containsKey(k))
            			throw new RuntimeException("error: register already in map! "+ k + " "+ managersRegistered);
            		managersRegistered.put(k, map.get(k));
            	}
            }
        }
        // for some reason, "contactPhotos" service manager is not initialize the same way as the other
        // managers. 
        // TODO: should change how this manager is initialized
        String CONTACT_PHOTOS_SERVCE = "com.android.contacts.common.ContactPhotoManager";
        if (Scene.v().containsClass(CONTACT_PHOTOS_SERVCE)) {   	
        	managersRegistered.put("contactPhotos", Scene.v().getSootClass(CONTACT_PHOTOS_SERVCE));
        }


        for (SootMethod sm : getManagerSMSet) {
        	if (sm.toString().endsWith("getSystemService(java.lang.String)>")) {
        		logger.info("skipping "+ sm);
        		continue;
        	}
            Body b = sm.retrieveActiveBody();
            System.out.println("get Manager - : " + sm + "\n");
            logger.debug("" + b);
            // replace(b, sigList.get(i));
            managersGet.put(sm, Util.getGetUnitStringMap(sm, r_getManagerSig));

        }

        for (String k : managersRegistered.keySet()) {
                init.add(k);
                managers.put(k, managersRegistered.get(k));
                System.out.println(" <manager init> " + k + " : " + managersRegistered.get(k));
        }
        for (SootMethod k : managersGet.keySet()) {
            Map<Unit, String> m = managersGet.get(k);
            for (Unit u : m.keySet()) {
                called.add(m.get(u));
            }
        }
        for (String m : called) {
            System.out.println(" <manager called> " + m);
            if (!managers.containsKey(m)) {
            	logger.warn("error: manager called but not initialized: "+ m);
            	//throw new RuntimeException("error: manager called but not initialized: "+ m);
            }
        }

        checkAllCalledAreInit("manager");

    }

    public Map<String, SootClass> getManagers() {
        return managers;
    }

    public void redirect(SootClass servicesInitClass) {
        List<SootMethod> smList = new ArrayList<SootMethod>(getManagerSMSet);
        // add method from service init class
        for (SootMethod sm : servicesInitClass.getMethods()) {
            if (sm.getName().startsWith("getManager_")) {
                smList.add(sm);
            }
        }
        for (SootMethod sm : smList) {
            if (sm.getName().equals("getSystemService")) {
                System.out.println("Skipping " + sm);
                continue;
            }
            Body b = sm.retrieveActiveBody();
            RedirectServiceManager rsm = new RedirectServiceManager();
            List<Unit> getSystemServiceList = rsm.hasCallToSystemServices(b);
            List<Unit> getServiceList = rsm.hasCallToService(b);
            // redirect services
            if (Config.doRedirectService()) {
                rsm.redirectInstanceOf(b);
                List<Unit> getSystemService = rsm.hasCallToSystemServices(b);
                if (getSystemService.size() != 0) {
                    List<String> servicesCalled = ServiceMapper.v().getServiceNames(b,
                            getSystemService);
                    System.out.println("Redirecting to managers in method " + sm);
                    rsm.redirectGetSystemServiceCalls(sm.retrieveActiveBody(), getSystemService,
                            servicesCalled, servicesInitClass);
                }
            }
        }
    }

}
