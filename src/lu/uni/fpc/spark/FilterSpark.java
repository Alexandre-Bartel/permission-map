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

package lu.uni.fpc.spark;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lu.uni.fpc.EntryPointKey;
import lu.uni.fpc.MethodSearchTree;
import lu.uni.fpc.Util;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;

public class FilterSpark {
  
  private static FilterSpark instance = null;
  
  private static Logger logger = LoggerFactory.getLogger(lu.uni.fpc.spark.FilterSpark.class);
  
  private List<String> classesToAnalyze = new ArrayList<String>();
  private MethodSearchTree methodSignaturesToSkip = new MethodSearchTree();
  private Map<File, Long> loadedFiles = new HashMap<File, Long>();
  
  
  private FilterSpark() {}
  
  public static FilterSpark v() {
    if (instance == null) {
      instance = new FilterSpark();
    }
    return instance;
  }
  
  // load entry points to analyze. Other entry points are skipped.
  //
  // forceEpSet contains string of entry point methods but not its wrapper
  // recall that an entry point is: (wrapper -> method)
  private Set<String> forceEpSet = new HashSet<String>();
  public long forceEntryPoints(String forceEpFilename) {
    long epnbr = 0;
    BufferedReader br;
    
    File forceEpFile = new File(forceEpFilename);
    if (!forceEpFile.exists() && !forceEpFile.canRead())
      throw new RuntimeException("error: file does not exist or cannot be read. '"+ forceEpFile +"'");
    try {
      br = new BufferedReader(new FileReader(forceEpFile));
      String line;
      while ((line = br.readLine()) != null) {
       if (line.startsWith("#"))
         continue;
       if (line.equals(""))
         continue;
       if (!line.contains("->")) {
         continue;
       }
       //line = line.replaceAll("> : ", ">");
       //String wrapper = line.split(" -> ")[0].trim();
       String method = line.split(" -> ")[1].trim();
       //EntryPointKey epk = new EntryPointKey(wrapper, method);
       System.out.println("add epk method '"+ method +"'");
       forceEpSet.add(method);
       //if (!isForcedEntryPointMethod(epk))
       //  throw new RuntimeException("error: epk added in set is not detected! "+ epk);
       epnbr++;
      }
      br.close();
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(-1);
    }
    return epnbr;
  }
  
  // load zero permission methods
  private Set<String> zeroPermMethodsSet = new HashSet<String>();
  public long loadZeroPermMethods(String zPermMethodsFilename) {
    long nbr = 0;
    BufferedReader br;
    
    File zPermMethodsFile = new File(zPermMethodsFilename);
    if (!zPermMethodsFile.exists() && !zPermMethodsFile.canRead())
      throw new RuntimeException("error: file does not exist or cannot be read. '"+ zPermMethodsFile +"'");
    try {
      br = new BufferedReader(new FileReader(zPermMethodsFile));
      String line;
      while ((line = br.readLine()) != null) {
       if (line.startsWith("#"))
         continue;
       if (line.equals(""))
         continue;
       if (!line.startsWith("<")) {
         continue;
       }
       String method = line.trim();
       System.out.println("add zero permission method '"+ method +"'");
       zeroPermMethodsSet.add(method);
       nbr++;
      }
      br.close();
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(-1);
    }
    return nbr;
  }
  
  public boolean mustSkipZeroPermMethod(Stack<SootMethod> stack, SootMethod currentMethod) {
    
    String mName = currentMethod.toString();
    
    if (stack.size() < 2)
      return false;
    
    if (zeroPermMethodsSet.contains(mName))
      return true;
        
    return false;
  }
  
  
  
  
  private boolean isForcedEntryPointMethod(EntryPointKey epk1) {
    EntryPointKey epk = new EntryPointKey(epk1.getWrapperString(), epk1.getEpString());
    if (forceEpSet.contains(epk))
      return true;
    return false;
  }
  
  private boolean isForcedEntryPointMethod (SootMethod wrapperSM, SootMethod epSM) {
    EntryPointKey epk = new EntryPointKey(wrapperSM, epSM);
    if (forceEpSet.contains(epk))
      return true;
    return false;
  }
  
  public void updateFromFiles() {
    for (File f: loadedFiles.keySet()) {
      if (f.lastModified() != loadedFiles.get(f).longValue()) {
        addClassesToSkip (f);
      }
    }
  }
  
  public void addDefaultClassesToKeep() {
    // TODO Auto-generated method stub
    
  }

  public void addDefaultClassesToSkip() {
    // TODO Auto-generated method stub
    
  }
  
  public void addClassesToAnalyze(File f) {
    BufferedReader br;
    try {
      br = new BufferedReader(new FileReader(f));
      String line;
      while ((line = br.readLine()) != null) {
       if (line.startsWith("#"))
         continue;
       if (line.equals(""))
         continue;
       classesToAnalyze.add(line);
      }
      br.close();
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(-1);
    }
  }
  
  public void addClassesToSkip(File f) {
    String filename = f.getPath();
    if (!f.exists()) {
      System.err.println("file not found! '"+ filename +"'");
      return;
    }
    loadedFiles.put(f, new Long(f.lastModified()));
    BufferedReader br;
    int c = 0;
    try {
      br = new BufferedReader(new FileReader(f));
      String line;
      while ((line = br.readLine()) != null) {
       if (line.startsWith("#"))
         continue;
       if (line.equals(""))
         continue;
       if (!line.startsWith("<")) {
         System.err.println("error in file '"+ filename +"' line does not start with < : '"+ line +"'");
         continue;
       }
       String method = line;
       methodSignaturesToSkip.addMethod(method);
       c++;
      }
      br.close();
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(-1);
    }
    System.out.println("[filter-spark] Add "+ c +" methods from "+ filename);
  }
  
  private boolean skipLoadedMethods (SootMethod sm) {
    String msig = sm.toString();
    if (methodSignaturesToSkip.hasMethod(msig))
        return true;  
    return false;
  }
  
  private Set<EntryPointKey> epkToSkip = new HashSet<EntryPointKey>();
  public void addSkipEntryPoint(String fn) {
    File f = new File(fn);
    if (!f.exists()) {
      System.err.println("file not found! '"+ fn +"'");
      return;
    }
    int c = 0;
    BufferedReader br;
    try {
      br = new BufferedReader(new FileReader(f));
      String line;
      while ((line = br.readLine()) != null) {
       if (line.startsWith("#"))
         continue;
       if (line.equals(""))
         continue;
       if (!line.startsWith("<")) {
         System.err.println("error in file '"+ fn +"' line does not start with < : '"+ line +"'");
         continue;
       }
       String[] split = line.split(" -> ");
       String wrapper = split[0];
       String method = split[1];
       EntryPointKey epk = new EntryPointKey(wrapper, method);
       epkToSkip.add(epk);
       c++;
      }
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(-1);
    }
    System.out.println("[filter-spark] Add "+ c +" entry point methods to skip from "+ fn);
  }
  public boolean mustSkipEntryPoint(EntryPointKey epk) {
    if (epkToSkip.contains(epk)) {
      logger.info("Skipping entry point '"+ epk +"'");
      return true;
    }
    return false;
  }
  
//  public boolean mustAnalyzeEntryPoint(EntryPointKey epk) {
//    // force entry point to analyze
//    if (!ConfigSpark.v().hasReadZeroPermEp())
//      return true;
//
//    SootMethod wrapper = epk.getWrapper();
//    SootMethod ep = epk.getEp();
//    String declaringClass = wrapper.getDeclaringClass().toString();
//    if (declaringClass.startsWith("ServicesInit") || declaringClass.startsWith("GenerationGG"))
//      return false;//return false;
//    if (!declaringClass.startsWith("Wrapper")) {
//      throw new RuntimeException("error: expected wrapper class, got '"+ wrapper +"'");
//    }
//    if (isForcedEntryPointMethod(wrapper, ep)) {
//      logger.info("[forceep] forced entry point: "+ wrapper +" -> "+ ep);
//      return true;
//    } else {
//      logger.info("[forceep] skip entry point: "+ wrapper +" -> "+ ep);
//      return false;
//    }
//    
//  }
  
