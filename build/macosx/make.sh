#!/bin/sh


### -- CHECK TO MAKE SURE BAGEL EXISTS -------------------------

# move to base 'processing' directory
cd ../..

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

cd build/macosx


### -- SETUP WORK DIR -------------------------------------------

if test -d work 
then
  echo 
else
  echo Setting up directories to build under Mac OS X
  cp -r ../shared work

  echo Extracting examples...
  #cd work/sketchbook
  cd work/lib
  unzip -q examples.zip
  rm examples.zip
  cd ../..

  echo Extracting reference...
  cd work
  unzip -q reference.zip
  rm reference.zip
  cd ..

  mkdir work/lib/export
  mkdir work/lib/build

  mkdir work/classes

  # get a copy of the mac-specific properties
  #cp dist/lib/pde_macosx.properties work/lib/

  # grab serial goodies
  echo Copying serial support from bagel dir...
  cp ../../bagel/serial/RXTXcomm.jar work/lib/
  cp ../../bagel/serial/libSerial.jnilib work/

  # copy gl4java libs and jar file
  # disabled till the next release when i can recompile for 1.4
  #cp ../../bagel/opengl/gl4java.jar work/lib/
  #cp ../../bagel/opengl/libGL4JavaJauGljJNI13.jnilib work/

  # to have a copy of this guy around for messing with
  echo Copying Processing.app...
  #cp -a dist/Processing.app work/   # #@$(* bsd switches
  #cp -dpR dist/Processing.app work/
  cp -R dist/Processing.app work/
  #cd work/Processing.app
  #find . -name "CVS" -depth -exec rm {} \;
  #cd ../..

  # get jikes and depedencies
  #gunzip < dist/jikes.gz > work/jikes
  echo Copying jikes...
  cp dist/jikes work/
  chmod +x work/jikes

  # build classes/grammar for preprocessor
#  echo Building antlr grammar code...
#  cd ../../app/preprocessor
#  # first build the default java goop
#  java -cp ../../build/macosx/work/lib/antlr.jar antlr.Tool java.g
#  # now build the pde stuff that extends the java classes
#  java -cp ../../build/macosx/work/lib/antlr.jar antlr.Tool -glib java.g pde.g
#  cd ../../build/macosx

  #echo
fi


### -- START BUILDING -------------------------------------------

# move to root 'processing' directory
cd ../..


### -- BUILD BAGEL ----------------------------------------------

cd bagel

# rxtx comm.jar will be included by the build script
CLASSPATH=/System/Library/Frameworks/JavaVM.framework/Classes/classes.jar:/System/Library/Frameworks/JavaVM.framework/Classes/ui.jar:/System/Library/Java/Extensions/QTJava.zip:/System/Library/Java/Extensions/MRJToolkit.jar
export CLASSPATH

### --- make version with all the goodies for the application
#echo Building bagel with serial, video, audio, and jdk13 support
#perl make.pl JIKES=../build/macosx/work/jikes SERIAL RXTX VIDEO SONIC JDK13
#cp classes/*.class ../build/macosx/work/classes/

### --- make version without serial for applet exporting
#echo Building bagel for export with audio
#perl make.pl JIKES=../build/macosx/work/jikes SONIC
#cp classes/*.class ../build/macosx/work/lib/export/

echo Building export classes for 1.1
rm -f classes/*.class
perl make.pl JIKES=../build/macosx/work/jikes
cd classes
zip -0q ../../build/macosx/work/lib/export11.jar *.class
cd ..

echo Building export classes for 1.3
rm -f classes/*.class
perl make.pl JIKES=../build/macosx/work/jikes
cd classes
zip -0q ../../build/macosx/work/lib/export13.jar *.class
cd ..

# head back to root "processing" dir
cd ../app



### -- BUILD PARSER ---------------------------------------------

# add code here later to conditionally build the parser. 
# but for now, the parser is only built when the work dir 
# is created, to speed the build process.

echo Removing preproc code so it will regenerate
#rm preprocessor/expandedpde.g

if test -f preprocessor/expandedpde.g
then
  echo
else
  cd preprocessor
  # build classes/grammar for preprocessor
  echo Building antlr grammar code...
  # first build the default java goop
  java -cp ../../build/macosx/work/lib/antlr.jar antlr.Tool java.g
  # now build the pde stuff that extends the java classes
  java -cp ../../build/macosx/work/lib/antlr.jar antlr.Tool -glib java.g pde.g
  cd ..
fi


### -- BUILD PDE ------------------------------------------------

echo Building PDE for JDK 1.3

# new rxtx
CLASSPATH=../build/macosx/work/classes:../build/macosx/work/lib/kjc.jar:../build/macosx/work/lib/antlr.jar:../build/macosx/work/lib/oro.jar:../build/macosx/work/lib/RXTXcomm.jar:$CLASSPATH

perl ../bagel/buzz.pl "../build/macosx/work/jikes +D -classpath $CLASSPATH -d ../build/macosx/work/classes" -dJDK13 -dMACOS -dRXTX *.java jeditsyntax/*.java preprocessor/*.java
#perl ../bagel/buzz.pl "javac -classpath $CLASSPATH -d ../build/macosx/work/classes" -dJDK13 -dMACOS -dRXTX *.java jeditsyntax/*.java preprocessor/*.java

cd ../build/macosx/work/classes
rm -f ../lib/pde.jar
zip -0rq ../lib/pde.jar .
cd ../..

# get the libs
cp work/lib/*.jar work/Processing.app/Contents/Resources/Java/
