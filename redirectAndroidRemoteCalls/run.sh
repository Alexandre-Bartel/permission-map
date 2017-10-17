#!/bin/bash
#
#
# 1) ./run.sh /path/to/android-4.0.1_r1.jar

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
Usage: $0 -a <path-to-android-system-jar> -o <output-directory> -c <config-file>
          -s <path-to-soot-classes>

OPTIONS:
   -h      Show this message
   -c      Path to configuration file.
   -p      Path to file describing content providers.
   -a      Path to jar or directory containing the Android system bytecode
           to modify.
   -o      Output directory where the modified bytecode is stored.
   -s      Path to Soot classes (.jar or directory)
EOF
  exit -1
}

function getJar {
  for mLine in `grep "^$1" "${ANT_SETTINGS}"`
  do
    result=`echo "${mLine}" | sed s/.*=//`
    if [ ! -f ${result} ]
    then
      echo "error: jar file does not exist: '"${result}"'"
      die
    fi
  done
}

# get script directory
MY_PATH="`dirname \"$0\"`"              # relative
MY_PATH="`( cd \"$MY_PATH\" && pwd )`"  # absolutized and normalized
if [ -z "$MY_PATH" ] ; then
	# error; for some reason, the path is not accessible
	# to the script (e.g. permissions re-evaled after suid)
	exit 1  # fail
fi

ANT_SETTINGS="${MY_PATH}/ant.settings"

# parse arguments
ANDROID_SYSTEM_JAR=""
SOOT_OUT_DIR=""
CONFIG_FILE=""
CONTENT_PROVIDER_FILE=""
SOOT_CLASSES=""
while getopts “hc:a:p:o:s:” OPTION
do
    case $OPTION in
         h)
             usage
             exit 1
             ;;
         c)
             CONFIG_FILE=$OPTARG
             ;;
         a)
             ANDROID_SYSTEM_JAR=$OPTARG
             ;;
         o)
             SOOT_OUT_DIR=$OPTARG
             ;;
         p)
             CONTENT_PROVIDER_FILE=$OPTARG
             ;;
         s)
             SOOT_CLASSES=$OPTARG
             ;;
         ?)
             usage
             exit
             ;;
    esac
done

if [ "$SOOT_OUT_DIR" == "" ] ; then
  echo "error: no output dir specified!"
  usage
  exit -1
fi
if [ "$ANDROID_SYSTEM_JAR" == "" ] ; then
  echo "error: no android system jar specified!"
  usage
  exit -1
fi
if [ "$CONFIG_FILE" == "" ] ; then
  echo "error: no configuration file specified!"
  usage
  exit -1
fi
if [ "$CONTENT_PROVIDER_FILE" == "" ] ; then
  echo "error: no content provider file specified!"
  usage
  exit -1
fi
if [ "$SOOT_CLASSES" == "" ] ; then
  echo "error: no path to soot classes specified!"
  usage
  exit -1
fi




# construct arguments to launch Soot
PROCESS_THIS=" -process-dir ${ANDROID_SYSTEM_JAR}"

SOOT_JAR=${SOOT_CLASSES}

JAVA_CLASSPATH="\
$MY_PATH/../bin/:\
$MY_PATH/../libs/slf4j-simple-1.7.7.jar:\
$MY_PATH/../libs/slf4j-api-1.7.7.jar:\
$SOOT_JAR:\
$ANDROID_SYSTEM_JAR:\
"
#$MY_PATH/libs/axmlprinter.jar\
SOOT_CLASSPATH="\
$ANDROID_SYSTEM_JAR:\
"
#/usr/lib/jvm/java-6-sun-1.6.0.26/jre/lib/rt.jar:\
#$MY_PATH/../entryPointWrapper-testLib/lib/EntryPointWrapper-testLib.jar:\
#.:/tmp/sootout/generatedTests/:"

#$JAVA_HOME_PATH:\
#$MY_PATH/../entryPointWrapper-testLib/lib/EntryPointWrapper-testLib.jar:\
#"
#/tmp/sootout/generatedTests:\
#"${JAVA_HOME_PATH}":\
#/usr/lib/jvm/java-6-sun-1.6.0.26/jre/lib/rt.jar:\

SOOT_CMD="lu.uni.rrc.RedirectCalls \
 -cfg $CONFIG_FILE \
 -cp $CONTENT_PROVIDER_FILE \
 -soot-class-path $SOOT_CLASSPATH \
 -d $SOOT_OUT_DIR \
 -w \
 -allow-phantom-refs \
 -main-class MainClass \
 -include-all \
 $PROCESS_THIS
"
# -f n \
# -full-resolver \
# -polyglot \
# -v \
# --verbose \
echo "java classpath: "$JAVA_CLASSPATH
echo "soot command  : "$SOOT_CMD

# start soot
java -Xmx4000m -classpath \
${JAVA_CLASSPATH} \
${SOOT_CMD}\


