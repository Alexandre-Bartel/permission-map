#!/bin/bash


CHA_RUN="./runFindPermissionChecks.sh -g cha -l /home/alex/experiments/permissions/soot_findPermissionChecks/ep-chaNaive/"
CONFIGS_DIR="./configs/cha/"
BYTECODE_DIR="../../_androidBytecode/"
OUT_DIR="./allCHA-001/"

mkdir ${OUT_DIR}

# cha naive
$CHA_RUN -c ${CONFIGS_DIR}/normal/fpc.cfg.cha-naive -a ${BYTECODE_DIR}/nonTransformedAndroid-SI/ -o ${OUT_DIR}/cha-naive/ &> ${OUT_DIR}/cha-naive/cha.out &
$CHA_RUN -c ${CONFIGS_DIR}/skipStubProxy/fpc.cfg.cha-naive -a ${BYTECODE_DIR}/nonTransformedAndroid-SI/ -o ${OUT_DIR}/cha-naive-skipsp/ &> ${OUT_DIR}/cha-naive-skipsp/cha.out &
# cha modif bytecode
$CHA_RUN -c ${CONFIGS_DIR}/normal/fpc.cfg.cha-modifbc -a ${BYTECODE_DIR}/transformedAndroid.services/ -o ${OUT_DIR}/cha-modifbc/ &> ${OUT_DIR}/cha-modifbc/cha.out &
$CHA_RUN -c ${CONFIGS_DIR}/skipStubProxy/fpc.cfg.cha-modifbc -a ${BYTECODE_DIR}/transformedAndroid.services/ -o ${OUT_DIR}/cha-modifbc-skipsp/ &> ${OUT_DIR}/cha-modifbc-skipsp/cha.out &
# cha service id
$CHA_RUN -c ${CONFIGS_DIR}/normal/fpc.cfg.cha-serviceid -a ${BYTECODE_DIR}/nonTransformedAndroid-SI/ -o ./cha-serviceid/ &> ${OUT_DIR}/cha-serviceid/cha.out &
$CHA_RUN -c ${CONFIGS_DIR}/skipStubProxy/fpc.cfg.cha-serviceid -a ${BYTECODE_DIR}/nonTransformedAndroid-SI/ -o ${OUT_DIR}/cha-serviceid-skipsp/ &> ${OUT_DIR}/cha-serviceid-skipsp/cha.out &
# cha service id + modif bytecode
$CHA_RUN -c ${CONFIGS_DIR}/normal/fpc.cfg.cha-modifbc+serviceid -a ${BYTECODE_DIR}/transformedAndroid.services/ -o ${OUT_DIR}/cha-modifbc+serviceid/ &> ${OUT_DIR}/cha-modifbc+serviceid/cha.out &
$CHA_RUN -c ${CONFIGS_DIR}/skipStubProxy/fpc.cfg.cha-modifbc+serviceid -a ${BYTECODE_DIR}/transformedAndroid.services/ -o ${OUT_DIR}/cha-modifbc+serviceid-skipsp/ &> ${OUT_DIR}/cha-modifbc+serviceid-skipsp/cha.out &



