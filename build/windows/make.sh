#!/bin/sh


### -- SETUP WORK DIR -------------------------------------------

if test -d work
then
else
  echo Setting up directories to build P5...
  cp -r ../shared work
  rm -rf work/CVS
  rm -f work/.DS_Store 
  # in case one of those little mac poopers show up

  echo Extracting examples...
  cd work
  unzip -q examples.zip
  rm examples.zip
  cd ..

  echo Extracting reference...
  cd work
  unzip -q reference.zip
  # necessary for launching reference from shell/command prompt
  # which is done internally to view reference
  chmod +x reference/*.html
  # needed by 'help' menu
  chmod +x reference/environment/*.html
  # chmod -R +x *.html doesn't seem to work

  rm reference.zip
  cd ..

  echo Extracting enormous JRE...
  unzip -q -d work jre.zip
  # cygwin requires this because of unknown weirdness
  # it was not formerly this anal retentive
  cd work/java/bin/
  chmod +x *.exe *.dll 
  chmod +x client/*.dll
  cd ../../..

  mkdir work/lib/build
  mkdir work/classes

  echo Compiling processing.exe
  cd launcher
  make && cp processing.exe ../work/
  cd ..

  # get jikes and depedencies
  cp dist/jikes.exe work/
  chmod +x work/jikes.exe
fi

cd ../..

### -- BUILD CORE ----------------------------------------------

echo Building processing.core

# move to bagel inside base 'processing' directory
#cd bagel
cd core
rm -f processing/core/*.class

# new regular version
CLASSPATH="..\\build\\windows\\work\\java\\lib\\rt.jar"
export CLASSPATH

perl preproc.pl
../build/windows/work/jikes -d . +D -target 1.1 *.java
# use this from time to time to test 1.1 savviness
#/cygdrive/c/msjdk-4.0/bin/jvc /d . *.java
zip -rq ../build/windows/work/lib/core.jar processing


# back to base processing dir
cd ..

#################### TEMPORARY #####################
# set to true to re-enable building the preprocessor
if false
then
#################### TEMPORARY #####################

### -- BUILD PREPROC ---------------------------------------------

echo Building PDE for JDK 1.4

cd app/preprocessor

# first build the default java goop
../../build/windows/work/java/bin/java \
    -cp "..\\..\\build\\windows\\work\\lib\\antlr.jar" antlr.Tool java.g

# now build the pde stuff that extends the java classes
../../build/windows/work/java/bin/java \
    -cp "..\\..\\build\\windows\\work\\lib\\antlr.jar" antlr.Tool \
    -glib java.g pde.g

# back to base processing dir
cd ../..

#################### TEMPORARY #####################
fi
#################### TEMPORARY #####################


### -- BUILD PDE ------------------------------------------------

cd app

CLASSPATH="..\\build\\windows\\work\\lib\\core.jar;..\\build\\windows\\work\\lib\\mrj.jar;..\\build\\windows\\work\\lib\antlr.jar;..\\build\\windows\\work\\lib\\oro.jar;..\\build\\windows\\work\\java\\lib\\rt.jar"

../build/windows/work/jikes +D -classpath $CLASSPATH -d ..\\build\\windows\\work/classes *.java jeditsyntax/*.java preprocessor/*.java tools/*.java
#/cygdrive/c/jdk-1.4.2_05/bin/javac.exe -classpath $CLASSPATH -d ..\\build\\windows\\work/classes *.java jeditsyntax/*.java preprocessor/*.java

cd ../build/windows/work/classes
rm -f ../lib/pde.jar
zip -0rq ../lib/pde.jar .

# back to build/windows
cd ../..


### -- BUILD LIBRARIES ------------------------------------------------

CLASSPATH="..\\..\\build\\windows\\work\\lib\\core.jar;..\\..\\build\\windows\\work\\java\\lib\\rt.jar"


# SERIAL LIBRARY
echo Build serial library...
cd ../../lib/serial
../../build/windows/work/jikes +D -classpath "code\\RXTXcomm.jar;$CLASSPATH" -d . *.java 
rm -f library/serial.jar
zip -r0q library/serial.jar processing
rm -rf processing
mkdir -p ../../build/windows/work/libraries/serial/library/
cp library/serial.jar ../../build/windows/work/libraries/serial/library/


# NET LIBRARY
echo Build net library...
cd ../../lib/net
../../build/windows/work/jikes +D -d . *.java 
rm -f library/net.jar
zip -r0q library/net.jar processing
rm -rf processing
mkdir -p ../../build/windows/work/libraries/net/library/
cp library/net.jar ../../build/windows/work/libraries/net/library/


# VIDEO LIBRARY
echo Build video library...
QTJAVA="$WINDIR\\system32\\QTJava.zip"
if test -f "${QTJAVA}"
then
  echo "Found Quicktime at $QTJAVA"
else 
  echo "could not find qtjava.zip in"
  echo "${WINDIR}\\system32\\qtjava.zip"
  echo "quicktime for java must be installed before building."
  exit 1;
fi
cd ../../lib/video
../../build/windows/work/jikes +D -classpath "$QTJAVA;$CLASSPATH" -d . *.java 
rm -f library/video.jar
zip -r0q library/video.jar processing
rm -rf processing
mkdir -p ../../build/windows/work/libraries/video/library/
cp library/video.jar ../../build/windows/work/libraries/video/library/
