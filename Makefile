
MKFILE_PATH := $(abspath $(lastword $(MAKEFILE_LIST)))

PY    := python
JAVAC := javac
JAR   := jar

CMG_LIB   := /usr/local/lib
CMG_BIN   := /usr/local/bin
MAKE_PATH := $(CURDIR)
CODEGEN_PATH := ${MAKE_PATH}/comingle_code_generator
RUNTIME_PATH := ${MAKE_PATH}/comingle_runtime
TMP_PATH     := /tmp

uninstall:
	echo "Uninstalling Comingle Compiler..."
	rm -rf ${CMG_LIB}/comingle
	rm -f ${CMG_BIN}/cmgc
	rm -f ${CMG_BIN}/cmgc.conf

install:
	echo "Installing Comingle Compiler..."
	cd ${CODEGEN_PATH} ; ${PY} setup.py install
	
	echo "Clear temp directories..."
	rm -rf ${TMP_PATH}/comingle 
	rm -f ${TMP_PATH}/cmgc.conf
	
	echo "Making temp libraries in ${TMP_PATH}"
	mkdir ${TMP_PATH}/comingle 
	cp ${RUNTIME_PATH}/refined/jars/* ${TMP_PATH}/comingle/.
	cp ${CODEGEN_PATH}/cmgc.py ${TMP_PATH}/comingle/.
	
	echo "Moving Comingle libraries to ${CMG_LIB}"
	mv ${TMP_PATH}/comingle ${CMG_LIB}/.
	
	echo "Creating cmgc.conf in ${TMP_PATH}"		
	echo "PY=${PY}" >> ${TMP_PATH}/cmgc.conf
	echo "JAVAC=${JAVAC}" >> ${TMP_PATH}/cmgc.conf
	echo "JAR=${JAR}" >> ${TMP_PATH}/cmgc.conf
	echo "CMG_LIB=${CMG_LIB}" >> ${TMP_PATH}/cmgc.conf
	echo "TMP_PATH=${TMP_PATH}" >> ${TMP_PATH}/cmgc.conf
	
	echo "Copying Comingle executable script to ${CMG_BIN}"
	cp ${CODEGEN_PATH}/cmgc ${CMG_BIN}/.
	mv ${TMP_PATH}/cmgc.conf ${CMG_BIN}/.
	
	echo "Done! Run cmgc to compile Comingle Programs!"