  public boolean mustSkip(Stack<SootMethod> stack, SootMethod currentMethod) {
    
    String mName = currentMethod.toString().replaceAll("<", "").replaceAll(">", "");
    String className = mName.split(":")[0];
    SootClass mClass = Scene.v().getSootClass(mName.split(":")[0]);
    
    if (stack.size() == 1) {
      if (!(mName.startsWith("Wrapper_"))) {
        SStats.v().addNotEntryPointWrapper();
        return true;
      }
    }
    
    if (stack.size() == 2) {
      if (!(mName.startsWith("android.") || mName.startsWith("com.android."))) {
        SStats.v().addNotEntryPointMethod();
        return true;
      }
      if (ConfigSpark.v().doForceEntryPoints() && forceEpSet.size() > 0) {
        if (!forceEpSet.contains(currentMethod.toString())) {
          System.out.println("skipping  ep: "+ stack.lastElement() +" -> "+ currentMethod);
          return true;
        } else {
          System.out.println("analyzing ep: "+ stack.lastElement() +" -> "+ currentMethod);
        }
      }
    }
      
    
    if (stack.size() <= 2)
      return false;
    
    // only keep android.* and com.android.*
    if (!(mName.startsWith("android.") || mName.startsWith("com.android."))) {
      //System.out.println("[filter-android] "+ mName);
      return true;
    }
    
    if (ConfigSpark.v().skipStubProxy())
      if (className.endsWith("$Proxy") || className.endsWith("$Stub$Proxy"))
        return true;
        
    return false;
  }
  
  public boolean mustSkip3(Stack<SootMethod> stack, SootMethod currentMethod) {
    String mName = currentMethod.toString().replaceAll("<", "").replaceAll(">", "");
    SootClass mClass = Scene.v().getSootClass(mName.split(":")[0]);
    String cName = mClass.getName();
  
    if (stack.size() > 2 && !mName.startsWith("android.") && !mName.startsWith("com.android.")) {
      //logger.info("[filter-android] "+ mName);
      return true;
    }
    
    if (cName.startsWith("ServicesInit") || cName.startsWith("GenerationGG") || cName.endsWith("\\$Proxy"))
      return true;//return false;
    
    // depth 0: main entry point
    // depth 1: wrapper entry point
    if (stack.size() < 2) {
      return false;
    }
    
    // check if must analyze method in class
    boolean mustAnalyzeClass = false;
    for (String s: classesToAnalyze) {
      if (mName.startsWith(s)) {
        mustAnalyzeClass = true;
      }
    }
    if (!mustAnalyzeClass)
      return true;
    
    if (skipLoadedMethods(currentMethod))
      return true;
    
    return false;
  }
  
