#!/bin/sh


### -- SETUP WORK DIR -------------------------------------------

if test -d work 
then
  echo 
else
  echo Setting up directories to build under Mac OS X
  cp -r ../shared work

  echo Extracting examples
  cd work/sketchbook
  unzip -q examples.zip
  rm examples.zip
  cd ../..

  echo Extracting reference...
  cd work
  unzip -q reference.zip
  rm reference.zip
  cd ..

  # copy gl4java libs and jar file
  #cp ../../bagel/opengl/gl4java.jar work/lib/
  #cp ../../bagel/opengl/macosx/libGL4JavaJauGljJNI13.jnilib work/

  mkdir work/lib/export
  mkdir work/lib/build

  mkdir work/classes

  cp dist/lib/pde_macosx.properties work/lib/

  # grab serial goodies
  echo Copying serial support from bagel dir
  cp ../../bagel/serial/RXTXcomm.jar work/lib/
  cp ../../bagel/serial/libSerial.jnilib work/

  # get jikes and depedencies
  gunzip < dist/jikes.gz > work/jikes
  chmod +x work/jikes

  echo
fi


### -- START BUILDING -------------------------------------------

# move to 'app' directory
cd ../..


### -- BUILD BAGEL ----------------------------------------------

# make sure bagel exists, if not, check it out of cvs
if test -d bagel
then 
  echo
else
  echo Doing CVS checkout of bagel...
  cvs co bagel
  cd bagel
  cvs update -P
  cd ..
fi
cd bagel

# old comm.jar
#MACOSX_CLASSPATH=/System/Library/Frameworks/JavaVM.framework/Classes/classes.jar:/System/Library/Frameworks/JavaVM.framework/Classes/ui.jar:/System/Library/Frameworks/JavaVM.framework/Home/lib/ext/comm.jar:/System/Library/Java/Extensions/QTJava.zip:/System/Library/Java/Extensions/MRJToolkit.jar

# new rxtx comm
MACOSX_CLASSPATH=/System/Library/Frameworks/JavaVM.framework/Classes/classes.jar:/System/Library/Frameworks/JavaVM.framework/Classes/ui.jar:/System/Library/Java/Extensions/QTJava.zip:/System/Library/Java/Extensions/MRJToolkit.jar
# need not be included

CLASSPATH=$MACOSX_CLASSPATH
export CLASSPATH

### --- make version with all the goodies for the application
echo Building bagel with serial, video, audio, and jdk13 support
perl make.pl SERIAL RXTX VIDEO SONIC JDK13
cp classes/*.class ../build/macosx/work/classes/

### --- make version without serial for applet exporting
echo Building bagel for export with audio
perl make.pl SONIC
cp classes/*.class ../build/macosx/work/lib/export/

cd ..
cd app


### -- BUILD PDE ------------------------------------------------

echo Building PDE for JDK 1.4

# new rxtx
CLASSPATH=../build/macosx/work/classes:../build/macosx/work/lib/kjc.jar:../build/macosx/work/lib/oro.jar:../build/macosx/work/lib/RXTXcomm.jar:$MACOSX_CLASSPATH

perl ../bagel/buzz.pl "jikes +D -classpath $CLASSPATH -d ../build/macosx/work/classes" -dJDK13 -dJDK14 -dMACOS *.java jeditsyntax/*.java

cd ../build/macosx/work/classes
rm -f ../lib/pde.jar
zip -0q ../lib/pde.jar *.class
cd ../..
