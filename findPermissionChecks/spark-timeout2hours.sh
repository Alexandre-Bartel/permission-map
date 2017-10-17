#!/bin/bash

# -l ../_targetAndroidBytecode/4.0.1_r1/entrypoints/java-essential-spark/ \
# -l ./ep-chaNaive/ \
./runFindPermissionChecks.sh \
 -g spark \
 -l ./entryPoints/ep-chaNaive/ \
 -a ../_targetAndroidBytecode/4.0.1_r1/bytecode/services-redirection-all-public/onlyAndroid/:../_targetAndroidBytecode/4.0.1_r1/bytecode/original/android-4.0.1_r1.jar \
 -c spark-timeout2hours/spark-config.cfg \
 -o ./spark-timeout2hours/out/ \
 -m 20000 \
 &> ./spark-timeout2hours/out/spark.out
