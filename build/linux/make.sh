#!/bin/sh


### -- SETUP WORK DIR -------------------------------------------

if test -d work
then
  echo
else
  echo Setting up directories to build for linux...
  cp -r ../shared work
  cp -r ../../lib work/libraries

  cd work
  unzip -q examples.zip
  rm examples.zip
  cd ..

  cd work
  unzip -q reference.zip
  rm reference.zip
  cd ..

  tar --extract --file=jre.tgz --ungzip --directory=work

  #mkdir work/lib/export
  mkdir work/lib/build
  mkdir work/classes

  # get the serial stuff
  #echo Copying serial support from bagel dir
  #cp ../../bagel/serial/RXTXcomm.jar work/lib/
  #mkdir work/lib/i386
  #cp ../../bagel/serial/librxtxSerial.so work/lib/i386/libSerial.so
  #chmod +x work/librxtxSerial.so

  # get jikes and depedencies
  cp dist/jikes work/
  chmod +x work/jikes

  echo
fi

cd ../..


### -- BUILD CORE ----------------------------------------------


echo Building processing.core

# move to bagel inside base 'processing' directory
cd core

# new regular version
CLASSPATH="../build/linux/work/java/lib/rt.jar"
export CLASSPATH

perl preproc.pl
../build/linux/work/jikes -d . +D -target 1.1 *.java
zip -rq ../build/linux/work/lib/core.jar processing
rm -rf processing


# back to base processing dir
cd ..


### -- BUILD PREPROC ------------------------------------------------

echo Building PDE for JDK 1.3

cd app/preprocessor

# first build the default java goop
# long path is to avoid requiring java to be in your PATH

../../build/linux/work/java/bin/java \
  -cp ../../build/linux/work/lib/antlr.jar antlr.Tool java.g

# now build the pde stuff that extends the java classes
../../build/linux/work/java/bin/java \
  -cp ../../build/linux/work/lib/antlr.jar antlr.Tool -glib java.g pde.g

cd ../..


### -- BUILD PDE ------------------------------------------------

cd app

CLASSPATH="../build/linux/work/lib/core.jar:../build/linux/work/lib/mrj.jar:../build/linux/work/lib/antlr.jar:../build/linux/work/lib/oro.jar:../build/linux/work/java/lib/rt.jar"

../build/linux/work/jikes +D -classpath $CLASSPATH -d ../build/linux/work/classes *.java jeditsyntax/*.java preprocessor/*.java tools/*.java

cd ../build/linux/work/classes
rm -f ../lib/pde.jar
zip -0rq ../lib/pde.jar .
cd ../../../..



### -- BUILD LIBRARIES ------------------------------------------------


cd build/linux


CLASSPATH="../../build/linux/work/lib/core.jar:../../build/linux/work/java/lib/rt.jar"


# SERIAL LIBRARY
echo Build serial library...
cd ../../lib/serial
../../build/linux/work/jikes +D -classpath "code/RXTXcomm.jar:$CLASSPATH" -d . *.java 
rm -f library/serial.jar
zip -r0q library/serial.jar processing
rm -rf processing
mkdir -p ../../build/linux/work/libraries/serial/library/
cp library/serial.jar ../../build/linux/work/libraries/serial/library/


# NET LIBRARY
echo Build net library...
cd ../../lib/net
../../build/linux/work/jikes +D -d . *.java 
rm -f library/net.jar
zip -r0q library/net.jar processing
rm -rf processing
mkdir -p ../../build/linux/work/libraries/net/library/
cp library/net.jar ../../build/linux/work/libraries/net/library/


# PARTICLES LIBRARY
echo Build particles library...
cd ../../lib/particles
../../build/linux/work/jikes +D -d . *.java 
rm -f library/particles.jar
zip -r0q library/particles.jar simong
rm -rf simong
mkdir -p ../../build/linux/work/libraries/particles/library/
cp library/particles.jar ../../build/linux/work/libraries/particles/library/


cd ../../build/linux


### -- BUILD STUB -----------------------------------------------

install -m 755 dist/processing work/processing
