#!/bin/sh


### -- BUILD WINDOWS ---- ---------------------------------------

DIST=build/windows/processing
cp -r build/windows/dist $DIST






### -- BUILD PDE for 1.1 ----------------------------------------

echo Building PDE for JDK 1.1 \(Mac OS 9\)

CLASSPATH=build/macos9/classes:build/macos9/JDKClasses.zip:build/macos9/javax.comm.MRJ.jar:lib/kjc.jar:lib/oro.jar

cp ../bagel/classes/*.class build/macos9/classes/

perl buzz.pl "jikes +D -d build/macos9/classes" *.java kjc/*.java 

cd build/macos9/classes
rm -f ../dist/lib/pde.jar
zip -0q ../dist/lib/pde.jar *.class
cd ../../..


### -------------------------------------------------------------