  public boolean mustSkip2(Stack<SootMethod> stack, SootMethod currentMethod) {
    
    String mName = currentMethod.toString().replaceAll("<", "").replaceAll(">", "");
    SootClass mClass = Scene.v().getSootClass(mName.split(":")[0]);
    SootClass handlerClass = Scene.v().getSootClass("android.os.Handler");
    SootClass contentProviderClass = Scene.v().getSootClass("android.content.ContentProvider");
    
    // only keep android.* and com.android.*
    if (stack.size() > 2 && !mName.startsWith("android.") && !mName.startsWith("com.android.")) {
      //logger.info("[filter-android] "+ mName);
      return true;
    }
    
    // skip loaded methods
    if (stack.size() > 2 && skipLoadedMethods(currentMethod))
      return true;
    
    if (stack.size() > 2 && mName.startsWith("android.text")) {
      return true;
    }
    
    // skip method implementing non android. or com.android. interfaces
    if (mustSkipMethodsFromNonAndroidClass(currentMethod, mClass)) {
      //logger.info("[filter-nonAndroidClassMethod] "+ mName);
      return true;
    }
    
    // skip method returning objects
    String returnT = mName.split(": ")[1].split(" ")[0];
    if (stack.size() > 2 && returnT.startsWith("java.lang.Object")) {
      //logger.info("[filter-returnObj] "+ mName);
      return true;
    }
    
    // handler
    if (Util.c1isBelowc2(mClass, handlerClass)) {
      //logger.info("[filter-handler] "+ mName);
      return true;
    }
    
    // content provider
    if (Util.c1isBelowc2(mClass, contentProviderClass)) {
      //logger.info("[filter-contentProv] "+ mName);
      return true;
    }
    
    // run() ...
    if (mustSkipMethodFromThread(mName)) {
      //logger.info("[filter-run] "+ mName);
      return true;
    }
    
    // binder
    // from IBinder.java
    if (mustSkipMethodFromBinder(currentMethod, mClass)) {
      //logger.info("[filter-binder] "+ mName);
      return true;
    }
    
    if (mustSkipContent(currentMethod, mClass)) {
      //logger.info("[filter-contentProv2] "+ mName);
      return true;   
    }
    
    // intents
    if (mustSkipMethodFromIntent(currentMethod, mClass)) {
      //logger.info("[filter-intents] "+ mName);
      return true;
    }
    
    if (mustSkipMethodFromContext(currentMethod, mClass)) {
      //logger.info("[filter-context] "+ mName);
      return true;
    }
    
    // return here!
    if (1 > 0)
      return false;
    
    
    
    
    // BEGIN
    //
    // remove threads
    //
    String[] threadStrings = {
        "Thread",
        "run("
    };
    for (String str: threadStrings) {
      if (mName.contains(str)) {
        return true;
      }
    }    
    


    // ContentService:
    // BroadcastReceiver:
    // Receiver
    // Listener
    
    

    
    // contains Object as parameter
    if (mName.contains("(java.lang.Object") ||
        mName.contains(",java.lang.Object")) {
        return true;
      }
    
    if (mName.contains("Listener:") ||
        mName.contains("Bundle:") ||
        mName.contains("ClassLoader:") ||
        mName.contains(" peek") ||
        mName.contains(" send") || //send Event
        mName.contains("Event(") 
        )  {
      return true;
    }
    
    //
    // remove communication through binder
    //
    String[] binderStrings = {
        // from IBinder.java
        "getInterfaceDescriptor(",
        "pingBinder(",
        "isBinderAlive(",
        "queryLocalInterface(",
        "dump(",
        "transact(",
        "binderDied(",
        "linkToDeath(",
        "unlinkToDeath(",
        // from IInterface.java
        "asBinder(",
        // from Binder.java
        "onTransact(",
        "execTransact(",
        "finalize(",
        "destroy(",
        "sendDeathNotice(",
        // ActivityStack
        "ActivityStack:",
        "Activity:",
        "Handler(", 
        "handleMessage(", 
        "updateLRUListLocked(", 
        "findTaskLocked(", 
        "findActivityLocked(", 
        "startPausingLocked(", 
        "completePauseLocked(", 
        "completeResumeLocked(", 
        "findActivityInHistoryLocked(", 
        "moveActivityToFrontLocked(", 
        "stopActivityLocked(", 
        "removeActivityFromHistoryLocked(", 
        "removeHistoryRecordsForAppLocked(", 
        "finishTaskMoveLocked(",
        // Parcel
        "Parcel: int readExceptionCode(",
        "Parcel: void readException(",
    };
    for (String str: binderStrings) {
      if (mName.contains(str)) {
        return true;
      }
    }
    
    //
    // remove communication through intent
    //
    String[] intentStrings = {
        // from Context.java
        "getTheme(",
        "obtainStyledAttributes(",                                                                                                                                                                     
        "getClassLoader(",
        "getPackageName(",
        "getApplicationInfo(",
        "getPackageResourcePath(",
        "getPackageCodePath(",
        "getSharedPrefsFile(",
        "getSharedPreferences(",
        "openFileInput(",
        "openFileOutput(",
        "deleteFile(",
        "getFileStreamPath(",
        "getFilesDir(",
        "getExternalFilesDir(",
        "getCacheDir(",
        "getExternalCacheDir(",
        "fileList(",
        "getDir(",
        "openOrCreateDatabase(",
        "deleteDatabase(",
        "getDatabasePath(",
        "databaseList(",
        "getWallpaper(",
        "peekWallpaper(",
        "getWallpaperDesiredMinimumWidth(",
        "getWallpaperDesiredMinimumHeight(",
        "setWallpaper(",
        "setWallpaper(",
        "clearWallpaper(",
        "startActivity(",
        "startIntentSender(",
        "sendBroadcast(",
        "sendBroadcast(",
        "sendOrderedBroadcast(",
        "sendOrderedBroadcast(",
        "sendStickyBroadcast(",
        "sendStickyOrderedBroadcast(",
        "removeStickyBroadcast(",
        "registerReceiver(",
        "registerReceiver(",
        "unregisterReceiver(",
        "startService(",
        "stopService(",
        "bindService(",
        "unbindService(",
        "startInstrumentation(",
        "getSystemService(",
        // LocalBreadcastManager
        "handleMessage(",
        "executePendingBroadcasts(",
        "sendBroadcastSync(",
        // Intent
        "FilterComparison(", 
        "Intent(", 
        "addCategory(", 
        "addFlags(", 
        "clone(", 
        "cloneFilter(", 
        "createChooser(", 
        "createFromParcel(",                                                                                                                                                                           
        "describeContents(", 
        "equals(", 
        "fillIn(", 
        "filterEquals(", 
        "filterHashCode(", 
        "fromContext(", 
        "getAction(", 
        "getBooleanArrayExtra(", 
        "getBooleanExtra(", 
        "getBundleExtra(", 
        "getByteArrayExtra(", 
        "getByteExtra(", 
        "getCategories(", 
        "getCharArrayExtra(", 
        "getCharExtra(", 
        "getCharSequenceArrayExtra(", 
        "getCharSequenceArrayListExtra(", 
        "getCharSequenceExtra(", 
        "getComponent(", 
        "getData(", 
        "getDataString(", 
        "getDoubleArrayExtra(", 
        "getDoubleExtra(", 
        "getExtra(", 
        "getExtras(", 
        "getFlags(", 
        "getFloatArrayExtra(", 
        "getFloatExtra(", 
        "getIBinderExtra(", 
        "getIntArrayExtra(", 
        "getIntExtra(", 
        "getIntegerArrayListExtra(", 
        "getIntent(", 
        "getIntent(", 
        "getIntentOld(", 
        "getLongArrayExtra(", 
        "getLongExtra(", 
        "getPackage(", 
        "getParcelableArrayExtra(", 
        "getParcelableArrayListExtra(", 
        "getParcelableExtra(", 
        "getScheme(", 
        "getSerializableExtra(", 
        "getShortArrayExtra(", 
        "getShortExtra(", 
        "getSourceBounds(", 
        "getStringArrayExtra(", 
        "getStringArrayListExtra(", 
        "getStringExtra(", 
        "getType(", 
        "hasCategory(", 
        "hasExtra(", 
        "hasFileDescriptors(", 
        "hashCode(", 
        "newArray(", 
        "newArray(", 
        "parseIntent(", 
        "parseUri(", 
        "putCharSequenceArrayListExtra(", 
        "putExtra(", 
        "putExtras(", 
        "putIntegerArrayListExtra(", 
        "putParcelableArrayListExtra(", 
        "putStringArrayListExtra(", 
        "readFromParcel(", 
        "removeCategory(", 
        "removeExtra(", 
        "replaceExtras(", 
        "resolveActivity(", 
        "resolveActivityInfo(", 
        "resolveType(", 
        "resolveTypeIfNeeded(", 
        "setAction(", 
        "setClass(", 
        "setClassName(", 
        "setComponent(", 
        "setData(", 
        "setDataAndType(", 
        "setExtrasClassLoader(", 
        "setFlags(", 
        "setPackage(", 
        "setSourceBounds(", 
        "setType(", 
        "toShortString(", 
        "toString(", 
        "toURI(", 
        "toUri(", 
        "writeToParcel(",
        // IntentFilter
        "MalformedMimeTypeException(",                                                                                                                                                                 
        "MalformedMimeTypeException(", 
        "create(", 
        "IntentFilter(", 
        "IntentFilter(", 
        "IntentFilter(", 
        "IntentFilter(", 
        "setPriority(", 
        "getPriority(", 
        "addAction(", 
        "countActions(", 
        "getAction(", 
        "hasAction(", 
        "matchAction(", 
        "actionsIterator(", 
        "addDataType(", 
        "hasDataType(", 
        "countDataTypes(", 
        "getDataType(", 
        "typesIterator(", 
        "addDataScheme(", 
        "countDataSchemes(", 
        "getDataScheme(", 
        "hasDataScheme(", 
        "schemesIterator(", 
        "AuthorityEntry(", 
        "getHost(", 
        "getPort(", 
        "match(", 
        "addDataAuthority(", 
        "countDataAuthorities(", 
        "getDataAuthority(", 
        "hasDataAuthority(", 
        "authoritiesIterator(", 
        "addDataPath(", 
        "countDataPaths(", 
        "getDataPath(", 
        "hasDataPath(", 
        "pathsIterator(", 
        "matchDataAuthority(", 
        "matchData(", 
        "addCategory(", 
        "countCategories(", 
        "getCategory(", 
        "hasCategory(", 
        "categoriesIterator(", 
        "matchCategories(", 
        "writeToXml(", 
        "readFromXml(", 
        "dump(", 
        "createFromParcel(", 
        "newArray(", 
        "describeContents(", 
        "writeToParcel(", 
        "debugCheck(", 
        "IntentFilter(", 
        "findMimeType(",        
        // IntentSender
        "SendIntentException(", 
        "SendIntentException(", 
        "SendIntentException(", 
        "run(", 
        "getTargetPackage(", 
        "equals(", 
        "hashCode(", 
        "toString(", 
        "describeContents(", 
        "writeToParcel(", 
        "createFromParcel(", 
        "newArray(", 
        "readIntentSenderOrNullFromParcel(", 
        "getTarget(", 
        "IntentSender(", 
        "IntentSender(",
        // PendingIntent
        "CanceledException(",  
        "run(", 
        "getIntentSender(", 
        "cancel(", 
        "send(",  
        "getTargetPackage(", 
        "equals(", 
        "hashCode(", 
        "toString(", 
        "describeContents(", 
        "writeToParcel(", 
        "createFromParcel(", 
        "newArray(", 
        "readPendingIntentOrNullFromParcel(", 
        "getTarget(",
        // BroadcastReceiver
        "peekService(",
        "onReceive(",
        "finishUpdate(",
        "sendFinished(",
        "setResultCode(",
        "checkSynchronousHint(",
        // IntentResolver
        "IntentResolver:",
        
    };
    for (String str: intentStrings) {
      if (mName.contains(str)) {
        return true;
      }
    }
    

    
    //
    // remove communication through URI and content provider
    //
    String[] uriStrings = {
        // contentservice
        "getCurrentSyncs(",
        "addPeriodicSync(",
        "setSyncAutomatically(",
        "getSyncManager(", 
        "parseId(", 
        "process(", 
        //"<init>(", // big one
        "determineLength(",  
        "resolveValueBackReferences(", 
        "apply(",  
        "write(", 
        "backRefToValue(", 
        "resolveSelectionArgsBackReferences(", 
        "onChange(",
        // from ContentProvider.java
        "ContentProvider:",
        "query(",
        // ContentResolver
        "Random(",                                                                                                                                                                                     
        "ContentResolver(", 
        "acquireProvider(", 
        "acquireExistingProvider(", 
        "releaseProvider(", 
        "getType(", 
        "openInputStream(", 
        "openOutputStream(", 
        "getResourceId(", 
        "modeToMode(", 
        "insert(", 
        "bulkInsert(", 
        "delete(", 
        "acquireProvider(", 
        "acquireExistingProvider(", 
        "acquireProvider(", 
        "acquireContentProviderClient(", 
        "unregisterContentObserver(", 
        "registerContentObserver(",
        "notifyChange(", 
        "notifyChange(", 
        "startSync(", 
        "requestSync(", 
        "validateSyncExtrasBundle(", 
        "cancelSync(", 
        "getSyncAdapterTypes(", 
        "getSyncAutomatically(", 
        "removePeriodicSync(", 
        "getPeriodicSyncs(", 
        "getIsSyncable(", 
        "setIsSyncable(", 
        "getMasterSyncAutomatically(", 
        "setMasterSyncAutomatically(", 
        "isSyncActive(", 
        "getCurrentSync(", 
        "getSyncStatus(", 
        "isSyncPending(", 
        "addStatusChangeListener(", 
        "onStatusChanged(", 
        "removeStatusChangeListener(", 
        "samplePercentForDuration(", 
        "close(", 
        "finalize(", 
        "close(", 
        "finalize(", 
        "getContentService(", 
    };
    for (String str: uriStrings) {
      if (mName.contains(str)) {
        return true;
      }
    }
    
    //
    // remove communication through message, broadcast receiver and other
    //
    String[] messageStrings = {
        // from Message.java
        "Message:",
        "MessageQueue",
        "send(", 
        
        // from Application.java
        "onCreate(",
        "onTerminate(",
        "onConfigurationChanged(",
        "onLowMemory(",
        
        // from Parcelable.java
        "describeContents(",
        "writeToParcel(",
        "createFromParcel(",
        "newArray(",
        
        // BroadcastReceiver
        // ?
        
        // ActivityManager
        // ?
        
        // ServiceManager
        "IBinder>(", 
        "getIServiceManager(", 
        "getService(", 
        "addService(", 
        "checkService(", 
        "listServices(", 
        "initServiceCache(",
        
        // SQLite*.java
        "ActiveDatabases(", 
        "yieldIfContendedHelper(", 
        "native_column_count(", 
        "native_1x1_long(", 
        "native_1x1_string(", 
        "native_column_name(", 
        "native_clear_bindings(", 
        "native_compile(", 
        "native_execute(", 
        "native_finalize(", 
        "Random(", 
        "ReentrantLock(", 
        "native_getDbLookaside(", 
        "dbclose(", 
        "dbopen(", 
        "enableSqlProfiling(", 
        "enableSqlTracing(", 
        "Object(", 
        "SQLiteDatabase(", 
        "getAttachedDbs(", 
        "ActiveDatabases(", 
        "Pattern.compile(", 
        "SQLiteOpenHelper.class.getSimpleName(", 
        "getPragmaVal(", 
        "appendClauseEscapeClause(", 
        "appendClause(", 
        "computeProjection(", 
        "getObjInfo(", 
        "getPathForLogs(", 
        "getTime(", 
        "checkLockHoldTime(", 
        "closeClosable(", 
        "compile(",                                                                                                                                                                                    
        "deactivateCommon(", 
        "deallocCachedSqlStatements(", 
        "lockForced(", 
        "queryThreadLock(", 
        "queryThreadUnlock(", 
        "releaseCompiledSqlIfNotInCache(", 
        "sendMessage(", 
        "unlockForced(", 
        "onAllReferencesReleased(", 
        "native_bind_blob(", 
        "native_bind_double(", 
        "native_bind_long(", 
        "native_bind_null(", 
        "native_bind_string(", 
        "native_compile(", 
        "native_finalize(", 
        "compile(",
        "finalize(", 
        "finalize(", 
        "onAllReferencesReleased(", 
        "onAllReferencesReleasedFromContainer(", 
        "onAllReferencesReleasedFromContainer(", 
        "onCreate(", 
        "onUpgrade(", 
        "deleteRow(", 
        "inTransaction(", 
        "isDbLockedByCurrentThread(", 
        "isDbLockedByOtherThreads(", 
        "isInCompiledSqlCache(", 
        "isOpen(", 
        "isReadOnly(", 
        "needUpgrade(", 
        "onMove(", 
        "requery(", 
        "supportsUpdates(", 
        "yieldIfContended(", 
        "yieldIfContendedSafely(", 
        "yieldIfContendedSafely(", 
        "query(", 
        "rawQuery(", 
        "DbStats(", 
        "getUniqueId(", 
        "getPath(", 
        "delete(", 
        "getColumnIndex(", 
        "getCount(", 
        "getVersion(", 
        "update(", 
        "executeInsert(", 
        "getMaximumSize(", 
        "getPageSize(", 
        "insertOrThrow(", 
        "insert(", 
        "replace(", 
        "setMaximumSize(", 
        "simpleQueryForLong(", 
        "getSyncedTables(", 
        "SQLiteAbortException(", 
        "SQLiteAbortException(", 
        "SQLiteConstraintException(", 
        "SQLiteConstraintException(", 
        "SQLiteDatabaseCorruptException(", 
        "SQLiteDatabaseCorruptException(", 
        "getDatabase(",                                                                                                                                                                                
        "SQLiteDirectCursorDriver(", 
        "SQLiteDiskIOException(", 
        "SQLiteDiskIOException(", 
        "SQLiteDoneException(", 
        "SQLiteDoneException(", 
        "SQLiteException(", 
        "SQLiteException(", 
        "SQLiteFullException(", 
        "SQLiteFullException(", 
        "SQLiteMisuseException(", 
        "SQLiteMisuseException(", 
        "SQLiteOpenHelper(", 
        "SQLiteQueryBuilder(", 
        "compileStatement(", 
        "getNumActiveCursorsFinalized(", 
        "getHeapAllocatedSize(", 
        "getHeapFreeSize(", 
        "getHeapSize(", 
        "getHeapDirtyPages(", 
        "getPagerStats(", 
        "getDatabaseInfo(", 
        "create(", 
        "openDatabase(", 
        "openOrCreateDatabase(", 
        "openOrCreateDatabase(", 
        "findEditTable(", 
        "appendColumns(", 
        "buildUnionQuery(", 
        "getColumnNames(", 
        "getTables(", 
        "simpleQueryForString(", 
        "toString(", 
        "getMaxSqlCacheSize(", 
        "getReadableDatabase(", 
        "getWritableDatabase(", 
        "close(", 
        "setMaxSqlCacheSize(", 
        "acquireReference(", 
        "appendWhere(", 
        "appendWhereEscapeString(", 
        "beginTransaction(", 
        "beginTransactionWithListener(", 
        "bindBlob(", 
        "bindDouble(",                                                                                                                                                                                 
        "bindLong(", 
        "bindNull(", 
        "bindString(", 
        "clearBindings(", 
        "close(", 
        "cursorClosed(", 
        "cursorDeactivated(", 
        "cursorRequeried(", 
        "deactivate(", 
        "endTransaction(", 
        "execSQL(", 
        "execSQL(", 
        "execute(", 
        "handleMessage(", 
        "markTableSyncable(", 
        "onOpen(", 
        "purgeFromCompiledSqlCache(", 
        "registerDataSetObserver(", 
        "releaseReference(", 
        "releaseReferenceFromContainer(", 
        "resetCompiledSqlCache(", 
        "run(", 
        "setBindArguments(", 
        "setBindArguments(", 
        "setCursorFactory(", 
        "setDistinct(", 
        "setLoadStyle(", 
        "setLocale(", 
        "setLockingEnabled(", 
        "setPageSize(", 
        "setProjectionMap(", 
        "setSelectionArguments(", 
        "setStrictProjectionMap(", 
        "setTables(", 
        "setTransactionSuccessful(", 
        "setVersion(", 
        "setWindow(", 
        "releaseMemory(",
        
        // SQLiteContentProvider
        "ThreadLocal<Boolean>(", 
        "onCreate(", 
        "getDatabaseHelper(", 
        "insertInTransaction(", 
        "deleteInTransaction(", 
        "notifyChange(", 
        "getDatabaseHelper(", 
        "applyingBatch(", 
        "insert(", 
        "bulkInsert(", 
        "update(", 
        "delete(", 
        "applyBatch(", 
        "onBegin(", 
        "onCommit(", 
        "onRollback(", 
        "onBeginTransaction(", 
        "beforeTransactionCommit(", 
        "onEndTransaction(",
        
        // Cursor
        "android.database", // big package
        
    };
    for (String str: messageStrings) {
      if (mName.contains(str)) {
        return true;
      }
    }
    
    //
    // remove communication through handler
    //
    String[] handlerStrings = {
        // from Handler.*.java
        "Handler:",
        "HandlerThread:",
        "HandlerCaller:",
        "post(",
        "postAtTime(",
        "postAtTime(",
        "postDelayed(",
        "postAtFrontOfQueue(",
        "removeCallbacks(",
        "removeCallbacks(",
        "sendMessage(",
        "sendEmptyMessage(",
        "sendEmptyMessageDelayed(",
        "sendEmptyMessageAtTime(",
        "sendMessageDelayed(",
        "sendMessageAtTime(",
        "sendMessageAtFrontOfQueue(",
        "removeMessages(",
        "removeMessages(",
        "removeCallbacksAndMessages(",
        "hasMessages(",
        "hasMessages(",
        "getLooper(",
        "dump(",
        "toString(",
        "send(", 
    };
    for (String str: handlerStrings) {
      if (mName.contains(str)) {
        return true;
      }
    }
    // ENDS
    
    
    
//    if (mName.contains(" query(")) {
//      logger.info("skipping query: "+ m);
//      return;
//    }
//    
//    if (mName.contains(" run()")||
//        mName.contains(" handleMessage(") ||
//        mName.contains(" onTransact(") ||
//        mName.contains(" sendIntent(") ||
//        mName.contains("android.app.PendingIntent: void send(") ||
//        mName.contains("android.app.Activity: void performResume(") ||
//        mName.contains("ActivityStack") ||
//        mName.contains("onReceive(") || // to remove, for broadcast receivers
//        mName.contains("android.app.LocalActivityManager: void dispatchResume()") ||
//        mName.contains("android.app.ActivityGroup: void onStop()") ||
//        mName.contains("startIntentSender(") ||
//        mName.contains("bindService(") || 
//        mName.contains("startService(") ||
//        mName.contains("startInstrumentation")
//    ) {
//      logger.info("skipping run()/handleMessage/onTransact: "+ m);
//      return;
//    }
//    
//    if (mName.startsWith("java.") || 
//        mName.startsWith("libcore.") ||
//        mName.startsWith("org.apache.harmony.") ||
//        mName.startsWith("org.apache.") ||
//        mName.startsWith("android.text.") ||
//        mName.startsWith("android.os.") ||
//        mName.startsWith("android.net.Uri") ||
//        mName.startsWith("android.net.NetworkInfo") ||
//        mName.startsWith("android.database.") ||
//        mName.startsWith("android.os.Parcel") ||
//        mName.startsWith("android.os.Bundle") ||
//        mName.startsWith("android.graphics") ||
//        mName.startsWith("android.support") ||
//        mName.startsWith("android.view") ||
//        mName.startsWith("android.widget") ||
//        mName.startsWith("android.os.Binder") ||
//        mName.startsWith("android.content.ContentResolver") ||
//        //mName.startsWith("com.android.internal") || // ISms is inside here
//        mName.startsWith("android.webkit") ||
//        mName.startsWith("android.test") ||
//        mName.startsWith("android.opengl") ||
//        mName.startsWith("android.content.res") ||
//        mName.startsWith("android.util") ||
//        mName.startsWith("android.animation") ||
//        mName.contains(": java.lang.String") ||
//        
//        mName.contains("boolean equals") ||
//        mName.contains("boolean equal") ||
//        mName.contains("boolean compare") ||
//        mName.contains("createFromParcel(") ||
//        mName.contains("void writeToParcel") ||
//        mName.contains("Drawable getCachedIcon") ||
//        mName.contains("Drawable getDrawable") ||
//        mName.contains("AccountAuthenticatorCache:") ||
//        mName.contains("Info:") || // ResultInfo, PackageInfo
//        mName.contains("Cache:") || // RegisteredServicesCache
//        mName.contains("android.server.search.Searchables:") ||
//        mName.contains("Drawable loadIcon") ||
//        mName.contains("android.app.Dialog:") ||
//        mName.contains("android.content.res.XmlResourceParser getXml(") ||
//        mName.contains("android.app.SearchDialog:") ||
//        mName.contains(" android.content.res.Resources ") ||
//        mName.contains("android.app.LoadedApk: void <init>(") ||
//        mName.contains("getSystemContext()") ||
//        mName.contains(": java.lang.ClassLoader getClassLoader()") ||
//        mName.contains("registerReceiver(") ||
//        mName.contains("getApplicationInfo(") ||
//        mName.contains("getPackageInfo(") ||
//        mName.contains("getActivityInfo(") ||
//        mName.contains("boolean isPackage") ||
//        mName.contains("void finish()") ||
//        mName.contains("ResumeActivity") ||
//        //mName.contains("")
//        
//        
//        mName.contains("toString") ||
//        mName.contains("getString") ||
//        mName.startsWith("sun.")) { // ||
//      ////        mName.indexOf(".") != -1 && (
//      ////            !mName.startsWith("android.") &&
//      ////            !mName.startsWith("com.google") &&
//      ////            !mName.startsWith("com.android")) ) { 
////      logger.info("[not important method] "+ m); 
//      return; 
//    }
    return false;
  }

