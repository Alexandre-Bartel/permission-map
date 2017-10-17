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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import soot.SootClass;
import soot.SootMethod;

public class Config {

	private List<String> targetLibPaths = new ArrayList<String>();
	private List<String> targetClasses = new ArrayList<String>();
	private List<String> skipLibPaths = new ArrayList<String>();
	private List<String> skipClasses = new ArrayList<String>();

	private Map<String, Integer> frameworkMethods = new HashMap<String, Integer>();
	private Set<String> frameworkClasses = new HashSet<String>();

	private boolean generateTypes = false;

	private static Config singleton = null;
	private Config() {}

	public static Config v() {
		if (singleton == null)
			singleton = new Config();
		return singleton;
	}

	public boolean isTargetClass(String sclass) {
		// skip classes
		for (String p: skipLibPaths) {
			if (sclass.startsWith(p)) {
				return false;
			}
		}
		for (String c: skipClasses) {
			if (sclass.equals(c))
				return false;
		}
		
		// check classes
		if (frameworkClasses.contains(sclass)) {
			return true;
		}
		if (targetClasses.contains(sclass)) {
				return true;
		}
		for (String p: targetLibPaths) {
			if (sclass.startsWith(p)) {
				return true;
			}
		}
		
		// default
		return false;
	}

	public void setGenerateType(boolean b) {
		generateTypes = b;
	}

	public boolean getGenerateType() {
		return generateTypes;
	}

	public void loadConfig(File f) throws IOException {
		InputStreamReader in = new InputStreamReader(new FileInputStream(f));
		BufferedReader br = new BufferedReader(in);
		String s;
		while ((s = br.readLine()) != null) {
			if (s.startsWith("#"))
				continue;
			s = s.trim();
			if (s.equals(""))
				continue;
			if (s.startsWith("-")) {
				s = s.replaceAll("-", "").trim();
				if (s.endsWith("."))
					skipLibPaths.add(s);
				else
					skipClasses.add(s);
			} else {
				if (s.endsWith("."))
					targetLibPaths.add(s.trim());
				else
					targetClasses.add(s.trim());
			}
		}

		printConfig();
	}

	public void printConfig() {
		System.out.println("uhtoenhunohun");
		for (String s: targetLibPaths) {
			System.out.println("target lib path  : "+ s);
		}
		for (String s: targetClasses) {
			System.out.println("target class     : "+ s);
		}
		for (String s: skipLibPaths) {
			System.out.println("skip lib path    : "+ s);
		}
		for (String s: skipClasses) {
			System.out.println("skip class       : "+ s);
		}
	}

	public void reset() {
		targetLibPaths = new ArrayList<String>();
		generateTypes = false;
	}

	public void loadFrameworkClasses(File managerClassesFile) {
		int i = 0;
		try {
			BufferedReader br = new BufferedReader(new FileReader(managerClassesFile));
			String l = null;
			while ((l = br.readLine()) != null) {
				l = l.trim();
				if (!l.startsWith("<")) {
					continue;
				}
				String method = l.split("> ")[0] +">";
				String modifiers = l.split("> ")[1];
				frameworkMethods.put(method, new Integer(Integer.parseInt(modifiers)));
				//System.out.println("add new method: '"+ l +"'");
				String newClassName = l.split(":")[0].replaceAll("^<", "");
				frameworkClasses.add(newClassName);
				//System.out.println("add new class '"+ newClassName +"'");
				i++;
			}
			System.out.println("Loaded "+ i +" framework methods from "+ managerClassesFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new RuntimeException("error");
		}

	}
	public boolean isFrameworkMethod(SootMethod sm) {
		boolean hasKey = frameworkMethods.containsKey(sm.toString());
		if (!hasKey) { 
			System.out.println("Warning: not a framework method : "+ sm);
			return false; 
		}
		
		// check modifiers
		int smModifiers = sm.getModifiers();
		int frameworkModifiers = frameworkMethods.get(sm.toString()).intValue();
		if (sm.getModifiers() != frameworkModifiers) {
			System.out.println("Warning: different modifiers for method "+ sm + " " + smModifiers + " " + frameworkModifiers);
			return false;
		}
		
		return true;
	}
	
	public boolean isFrameworkClass(SootClass sc) {
		return frameworkClasses.contains(sc.toString());
	}

	public boolean isServicesInitMethod(SootMethod m) {
		return m.getDeclaringClass().toString().equals("ServicesInit");
	}


}
