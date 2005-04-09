#!/bin/sh


### -- SETUP WORK DIR -------------------------------------------

if test -d work 
then
  BUILD_PREPROC=false
else
  if test -f /sw/bin/cp
  then
    echo
  else
    echo You need to install fink with fileutils, textutils, etc
    exit
  fi

  echo Setting up directories to build under Mac OS X
  BUILD_PREPROC=true

  cp -r ../shared work

  # needs to make the dir because of packaging goofiness
  mkdir -p work/classes/processing/app/preproc
  mkdir -p work/classes/processing/app/syntax
  mkdir -p work/classes/processing/app/tools

  cp -r ../../lib work/libraries
  cp -r ../../net work/libraries/
  cp -r ../../opengl work/libraries/
  cp -r ../../serial work/libraries/
  cp -r ../../video work/libraries/

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
  # cvs doesn't seem to want to honor the +x bit 
  chmod +x work/Processing.app/Contents/MacOS/JavaApplicationStub

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

perl preproc.pl
../build/macosx/work/jikes -d . +D -target 1.1 *.java
zip -r0q ../build/macosx/work/lib/core.jar processing

# head back to root "processing" dir
cd ../app



### -- BUILD PARSER ---------------------------------------------

#BUILD_PREPROC=true

if $BUILD_PREPROC
then
  cd preproc
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

# compile the code as java 1.3, so that the application will run and
# show the user an error, rather than crapping out with some strange
# "class not found" crap
#../build/macosx/work/jikes -target 1.3 +D -classpath ../build/macosx/work/classes:../build/macosx/work/lib/core.jar:../build/macosx/work/lib/antlr.jar:../build/macosx/work/lib/oro.jar:../build/macosx/work/lib/registry.jar:$CLASSPATH -d ../build/macosx/work/classes *.java syntax/*.java preproc/*.java tools/*.java
../build/macosx/work/jikes -target 1.3 +D -classpath ../build/macosx/work/classes:../build/macosx/work/lib/core.jar:../build/macosx/work/lib/antlr.jar:../build/macosx/work/lib/oro.jar:../build/macosx/work/lib/registry.jar:$CLASSPATH -d ../build/macosx/work/classes tools/*.java preproc/*.java syntax/*.java  *.java 

cd ../build/macosx/work/classes
rm -f ../lib/pde.jar
zip -0rq ../lib/pde.jar .
cd ../..

# i fought the packages and the packages won
#cd ../..
#WORKLIB=processing/build/macosx/work/lib
#./processing/build/macosx/work/jikes -d . -target 1.3 +D -classpath $WORKLIB/core.jar:$WORKLIB/antlr.jar:$WORKLIB/oro.jar:$WORKLIB/registry.jar:$CLASSPATH -d . processing/app/*.java processing/app/preproc/*.java processing/app/syntax/*.java processing/app/tools/*.java
#zip -rq $WORKLIB/pde.jar processing/app/*.class processing/app/preproc/*.class processing/app/syntax/*.class processing/app/tools/*.class

# get the libs
mkdir -p work/Processing.app/Contents/Resources/Java/
cp work/lib/*.jar work/Processing.app/Contents/Resources/Java/


### -- BUILD LIBRARIES ------------------------------------------------


PLATFORM=macosx


CLASSPATH=../build/$PLATFORM/work/lib/core.jar:$CLASSPATH
JIKES=../build/$PLATFORM/work/jikes
CORE=../build/$PLATFORM/work/lib/core.jar
LIBRARIES=../build/$PLATFORM/work/libraries

# move to processing/build 
cd ..


# SERIAL LIBRARY
echo Building serial library...
cd ../serial
$JIKES -target 1.1 +D -classpath "code/RXTXcomm.jar:$CORE:$CLASSPATH" -d . *.java 
rm -f library/serial.jar
zip -r0q library/serial.jar processing
rm -rf processing
mkdir -p $LIBRARIES/serial/library/
cp library/serial.jar $LIBRARIES/serial/library/


# NET LIBRARY
echo Building net library...
cd ../net
$JIKES -target 1.1 +D -d . *.java 
rm -f library/net.jar
zip -r0q library/net.jar processing
rm -rf processing
mkdir -p $LIBRARIES/net/library/
cp library/net.jar $LIBRARIES/net/library/


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
cd ../video
$JIKES -target 1.1 +D -classpath "$QTJAVA:$CLASSPATH" -d . *.java 
rm -f library/video.jar
zip -r0q library/video.jar processing
rm -rf processing
mkdir -p $LIBRARIES/video/library/
cp library/video.jar $LIBRARIES/video/library/


# OPENGL LIBRARY
echo Building OpenGL library...
cd ../opengl
$JIKES -target 1.1 +D -classpath "library/jogl.jar:$CLASSPATH" -d . *.java 
rm -f library/opengl.jar
zip -r0q library/opengl.jar processing
rm -rf processing
mkdir -p $LIBRARIES/opengl/library/
cp library/opengl.jar $LIBRARIES/opengl/library/


CLASSPATH=../$CLASSPATH
JIKES=../../build/$PLATFORM/work/jikes
CORE=../../build/$PLATFORM/work/lib/core.jar
LIBRARIES=../../build/$PLATFORM/work/libraries


# PARTICLES LIBRARY
echo Build particles library...
cd ../lib/particles
$JIKES -target 1.1 +D -d . *.java 
rm -f library/particles.jar
zip -r0q library/particles.jar simong
rm -rf simong
mkdir -p $LIBRARIES/particles/library/
cp library/particles.jar $LIBRARIES/particles/library/

echo
echo Done.