  private boolean mustSkipMethodFromContext(SootMethod m, SootClass mClass) {
    SootClass contextClass = Scene.v().getSootClass("android.content.Context");
    if (Util.c1isBelowc2(mClass, contextClass) && Util.methodisOneOfClass(m, contextClass)) {
      if (m.toString().contains("check"))
        return false;
      return true;
    }
    return false;
  }

  private boolean mustSkipContent(SootMethod m, SootClass mClass) {
    SootClass contentObserverClass = Scene.v().getSootClass("android.database.ContentObserver");
    SootClass dataSetObserverClass = Scene.v().getSootClass("android.database.DataSetObserver");
    SootClass iContentObserverClass = Scene.v().getSootClass("android.database.IContentObserver");
    SootClass observableClass = Scene.v().getSootClass("android.database.Observable");    
    SootClass cursorClass = Scene.v().getSootClass("android.database.Cursor");
    SootClass iBulkCursorClass = Scene.v().getSootClass("android.database.IBulkCursor");
    
    
    
    if (Util.c1isBelowc2(mClass, contentObserverClass) && Util.methodisOneOfClass(m, contentObserverClass)) {
      return true;
    }
    if (Util.c1isBelowc2(mClass, iContentObserverClass) && Util.methodisOneOfClass(m, iContentObserverClass)) {
      return true;
    }
    if (Util.c1isBelowc2(mClass, dataSetObserverClass) && Util.methodisOneOfClass(m, dataSetObserverClass)) {
      return true;
    }
    if (Util.c1isBelowc2(mClass, observableClass) && Util.methodisOneOfClass(m, observableClass)) {
      return true;
    }
    
    if (Util.c1isBelowc2(mClass, cursorClass) && Util.methodisOneOfClass(m, cursorClass)) {
      return true;
    }
    if (Util.c1isBelowc2(mClass, iBulkCursorClass) && Util.methodisOneOfClass(m, iBulkCursorClass)) {
      return true;
    }
    
    SootClass AbstractThreadedSyncAdapterClass = Scene.v().getSootClass("android.content.AbstractThreadedSyncAdapter"); 
    if (Util.c1isBelowc2(mClass, AbstractThreadedSyncAdapterClass) && Util.methodisOneOfClass(m, AbstractThreadedSyncAdapterClass)) { return true; } 
   SootClass ActivityNotFoundExceptionClass = Scene.v().getSootClass("android.content.ActivityNotFoundException"); 
    if (Util.c1isBelowc2(mClass, ActivityNotFoundExceptionClass) && Util.methodisOneOfClass(m, ActivityNotFoundExceptionClass)) { return true; } 
   SootClass AsyncQueryHandlerClass = Scene.v().getSootClass("android.content.AsyncQueryHandler"); 
    if (Util.c1isBelowc2(mClass, AsyncQueryHandlerClass) && Util.methodisOneOfClass(m, AsyncQueryHandlerClass)) { return true; } 
   SootClass BroadcastReceiverClass = Scene.v().getSootClass("android.content.BroadcastReceiver"); 
    if (Util.c1isBelowc2(mClass, BroadcastReceiverClass) && Util.methodisOneOfClass(m, BroadcastReceiverClass)) { return true; } 
   SootClass ComponentCallbacksClass = Scene.v().getSootClass("android.content.ComponentCallbacks"); 
    if (Util.c1isBelowc2(mClass, ComponentCallbacksClass) && Util.methodisOneOfClass(m, ComponentCallbacksClass)) { return true; } 
   SootClass ComponentNameClass = Scene.v().getSootClass("android.content.ComponentName"); 
    if (Util.c1isBelowc2(mClass, ComponentNameClass) && Util.methodisOneOfClass(m, ComponentNameClass)) { return true; }                                                                          
   SootClass ContentInsertHandlerClass = Scene.v().getSootClass("android.content.ContentInsertHandler"); 
    if (Util.c1isBelowc2(mClass, ContentInsertHandlerClass) && Util.methodisOneOfClass(m, ContentInsertHandlerClass)) { return true; } 
   SootClass ContentProviderClass = Scene.v().getSootClass("android.content.ContentProvider"); 
    if (Util.c1isBelowc2(mClass, ContentProviderClass) && Util.methodisOneOfClass(m, ContentProviderClass)) { return true; } 
   SootClass ContentProviderClientClass = Scene.v().getSootClass("android.content.ContentProviderClient"); 
    if (Util.c1isBelowc2(mClass, ContentProviderClientClass) && Util.methodisOneOfClass(m, ContentProviderClientClass)) { return true; } 
   SootClass ContentProviderNativeClass = Scene.v().getSootClass("android.content.ContentProviderNative"); 
    if (Util.c1isBelowc2(mClass, ContentProviderNativeClass) && Util.methodisOneOfClass(m, ContentProviderNativeClass)) { return true; } 
   SootClass ContentProviderOperationClass = Scene.v().getSootClass("android.content.ContentProviderOperation"); 
    if (Util.c1isBelowc2(mClass, ContentProviderOperationClass) && Util.methodisOneOfClass(m, ContentProviderOperationClass)) { return true; } 
   SootClass ContentProviderResultClass = Scene.v().getSootClass("android.content.ContentProviderResult"); 
    if (Util.c1isBelowc2(mClass, ContentProviderResultClass) && Util.methodisOneOfClass(m, ContentProviderResultClass)) { return true; } 
   SootClass ContentQueryMapClass = Scene.v().getSootClass("android.content.ContentQueryMap"); 
    if (Util.c1isBelowc2(mClass, ContentQueryMapClass) && Util.methodisOneOfClass(m, ContentQueryMapClass)) { return true; } 
   SootClass ContentResolverClass = Scene.v().getSootClass("android.content.ContentResolver"); 
    if (Util.c1isBelowc2(mClass, ContentResolverClass) && Util.methodisOneOfClass(m, ContentResolverClass)) { return true; } 
   SootClass ContentServiceClass = Scene.v().getSootClass("android.content.ContentService"); 
    if (Util.c1isBelowc2(mClass, ContentServiceClass) && Util.methodisOneOfClass(m, ContentServiceClass)) { return true; } 
   SootClass ContentUrisClass = Scene.v().getSootClass("android.content.ContentUris"); 
    if (Util.c1isBelowc2(mClass, ContentUrisClass) && Util.methodisOneOfClass(m, ContentUrisClass)) { return true; } 
   SootClass ContentValuesClass = Scene.v().getSootClass("android.content.ContentValues"); 
    if (Util.c1isBelowc2(mClass, ContentValuesClass) && Util.methodisOneOfClass(m, ContentValuesClass)) { return true; } 
   SootClass ContextClass = Scene.v().getSootClass("android.content.Context"); 
    if (Util.c1isBelowc2(mClass, ContextClass) && Util.methodisOneOfClass(m, ContextClass)) { return true; } 
   SootClass ContextWrapperClass = Scene.v().getSootClass("android.content.ContextWrapper"); 
    if (Util.c1isBelowc2(mClass, ContextWrapperClass) && Util.methodisOneOfClass(m, ContextWrapperClass)) { return true; } 
   SootClass CursorEntityIteratorClass = Scene.v().getSootClass("android.content.CursorEntityIterator"); 
    if (Util.c1isBelowc2(mClass, CursorEntityIteratorClass) && Util.methodisOneOfClass(m, CursorEntityIteratorClass)) { return true; } 
   SootClass DefaultDataHandlerClass = Scene.v().getSootClass("android.content.DefaultDataHandler"); 
    if (Util.c1isBelowc2(mClass, DefaultDataHandlerClass) && Util.methodisOneOfClass(m, DefaultDataHandlerClass)) { return true; } 
   SootClass DialogInterfaceClass = Scene.v().getSootClass("android.content.DialogInterface"); 
    if (Util.c1isBelowc2(mClass, DialogInterfaceClass) && Util.methodisOneOfClass(m, DialogInterfaceClass)) { return true; } 
   SootClass EntityClass = Scene.v().getSootClass("android.content.Entity"); 
    if (Util.c1isBelowc2(mClass, EntityClass) && Util.methodisOneOfClass(m, EntityClass)) { return true; } 
   SootClass EntityIteratorClass = Scene.v().getSootClass("android.content.EntityIterator"); 
    if (Util.c1isBelowc2(mClass, EntityIteratorClass) && Util.methodisOneOfClass(m, EntityIteratorClass)) { return true; } 
   SootClass EventLogTagsClass = Scene.v().getSootClass("android.content.EventLogTags"); 
    if (Util.c1isBelowc2(mClass, EventLogTagsClass) && Util.methodisOneOfClass(m, EventLogTagsClass)) { return true; } 
   SootClass IContentProviderClass = Scene.v().getSootClass("android.content.IContentProvider"); 
   if (Util.c1isBelowc2(mClass, IContentProviderClass) && Util.methodisOneOfClass(m, IContentProviderClass)) { return true; }
   SootClass IContentServiceClass = Scene.v().getSootClass("android.content.IContentService");
    if (Util.c1isBelowc2(mClass, IContentServiceClass) && Util.methodisOneOfClass(m, IContentServiceClass)) { return true; }
   SootClass IIntentReceiverClass = Scene.v().getSootClass("android.content.IIntentReceiver");                                                                                                    
    if (Util.c1isBelowc2(mClass, IIntentReceiverClass) && Util.methodisOneOfClass(m, IIntentReceiverClass)) { return true; }
   SootClass IIntentSenderClass = Scene.v().getSootClass("android.content.IIntentSender");
    if (Util.c1isBelowc2(mClass, IIntentSenderClass) && Util.methodisOneOfClass(m, IIntentSenderClass)) { return true; }
   SootClass ISyncAdapterClass = Scene.v().getSootClass("android.content.ISyncAdapter");
    if (Util.c1isBelowc2(mClass, ISyncAdapterClass) && Util.methodisOneOfClass(m, ISyncAdapterClass)) { return true; }
   SootClass ISyncContextClass = Scene.v().getSootClass("android.content.ISyncContext");
    if (Util.c1isBelowc2(mClass, ISyncContextClass) && Util.methodisOneOfClass(m, ISyncContextClass)) { return true; }
   SootClass ISyncStatusObserverClass = Scene.v().getSootClass("android.content.ISyncStatusObserver");
    if (Util.c1isBelowc2(mClass, ISyncStatusObserverClass) && Util.methodisOneOfClass(m, ISyncStatusObserverClass)) { return true; }
   SootClass IntentClass = Scene.v().getSootClass("android.content.Intent");
    if (Util.c1isBelowc2(mClass, IntentClass) && Util.methodisOneOfClass(m, IntentClass)) { return true; }
   SootClass IntentFilterClass = Scene.v().getSootClass("android.content.IntentFilter");
    if (Util.c1isBelowc2(mClass, IntentFilterClass) && Util.methodisOneOfClass(m, IntentFilterClass)) { return true; }
   SootClass IntentSenderClass = Scene.v().getSootClass("android.content.IntentSender");
    if (Util.c1isBelowc2(mClass, IntentSenderClass) && Util.methodisOneOfClass(m, IntentSenderClass)) { return true; }
   SootClass MutableContextWrapperClass = Scene.v().getSootClass("android.content.MutableContextWrapper");
    if (Util.c1isBelowc2(mClass, MutableContextWrapperClass) && Util.methodisOneOfClass(m, MutableContextWrapperClass)) { return true; }
   SootClass OperationApplicationExceptionClass = Scene.v().getSootClass("android.content.OperationApplicationException");
    if (Util.c1isBelowc2(mClass, OperationApplicationExceptionClass) && Util.methodisOneOfClass(m, OperationApplicationExceptionClass)) { return true; }
   SootClass PeriodicSyncClass = Scene.v().getSootClass("android.content.PeriodicSync");
    if (Util.c1isBelowc2(mClass, PeriodicSyncClass) && Util.methodisOneOfClass(m, PeriodicSyncClass)) { return true; }
   SootClass ReceiverCallNotAllowedExceptionClass = Scene.v().getSootClass("android.content.ReceiverCallNotAllowedException");
    if (Util.c1isBelowc2(mClass, ReceiverCallNotAllowedExceptionClass) && Util.methodisOneOfClass(m, ReceiverCallNotAllowedExceptionClass)) { return true; }
   SootClass SearchRecentSuggestionsProviderClass = Scene.v().getSootClass("android.content.SearchRecentSuggestionsProvider");
    if (Util.c1isBelowc2(mClass, SearchRecentSuggestionsProviderClass) && Util.methodisOneOfClass(m, SearchRecentSuggestionsProviderClass)) { return true; }
   SootClass ServiceConnectionClass = Scene.v().getSootClass("android.content.ServiceConnection");
    if (Util.c1isBelowc2(mClass, ServiceConnectionClass) && Util.methodisOneOfClass(m, ServiceConnectionClass)) { return true; }
   SootClass SharedPreferencesClass = Scene.v().getSootClass("android.content.SharedPreferences");
    if (Util.c1isBelowc2(mClass, SharedPreferencesClass) && Util.methodisOneOfClass(m, SharedPreferencesClass)) { return true; }
   SootClass SyncAdapterTypeClass = Scene.v().getSootClass("android.content.SyncAdapterType");
    if (Util.c1isBelowc2(mClass, SyncAdapterTypeClass) && Util.methodisOneOfClass(m, SyncAdapterTypeClass)) { return true; }
   SootClass SyncAdaptersCacheClass = Scene.v().getSootClass("android.content.SyncAdaptersCache");
    if (Util.c1isBelowc2(mClass, SyncAdaptersCacheClass) && Util.methodisOneOfClass(m, SyncAdaptersCacheClass)) { return true; }
   SootClass SyncContextClass = Scene.v().getSootClass("android.content.SyncContext");
    if (Util.c1isBelowc2(mClass, SyncContextClass) && Util.methodisOneOfClass(m, SyncContextClass)) { return true; }
   SootClass SyncInfoClass = Scene.v().getSootClass("android.content.SyncInfo");
    if (Util.c1isBelowc2(mClass, SyncInfoClass) && Util.methodisOneOfClass(m, SyncInfoClass)) { return true; }
   SootClass SyncManagerClass = Scene.v().getSootClass("android.content.SyncManager");
    if (Util.c1isBelowc2(mClass, SyncManagerClass) && Util.methodisOneOfClass(m, SyncManagerClass)) { return true; }
   SootClass SyncOperationClass = Scene.v().getSootClass("android.content.SyncOperation");
    if (Util.c1isBelowc2(mClass, SyncOperationClass) && Util.methodisOneOfClass(m, SyncOperationClass)) { return true; }
   SootClass SyncQueueClass = Scene.v().getSootClass("android.content.SyncQueue");
    if (Util.c1isBelowc2(mClass, SyncQueueClass) && Util.methodisOneOfClass(m, SyncQueueClass)) { return true; }
   SootClass SyncResultClass = Scene.v().getSootClass("android.content.SyncResult");
    if (Util.c1isBelowc2(mClass, SyncResultClass) && Util.methodisOneOfClass(m, SyncResultClass)) { return true; }
   SootClass SyncStatsClass = Scene.v().getSootClass("android.content.SyncStats");
    if (Util.c1isBelowc2(mClass, SyncStatsClass) && Util.methodisOneOfClass(m, SyncStatsClass)) { return true; }
   SootClass SyncStatusInfoClass = Scene.v().getSootClass("android.content.SyncStatusInfo");
    if (Util.c1isBelowc2(mClass, SyncStatusInfoClass) && Util.methodisOneOfClass(m, SyncStatusInfoClass)) { return true; }
   SootClass SyncStatusObserverClass = Scene.v().getSootClass("android.content.SyncStatusObserver");
    if (Util.c1isBelowc2(mClass, SyncStatusObserverClass) && Util.methodisOneOfClass(m, SyncStatusObserverClass)) { return true; }
   SootClass SyncStorageEngineClass = Scene.v().getSootClass("android.content.SyncStorageEngine");
    if (Util.c1isBelowc2(mClass, SyncStorageEngineClass) && Util.methodisOneOfClass(m, SyncStorageEngineClass)) { return true; }
   SootClass UriMatcherClass = Scene.v().getSootClass("android.content.UriMatcher");
    if (Util.c1isBelowc2(mClass, UriMatcherClass) && Util.methodisOneOfClass(m, UriMatcherClass)) { return true; } 
    
    
    SootClass AssetFileDescriptorClass = Scene.v().getSootClass("android.content.res.AssetFileDescriptor"); 
    if (Util.c1isBelowc2(mClass, AssetFileDescriptorClass) && Util.methodisOneOfClass(m, AssetFileDescriptorClass)) { return true; } 
   SootClass AssetManagerClass = Scene.v().getSootClass("android.content.res.AssetManager"); 
    if (Util.c1isBelowc2(mClass, AssetManagerClass) && Util.methodisOneOfClass(m, AssetManagerClass)) { return true; } 
   SootClass ColorStateListClass = Scene.v().getSootClass("android.content.res.ColorStateList"); 
    if (Util.c1isBelowc2(mClass, ColorStateListClass) && Util.methodisOneOfClass(m, ColorStateListClass)) { return true; } 
   SootClass CompatibilityInfoClass = Scene.v().getSootClass("android.content.res.CompatibilityInfo"); 
    if (Util.c1isBelowc2(mClass, CompatibilityInfoClass) && Util.methodisOneOfClass(m, CompatibilityInfoClass)) { return true; } 
   SootClass ConfigurationClass = Scene.v().getSootClass("android.content.res.Configuration"); 
    if (Util.c1isBelowc2(mClass, ConfigurationClass) && Util.methodisOneOfClass(m, ConfigurationClass)) { return true; } 
   SootClass ObbInfoClass = Scene.v().getSootClass("android.content.res.ObbInfo"); 
    if (Util.c1isBelowc2(mClass, ObbInfoClass) && Util.methodisOneOfClass(m, ObbInfoClass)) { return true; } 
   SootClass ObbScannerClass = Scene.v().getSootClass("android.content.res.ObbScanner"); 
    if (Util.c1isBelowc2(mClass, ObbScannerClass) && Util.methodisOneOfClass(m, ObbScannerClass)) { return true; } 
   SootClass PluralRulesClass = Scene.v().getSootClass("android.content.res.PluralRules"); 
    if (Util.c1isBelowc2(mClass, PluralRulesClass) && Util.methodisOneOfClass(m, PluralRulesClass)) { return true; } 
   SootClass ResourcesClass = Scene.v().getSootClass("android.content.res.Resources"); 
    if (Util.c1isBelowc2(mClass, ResourcesClass) && Util.methodisOneOfClass(m, ResourcesClass)) { return true; } 
   SootClass StringBlockClass = Scene.v().getSootClass("android.content.res.StringBlock"); 
    if (Util.c1isBelowc2(mClass, StringBlockClass) && Util.methodisOneOfClass(m, StringBlockClass)) { return true; } 
   SootClass TypedArrayClass = Scene.v().getSootClass("android.content.res.TypedArray"); 
    if (Util.c1isBelowc2(mClass, TypedArrayClass) && Util.methodisOneOfClass(m, TypedArrayClass)) { return true; } 
   SootClass XmlBlockClass = Scene.v().getSootClass("android.content.res.XmlBlock"); 
    if (Util.c1isBelowc2(mClass, XmlBlockClass) && Util.methodisOneOfClass(m, XmlBlockClass)) { return true; } 
   SootClass XmlResourceParserClass = Scene.v().getSootClass("android.content.res.XmlResourceParser"); 
    if (Util.c1isBelowc2(mClass, XmlResourceParserClass) && Util.methodisOneOfClass(m, XmlResourceParserClass)) { return true; }
    
    
    SootClass ActivityInfoClass = Scene.v().getSootClass("android.content.pm.ActivityInfo");                                                                                                       
    if (Util.c1isBelowc2(mClass, ActivityInfoClass) && Util.methodisOneOfClass(m, ActivityInfoClass)) { return true; } 
   SootClass ApplicationInfoClass = Scene.v().getSootClass("android.content.pm.ApplicationInfo"); 
    if (Util.c1isBelowc2(mClass, ApplicationInfoClass) && Util.methodisOneOfClass(m, ApplicationInfoClass)) { return true; } 
   SootClass ComponentInfoClass = Scene.v().getSootClass("android.content.pm.ComponentInfo"); 
    if (Util.c1isBelowc2(mClass, ComponentInfoClass) && Util.methodisOneOfClass(m, ComponentInfoClass)) { return true; } 
   SootClass ConfigurationInfoClass = Scene.v().getSootClass("android.content.pm.ConfigurationInfo"); 
    if (Util.c1isBelowc2(mClass, ConfigurationInfoClass) && Util.methodisOneOfClass(m, ConfigurationInfoClass)) { return true; } 
   SootClass FeatureInfoClass = Scene.v().getSootClass("android.content.pm.FeatureInfo"); 
    if (Util.c1isBelowc2(mClass, FeatureInfoClass) && Util.methodisOneOfClass(m, FeatureInfoClass)) { return true; } 
   SootClass IPackageDataObserverClass = Scene.v().getSootClass("android.content.pm.IPackageDataObserver"); 
    if (Util.c1isBelowc2(mClass, IPackageDataObserverClass) && Util.methodisOneOfClass(m, IPackageDataObserverClass)) { return true; } 
   SootClass IPackageDeleteObserverClass = Scene.v().getSootClass("android.content.pm.IPackageDeleteObserver"); 
    if (Util.c1isBelowc2(mClass, IPackageDeleteObserverClass) && Util.methodisOneOfClass(m, IPackageDeleteObserverClass)) { return true; } 
   SootClass IPackageInstallObserverClass = Scene.v().getSootClass("android.content.pm.IPackageInstallObserver"); 
    if (Util.c1isBelowc2(mClass, IPackageInstallObserverClass) && Util.methodisOneOfClass(m, IPackageInstallObserverClass)) { return true; } 
   SootClass IPackageManagerClass = Scene.v().getSootClass("android.content.pm.IPackageManager"); 
    if (Util.c1isBelowc2(mClass, IPackageManagerClass) && Util.methodisOneOfClass(m, IPackageManagerClass)) { return true; } 
   SootClass IPackageMoveObserverClass = Scene.v().getSootClass("android.content.pm.IPackageMoveObserver"); 
    if (Util.c1isBelowc2(mClass, IPackageMoveObserverClass) && Util.methodisOneOfClass(m, IPackageMoveObserverClass)) { return true; } 
   SootClass IPackageStatsObserverClass = Scene.v().getSootClass("android.content.pm.IPackageStatsObserver"); 
    if (Util.c1isBelowc2(mClass, IPackageStatsObserverClass) && Util.methodisOneOfClass(m, IPackageStatsObserverClass)) { return true; } 
   SootClass InstrumentationInfoClass = Scene.v().getSootClass("android.content.pm.InstrumentationInfo"); 
    if (Util.c1isBelowc2(mClass, InstrumentationInfoClass) && Util.methodisOneOfClass(m, InstrumentationInfoClass)) { return true; } 
   SootClass LabeledIntentClass = Scene.v().getSootClass("android.content.pm.LabeledIntent"); 
    if (Util.c1isBelowc2(mClass, LabeledIntentClass) && Util.methodisOneOfClass(m, LabeledIntentClass)) { return true; } 
   SootClass PackageInfoClass = Scene.v().getSootClass("android.content.pm.PackageInfo"); 
    if (Util.c1isBelowc2(mClass, PackageInfoClass) && Util.methodisOneOfClass(m, PackageInfoClass)) { return true; } 
   SootClass PackageInfoLiteClass = Scene.v().getSootClass("android.content.pm.PackageInfoLite"); 
    if (Util.c1isBelowc2(mClass, PackageInfoLiteClass) && Util.methodisOneOfClass(m, PackageInfoLiteClass)) { return true; } 
   SootClass PackageItemInfoClass = Scene.v().getSootClass("android.content.pm.PackageItemInfo"); 
    if (Util.c1isBelowc2(mClass, PackageItemInfoClass) && Util.methodisOneOfClass(m, PackageItemInfoClass)) { return true; } 
   SootClass PackageManagerClass = Scene.v().getSootClass("android.content.pm.PackageManager"); 
    if (Util.c1isBelowc2(mClass, PackageManagerClass) && Util.methodisOneOfClass(m, PackageManagerClass)) { return true; } 
   SootClass PackageParserClass = Scene.v().getSootClass("android.content.pm.PackageParser"); 
    if (Util.c1isBelowc2(mClass, PackageParserClass) && Util.methodisOneOfClass(m, PackageParserClass)) { return true; } 
   SootClass PackageStatsClass = Scene.v().getSootClass("android.content.pm.PackageStats"); 
    if (Util.c1isBelowc2(mClass, PackageStatsClass) && Util.methodisOneOfClass(m, PackageStatsClass)) { return true; } 
   SootClass PathPermissionClass = Scene.v().getSootClass("android.content.pm.PathPermission"); 
    if (Util.c1isBelowc2(mClass, PathPermissionClass) && Util.methodisOneOfClass(m, PathPermissionClass)) { return true; } 
   SootClass PermissionGroupInfoClass = Scene.v().getSootClass("android.content.pm.PermissionGroupInfo"); 
    if (Util.c1isBelowc2(mClass, PermissionGroupInfoClass) && Util.methodisOneOfClass(m, PermissionGroupInfoClass)) { return true; } 
   SootClass PermissionInfoClass = Scene.v().getSootClass("android.content.pm.PermissionInfo"); 
    if (Util.c1isBelowc2(mClass, PermissionInfoClass) && Util.methodisOneOfClass(m, PermissionInfoClass)) { return true; } 
   SootClass ProviderInfoClass = Scene.v().getSootClass("android.content.pm.ProviderInfo"); 
    if (Util.c1isBelowc2(mClass, ProviderInfoClass) && Util.methodisOneOfClass(m, ProviderInfoClass)) { return true; } 
   SootClass RegisteredServicesCacheClass = Scene.v().getSootClass("android.content.pm.RegisteredServicesCache"); 
    if (Util.c1isBelowc2(mClass, RegisteredServicesCacheClass) && Util.methodisOneOfClass(m, RegisteredServicesCacheClass)) { return true; } 
   SootClass RegisteredServicesCacheListenerClass = Scene.v().getSootClass("android.content.pm.RegisteredServicesCacheListener"); 
    if (Util.c1isBelowc2(mClass, RegisteredServicesCacheListenerClass) && Util.methodisOneOfClass(m, RegisteredServicesCacheListenerClass)) { return true; } 
   SootClass ResolveInfoClass = Scene.v().getSootClass("android.content.pm.ResolveInfo");
   if (Util.c1isBelowc2(mClass, ResolveInfoClass) && Util.methodisOneOfClass(m, ResolveInfoClass)) { return true; } 
   SootClass ServiceInfoClass = Scene.v().getSootClass("android.content.pm.ServiceInfo"); 
    if (Util.c1isBelowc2(mClass, ServiceInfoClass) && Util.methodisOneOfClass(m, ServiceInfoClass)) { return true; } 
   SootClass SignatureClass = Scene.v().getSootClass("android.content.pm.Signature"); 
    if (Util.c1isBelowc2(mClass, SignatureClass) && Util.methodisOneOfClass(m, SignatureClass)) { return true; } 
   SootClass XmlSerializerAndParserClass = Scene.v().getSootClass("android.content.pm.XmlSerializerAndParser"); 
    if (Util.c1isBelowc2(mClass, XmlSerializerAndParserClass) && Util.methodisOneOfClass(m, XmlSerializerAndParserClass)) { return true; } 
    
    return false;
  }

