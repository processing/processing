#!/bin/sh


### -- SETUP WORK DIR -------------------------------------------

if test -d work 
then
  echo 
else
  if test -f /sw/bin/cp
  then
    echo
  else
    echo You need to install fink with fileutils, textutils, etc
    exit
  fi

  echo Setting up directories to build under Mac OS X
  cp -r ../shared work

  cp -r ../../lib work/libraries

  echo Extracting examples...
  cd work
  unzip -q examples.zip
  rm examples.zip
  cd ..

  echo Extracting reference...
  cd work
  unzip -q reference.zip
  rm reference.zip
  cd ..

  mkdir work/lib/build

  # to have a copy of this guy around for messing with
  echo Copying Processing.app...
  #cp -a dist/Processing.app work/   # #@$(* bsd switches
  /sw/bin/cp -a dist/Processing.app work/

  # get jikes and depedencies
  echo Copying jikes...
  cp dist/jikes work/
  chmod +x work/jikes
fi


### -- START BUILDING -------------------------------------------

# move to root 'processing' directory
cd ../..


### -- BUILD BAGEL ----------------------------------------------

cd core

echo Building processing.core...

# rxtx comm.jar will be included by the build script
CLASSPATH=/System/Library/Frameworks/JavaVM.framework/Classes/classes.jar:/System/Library/Frameworks/JavaVM.framework/Classes/ui.jar:/System/Library/Java/Extensions/QTJava.zip:/System/Library/Java/Extensions/MRJToolkit.jar
export CLASSPATH

../build/macosx/work/jikes -d . +D -target 1.1 *.java
zip -r0q ../build/macosx/work/lib/core.jar processing

# head back to root "processing" dir
cd ../app



### -- BUILD PARSER ---------------------------------------------

# add code here later to conditionally build the parser. 
# but for now, the parser is only built when the work dir 
# is created, to speed the build process.

#echo Removing preproc code so it will regenerate
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

echo Building the PDE...

../build/macosx/work/jikes +D -classpath ../build/macosx/work/lib/core.jar:../build/macosx/work/lib/antlr.jar:../build/macosx/work/lib/oro.jar:$CLASSPATH -d ../build/macosx/work/classes *.java jeditsyntax/*.java preprocessor/*.java tools/*.java

cd ../build/macosx/work/classes
rm -f ../lib/pde.jar
zip -0rq ../lib/pde.jar .
cd ../..

# get the libs
mkdir -p work/Processing.app/Contents/Resources/Java/
cp work/lib/*.jar work/Processing.app/Contents/Resources/Java/


### -- BUILD LIBRARIES ------------------------------------------------

CLASSPATH=../../build/macosx/work/lib/core.jar:$CLASSPATH


# SERIAL LIBRARY
echo Building serial library...
cd ../../lib/serial
../../build/macosx/work/jikes -target 1.1 +D -classpath "code/RXTXcomm.jar:../../build/macosx/work/lib/core.jar:$CLASSPATH" -d . *.java 
rm -f library/serial.jar
zip -r0q library/serial.jar processing
rm -rf processing
mkdir -p ../../build/macosx/work/libraries/serial/library/
cp library/serial.jar ../../build/macosx/work/libraries/serial/library/


# NET LIBRARY
echo Building net library...
cd ../../lib/net
../../build/macosx/work/jikes -target 1.1 +D -d . *.java 
rm -f library/net.jar
zip -r0q library/net.jar processing
rm -rf processing
mkdir -p ../../build/macosx/work/libraries/net/library/
cp library/net.jar ../../build/macosx/work/libraries/net/library/


# VIDEO LIBRARY
echo Building video library...
QTJAVA=/System/Library/Java/Extensions/QTJava.zip
if test -f "${QTJAVA}"
then
  echo "Found QuickTime for Java at $QTJAVA"
else 
  echo "QuickTime for Java must be installed before building."
  exit 1;
fi
cd ../../lib/video
../../build/macosx/work/jikes -target 1.1 +D -classpath "$QTJAVA:$CLASSPATH" -d . *.java 
rm -f library/video.jar
zip -r0q library/video.jar processing
rm -rf processing
mkdir -p ../../build/macosx/work/libraries/video/library/
cp library/video.jar ../../build/macosx/work/libraries/video/library/


# PARTICLES LIBRARY
echo Build particles library...
cd ../../lib/particles
../../build/macosx/work/jikes -target 1.1 +D -d . *.java 
rm -f library/particles.jar
zip -r0q library/particles.jar simong
rm -rf simong
mkdir -p ../../build/macosx/work/libraries/particles/library/
cp library/particles.jar ../../build/macosx/work/libraries/particles/library/


# OPENGL LIBRARY
echo Building OpenGL library...
cd ../../lib/opengl
../../build/macosx/work/jikes -target 1.1 +D -classpath "library/jogl.jar:$CLASSPATH" -d . *.java 
rm -f library/opengl.jar
zip -r0q library/opengl.jar processing
rm -rf processing
mkdir -p ../../build/macosx/work/libraries/opengl/library/
cp library/opengl.jar ../../build/macosx/work/libraries/opengl/library/


echo
echo Done.