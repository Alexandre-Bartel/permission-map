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

package lu.uni.epw;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import soot.Scene;
import soot.SootClass;
import soot.SootMethod;

public class Managers {

	public static boolean canProcess = false;

	private static Map<String, String> managerClasses2constructorMethods = new HashMap<String, String>();

	public static void loadManagers(String filename) {
		filename = filename.replaceAll("/[^/]*.jar", "/");
		filename += "/managers.txt";
		Debug.printDbg("[manager] looking for manager file '"+ filename +"'");
		File f = new File(filename);
		if (!f.exists()) {
			Debug.printDbg("[manager] no manager file found.");
			return;
		}
		Debug.printDbg("[manager] reading manager file...");
		canProcess = true;
		BufferedReader br;
		try {
			br = new BufferedReader(new FileReader(f));
			String line;
			while ((line = br.readLine()) != null) {
				if (line.startsWith("#"))
					continue;
				String[] sp = line.split("\\ ");
				String className = sp[0];
				String method = sp[1];
				System.out.println("[managers] add ("+ className +","+ method +")");
				managerClasses2constructorMethods.put(className, method);
			}
			br.close();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	public static List<SootMethod> getManagerConstructor(SootClass sc) {
		List<SootMethod> cl = new ArrayList<SootMethod>();
		boolean containsSI = Scene.v().containsClass("ServicesInit");
		if (!containsSI)
			throw new RuntimeException("error: Soot's scene does not contain ServicesInit!");
		SootClass services = Scene.v().getSootClass("ServicesInit");
		System.out.println("sc: " + sc);
		String managerMethodName = managerClasses2constructorMethods.get(sc.toString());
		if (!managerMethodName.startsWith("getManager_")) {
			managerMethodName = "getManager_" + managerMethodName;
		}
		System.out.println("manager method name: " + managerMethodName);
		SootMethod managerConstructor = services.getMethodByName(
				managerMethodName);
		System.out.println("[managers] returning method: "+ managerConstructor);
		cl.add(managerConstructor);
		return cl;
	}

	public static boolean isManager(SootClass sc) {
		if (canProcess && managerClasses2constructorMethods.keySet().contains(sc.getName()) ){
			System.out.println("is manager: "+ sc);
			return true;
		}
		return false;
	}

}
