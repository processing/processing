#!/bin/sh

g++ -mwindows -mno-cygwin -O2 -Wall -c -o launcher.o launcher.cpp
windres -i launcher.rc -o launcher-rc.o
g++ -mwindows -mno-cygwin -O2 -Wall -mwindows -mno-cygwin -O2 -Wall -o processing.exe launcher.o launcher-rc.o
cp processing.exe ../work/

g++ -mwindows -mno-cygwin -O2 -Wall -DEXPORT -c -o launcher.o launcher.cpp
g++ -mwindows -mno-cygwin -O2 -Wall -mwindows -mno-cygwin -O2 -Wall -o export.exe launcher.o 
cp export.exe ../work/lib/export/application.exe
cp export.exe ../../shared/lib/export/application.exe
