#!/bin/sh


### -- SETUP WORK DIR -------------------------------------------

if test -d work
then
else
  echo Setting up directories to build P5...
  #mkdir work
  cp -r ../shared work
  unzip -q -d work jre.zip

  mkdir work/lib/export
  mkdir work/lib/build
  # this will copy cvs files intact, meaning that changes
  # could be made and checked back in.. interesting
  mkdir work/classes

  cp dist/lib/pde_windows.properties work/lib/
  cp dist/Proce55ing.exe work/

  echo
fi


### -- START BUILDING -------------------------------------------

# move to base 'processing' directory
cd ../..


### -- BUILD BAGEL ----------------------------------------------

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

CLASSPATH=../build/windows/work/java/lib/rt.jar:../build/windows/work/java/lib/ext/comm.jar

### --- make version with serial for the application
echo Building bagel with serial support
perl make.pl SERIAL
cp classes/*.class ../build/windows/work/classes/

### --- make version without serial for applet exporting
echo Building bagel for export
perl make.pl
cp classes/*.class ../build/windows/work/lib/export/

cd ..


### -- BUILD PDE ------------------------------------------------

#echo Building PDE for JDK 1.3
echo Building PDE for JDK 1.4

cd app

CLASSPATH=../build/windows/work/classes:../build/windows/work/lib/kjc.jar:../build/windows/work/lib/oro.jar:../build/windows/work/java/lib/rt.jar:../build/windows/work/java/lib/ext/comm.jar

#perl ../bagel/buzz.pl "jikes +D -classpath $CLASSPATH -d ../build/windows/work/classes" -dJDK13 *.java lexer/*.java
perl ../bagel/buzz.pl "jikes +D -classpath $CLASSPATH -d ../build/windows/work/classes" -dJDK13 -dJDK14 *.java lexer/*.java

cd ../build/windows/work/classes
rm -f ../lib/pde.jar
zip -0q ../lib/pde.jar *.class

# back to 'build' dir
cd ../../..

