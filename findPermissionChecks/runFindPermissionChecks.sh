#!/bin/bash
#
# usage: ./runCallGraphPrinter.sh <cha|spark> <path-to-entryPoints> <path-to-android-system.jar> <output-dir>
# 1) ./runCallGraphPrinter.sh cha /tmp/sootout/generatedTests/ /jars/android/android.4.0.1.jar /tmp/sootout/
#
# 2) ./runFindPermissionChecks.sh spark android-4.0.1-afterRedirect/ /tmp/sootout/transformedAndroid/ /tmp/sootout/ &> out
#

##
## Author: Alexandre Bartel
## 
## (c) 2012 University of Luxembourg – Interdisciplinary Centre for 
## Security Reliability and Trust (SnT) - All rights reserved
##
## This program is free software: you can redistribute it and/or modify
## it under the terms of the GNU General Public License as published by
## the Free Software Foundation, either version 3 of the License, or
## (at your option) any later version.
##
## This program is distributed in the hope that it will be useful,
## but WITHOUT ANY WARRANTY; without even the implied warranty of
## MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
## GNU General Public License for more details.
##
## You should have received a copy of the GNU General Public License
## along with this program.  If not, see <http://www.gnu.org/licenses/>. 
##

function usage {
cat << EOF
Usage: $0 -g <cha|spark> -l <path-to-entry-points-dir> -a <path-to-android-system-jar> \
          -o <output-directory>" [-m <Java heap size in MiB>] -s <path-to-soot-classes>

OPTIONS:
   -h      Show this message
   -g      Call graph generation technique: cha or spark.
   -l      Path to directory containing the target library (entry points).
   -p      Path to the android.jar API that has been used to generate the entry points.
   -a      Path to directory containing the Android system bytecode. i
   -c      Path to configuration file.
   -o      Output directory.
   -s      Path to Soot classes (.jar or directory)
   -v      Verbose.
   -m      Java heap size in MiB
EOF
  exit -1
}


MY_PATH="`dirname \"$0\"`"              # relative
MY_PATH="`( cd \"$MY_PATH\" && pwd )`"  # absolutized and normalized
if [ -z "$MY_PATH" ] ; then
	# error; for some reason, the path is not accessible
	# to the script (e.g. permissions re-evaled after suid)
	exit 1  # fail
fi
cd $MY_PATH

# parse arguments
CALLGRAPH_TECHNIOUE=""
TARGET_LIB=""
ANDROID_SYSTEM_JAR=""
CONFIG_FILE=""
SOOT_OUT_DIR=""
JAVA_HEAP_SIZE="4000"
VERBOSE="false"
ANDROID_API=""
SOOT_CLASSES=""

while getopts “hg:l:p:a:c:o:s:m:v” OPTION
do
    case $OPTION in
         h)
             usage
             exit 1
             ;;
         g)
         echo "call graph: "$OPTARG
             CALLGRAPH_TECHNIOUE=$OPTARG
             ;;
         l)
         echo "target lib: "$OPTARG
             TARGET_LIB=$OPTARG
             ;;
         p)
         echo "android API: "$OPTARG
             ANDROID_API=$OPTARG
             ;;
         a)
         echo "android system jar: "$OPTARG
             ANDROID_SYSTEM_JAR=$OPTARG
             ;;
         c)
         echo "config file: "$OPTARG
             CONFIG_FILE=$OPTARG
             ;;
         o)
         echo "sout out: "$OPTARG
             SOOT_OUT_DIR=$OPTARG
             ;;
         s)
             SOOT_CLASSES=$OPTARG
             ;;
         m)
         echo "java heap size: "$OPTARG
             JAVA_HEAP_SIZE="$OPTARG"
             ;;
         v)
         echo "verbose : "$OPTARG
             VERBOSE="true"
             ;;
         ?)
             usage
             exit
             ;;
    esac
done



CG_METHOD=$CALLGRAPH_TECHNIOUE
if [ "cha" == "$CG_METHOD" ] ; then
  echo "Using CHA"
  CG_METHOD="lu.uni.fpc.cha.CHAFindPermissionChecks"
elif [ "spark" == "$CG_METHOD" ] ; then
  echo "Using Spark"
  CG_METHOD="lu.uni.fpc.spark.SparkFindPermissionChecks"
else
  usage
  exit -1
fi

if [[ "$SOOT_OUT_DIR" == "" ]] ; then
  echo "error: no output dir specified!"
  exit -1
fi
if [[ "$CONFIG_FILE" == "" ]] ; then
  CONFIG_FILE="./fpc.cfg"
fi

echo "Java heap size: "$JAVA_HEAP_SIZE

PROCESS_THIS=" -process-dir ${TARGET_LIB}"
KEEP_CFG_FILE="${TARGET_LIB}/keep.cfg"
SKIP_CFG_FILE="${TARGET_LIB}/skip.cfg"

JAVA_CLASSPATH="\
$MY_PATH/../bin:\
$SOOT_CLASSES:\
$MY_PATH/../libs/slf4j-api-1.7.7.jar:\
$MY_PATH/../libs/slf4j-simple-1.7.7.jar:\
"
#$MY_PATH/libs/AXMLPrinter2.jar:\
#$MY_PATH/libs/rt.jar:\
#$TARGET_LIB:\

SOOT_CLASSPATH="\
$TARGET_LIB:\
${ANDROID_SYSTEM_JAR}/android.code/:\
${ANDROID_SYSTEM_JAR}/systeminit.code/:\
${ANDROID_API}:\
"
#$MY_PATH/../entryPointWrapper-testLib/lib/EntryPointWrapper-testLib.jar:\
#/home/alex/experiments/permissionMap/system-platform/android-4.0.1_r1.jar:\

SOOT_CMD="${CG_METHOD} \
 -keep $KEEP_CFG_FILE \
 -skip $SKIP_CFG_FILE \
 -cfg $CONFIG_FILE \
 -soot-class-path $SOOT_CLASSPATH \
 -d $SOOT_OUT_DIR \
 -f n \
 -w \
 -i android. \
 -i com.android. \
 -x java. \
 -allow-phantom-refs \
 -main-class MainClass \
 -no-bodies-for-excluded \
 $PROCESS_THIS
"
#-full-resolver \

if [[ "${VERBOSE}" == "true" ]] ; then
  SOOT_CMD=${SOOT_CMD}" --verbose "
fi

# -polyglot \
# -v \
# --verbose \
# -v \
# -debug \
# -debug-resolver \

echo "java classpath: "$JAVA_CLASSPATH
echo "soot command  : "$SOOT_CMD

echo "${JAVA_CLASSPATH}"
echo "${SOOT_CMD}"

# start soot
java -Xss100m -Xmx"${JAVA_HEAP_SIZE}m" -classpath \
${JAVA_CLASSPATH} \
${SOOT_CMD}\


