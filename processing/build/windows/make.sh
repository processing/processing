#!/bin/sh


### -- SETUP WORK DIR -------------------------------------------

if test -d work
then
else
  echo Setting up directories to build P5 on windows...
  mkdir work
  unzip -q -d work jre.zip
  cp -r ../shared/lib work/
  rm -rf work/lib/CVS
  mkdir work/lib/export
  mkdir work/lib/build
  cp -r ../shared/sketchbook work/
  mkdir work/classes
  # this will copy cvs files intact, meaning that changes
  # could be made and checked back in.. interesting
  cp dist/run.bat work/
  echo
fi


### -- START BUILDING -------------------------------------------

# move to 'app' directory
cd ../..


#PLATFORM_CLASSPATH=java/lib/rt.jar:java/lib/ext/comm.jar


### -- BUILD BAGEL ----------------------------------------------
cd ..
# make sure bagel exists, if not, check it out of cvs
if test -d bagel
then 
else
  echo Doing CVS checkout of bagel...
  cvs co bagel
  cd bagel
  cvs update -P
  cd ..
fi
cd bagel

CLASSPATH=../app/build/windows/work/java/lib/rt.jar:../app/build/windows/work/java/lib/ext/comm.jar
#CLASSPATH=$PLATFORM_CLASSPATH

### --- make version with serial for the application
echo Building bagel with serial support
perl make.pl SERIAL
cp classes/*.class ../app/build/windows/work/classes/

### --- make version without serial for applet exporting
echo Building bagel for export
perl make.pl
cp classes/*.class ../app/build/windows/work/lib/export/

cd ..
cd app


### -- BUILD PDE ------------------------------------------------

echo Building PDE for JDK 1.3

CLASSPATH=build/windows/work/classes:build/windows/work/lib/kjc.jar:build/windows/work/lib/oro.jar:build/windows/work/java/lib/rt.jar:build/windows/work/java/lib/ext/comm.jar

perl ../bagel/buzz.pl "jikes +D -classpath $CLASSPATH -d build/windows/work/classes" -dJDK13 *.java 

cd build/windows/work/classes
rm -f ../lib/pde.jar
zip -0q ../lib/pde.jar *.class
cd ../..