  private boolean mustSkipMethodFromIntent(SootMethod m, SootClass mClass) {
    SootClass broadcasterClass = Scene.v().getSootClass("android.os.Broadcaster");
    if (Util.c1isBelowc2(mClass, broadcasterClass) && Util.methodisOneOfClass(m, broadcasterClass)) {
      return true;
    }
    return false;
  }

  private boolean mustSkipMethodFromBinder(SootMethod m, SootClass mClass) {
    SootClass binderClass = Scene.v().getSootClass("android.os.Binder");
    SootClass ibinderClass = Scene.v().getSootClass("android.os.IBinder");
    SootClass parcelClass = Scene.v().getSootClass("android.os.Parcel");
    SootClass parcelFileDescriptorClass = Scene.v().getSootClass("android.os.ParcelFileDescriptor");
    SootClass parcelFormatExceptionClass = Scene.v().getSootClass("android.os.ParcelFormatException");
    SootClass parcelUuidClass = Scene.v().getSootClass("android.os.ParcelUuid");
    SootClass parcelableSpanClass = Scene.v().getSootClass("android.os.ParcelableSpan");
    if (Util.c1isBelowc2(mClass, binderClass) && Util.methodisOneOfClass(m, binderClass)) {
      if (m.toString().contains("check")) // skip methods which check permissions
        return false;
      return true;
    }
    if (Util.c1isBelowc2(mClass, ibinderClass) && Util.methodisOneOfClass(m, binderClass)) {
      if (m.toString().contains("check"))
        return false;
      return true;
    }
    if (Util.c1isBelowc2(mClass, parcelClass))
      return true;
    if (Util.c1isBelowc2(mClass, parcelFileDescriptorClass))
      return true;
    if (Util.c1isBelowc2(mClass, parcelFormatExceptionClass))
      return true;
    if (Util.c1isBelowc2(mClass, parcelUuidClass))
      return true;
    if (Util.c1isBelowc2(mClass, parcelableSpanClass))
      return true;
    
    return false;
  }

  private boolean mustSkipMethodFromThread(String mName) {
    String[] threadStrings = {
        //"Thread",
        "run("
    };
    for (String str: threadStrings) {
      if (mName.contains(str)) {
        return true;
      }
    }  
    return false;
  }
  
  public boolean mustSkipMethodsFromNonAndroidClass(SootMethod m, SootClass mClass) {
    SootClass iteratorClass = Scene.v().getSootClass("java.util.Iterator");
    String mName = m.getName();
    for (SootMethod im: iteratorClass.getMethods()) {
      if (mName.equals(im.getName())) {
        return true;
      }
    }
    return false;
  }




}
