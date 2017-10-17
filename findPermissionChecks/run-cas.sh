#!/bin/bash
#
# (c) 2014 TU Darmstadt
#
# Author: Alexandre Bartel
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program.  If not, see <http://www.gnu.org/licenses/>. 
#

# configure your system by updating 'ant.settings' with the correct path to Soot
# (you can download the nightly build here: https://ssebuild.cased.de/nightly/soot/lib/soot-trunk.jar

ANDROID_FRAMEWORK="/home/alex/src/copes/in.cas/framework.jar"
ANDROID_API="/home/alex/src/copes/in.cas/android.jar"
OUTPUT_PREFIX="/tmp/toto/android3.cas" # no trailing slash
SOOT_CLASSES="/home/alex/jars/soot-trunk.jar"

BYTECODE_OUT_DIR=$OUTPUT_PREFIX".redirected/"
API_OUT_DIR=$OUTPUT_PREFIX".apimethods/"
WRAPPERS_OUT_DIR=$OUTPUT_PREFIX".wrappers/"
PERMISSIONS_MAP_OUT_DIR=$OUTPUT_PREFIX".permissions/"

function check {
    "$@"
    local status=$?
    if [ $status -ne 0 ]; then
        echo "error with $1: code $status" >&2
        exit
    fi
    return $status
}
function check255 {
    "$@"
    local status=$?
    if [ $status -ne 255 ]; then
        echo "error: return code not as expected! Error with $1: code $status" >&2
        exit
    fi
    return $status
}

# get script directory
MY_PATH="`dirname \"$0\"`"              # relative
MY_PATH="`( cd \"$MY_PATH\" && pwd )`"  # absolutized and normalized
if [ -z "$MY_PATH" ] ; then
  # error; for some reason, the path is not accessible
  # to the script (e.g. permissions re-evaled after suid)
  exit 1  # fail
fi


# modify bytecode of the Android *framework*
echo "[1/5] Instrumenting bytecode (may take a few minutes)..."
check ${MY_PATH}/../redirectAndroidRemoteCalls/run.sh -a $ANDROID_FRAMEWORK -c ${MY_PATH}/../redirectAndroidRemoteCalls/config.cfg -o $BYTECODE_OUT_DIR"/android.code/" -p ${MY_PATH}/../redirectAndroidRemoteCalls/config.cfg -s ${SOOT_CLASSES} &> $OUTPUT_PREFIX".redirect.log"
echo "...Done."

# generate list of methods from the Android *API*
echo "[2/5] Generating list of API methods..."
mkdir $API_OUT_DIR
check255 java -cp "${MY_PATH}/../bin/:${SOOT_CLASSES}" MyTransformers -process-dir $ANDROID_API -w -d $API_OUT_DIR -allow-phantom-refs -f n &> $OUTPUT_PREFIX".classlist.log"
echo "...Done."

# generate classes wrapper
echo "[3/5] Generating wrapper classes..."
check ${MY_PATH}/../entryPointWrapper/runGenerateClassWrappersWithSoot.sh -c config.cfg -b $BYTECODE_OUT_DIR -o $WRAPPERS_OUT_DIR -t $API_OUT_DIR"/classes.txt" -s ${SOOT_CLASSES} &> $OUTPUT_PREFIX".wrapper.log"
echo "...Done."

# compile classes wrappers
echo "[4/5] Compiling wrapper classes..."
check javac -cp $BYTECODE_OUT_DIR"/android.code/":$BYTECODE_OUT_DIR"/systeminit.code/" $WRAPPERS_OUT_DIR"/"*.java &> $OUTPUT_PREFIX".compile.log"
echo "...Done."

# run find permissions
echo "[5/5] Generating permission map (may take a few minutes)..."
check ${MY_PATH}/runFindPermissionChecks.sh -g spark -l $WRAPPERS_OUT_DIR -a $BYTECODE_OUT_DIR -c ${MY_PATH}/configs/spark.cfg -o $PERMISSIONS_MAP_OUT_DIR -p $ANDROID_API -s ${SOOT_CLASSES} &> $OUTPUT_PREFIX".findperms.log"
echo "...Done."
echo ""
echo "The permission map for $ANDROID_FRAMEWORK is available in $PERMISSIONS_MAP_OUT_DIR/epo.*.last.txt"

