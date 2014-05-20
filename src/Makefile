GPP=g++
CC=gcc
TARGET=InterprocessLock
PATH_JAVA=/usr/lib/jvm/java-7-oracle
all: c/InterprocessLockImp.cpp
	${GPP} -std=c++11 -lstdc++ -I${PATH_JAVA}/include/ -I${PATH_JAVA}/include/linux/ -fPIC -c c/InterprocessLockImp.cpp
	${GPP} -pthread -shared -Wl,-soname,lib"${TARGET}".so.1 -o lib"${TARGET}".so.1.0 InterprocessLockImp.o -lrt
	
	link lib"${TARGET}".so.1.0 lib"${TARGET}".so
