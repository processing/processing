#!/bin/sh


### -- SETUP WORK DIR -------------------------------------------

if test -d work
then
  echo
else
  echo Setting up directories to build for linux...
  cp -r ../shared work

  cd work/sketchbook
  unzip -q examples.zip
  rm examples.zip
  cd ../..

  cd work
  unzip -q reference.zip
  rm reference.zip
  cd ..

  tar --extract --file=jre.tgz --ungzip --directory=work

  mkdir work/lib/export
  mkdir work/lib/build
  mkdir work/classes

  #cp dist/lib/pde_linux.properties work/lib/

  # get the serial stuff
  echo Copying serial support from bagel dir
  cp ../../bagel/serial/RXTXcomm.jar work/lib/
  mkdir work/lib/i386
  cp ../../bagel/serial/librxtxSerial.so work/lib/i386/libSerial.so
  #chmod +x work/librxtxSerial.so

  # get jikes and depedencies
  cp dist/jikes work/
  chmod +x work/jikes

  echo
fi


### -- START BUILDING -------------------------------------------

# move to 'app' directory
cd ../../app


### -- BUILD BAGEL ----------------------------------------------
cd ..
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

CLASSPATH=../build/linux/work/java/lib/rt.jar
export CLASSPATH

### --- make version with serial for the application
echo Building bagel with serial and sonic support
perl make.pl JIKES=../build/linux/work/jikes SERIAL RXTX SONIC JDK13
cp classes/*.class ../build/linux/work/classes/

### --- make version without serial for applet exporting
echo Building bagel for export with sonic
perl make.pl JIKES=../build/linux/work/jikes SONIC
cp classes/*.class ../build/linux/work/lib/export/

cd ..
cd app


### -- BUILD PDE ------------------------------------------------

echo Building PDE for JDK 1.3

cd preprocessor

# first build the default java goop
# long path is to avoid requiring java to be in your PATH

../../build/linux/work/java/bin/java \
  -cp ../../build/linux/work/lib/antlr.jar antlr.Tool java.g

# now build the pde stuff that extends the java classes
../../build/linux/work/java/bin/java \
  -cp ../../build/linux/work/lib/antlr.jar antlr.Tool -glib java.g pde.g

cd ..

CLASSPATH=../build/linux/work/classes:../build/linux/work/lib/kjc.jar:../build/linux/work/lib/antlr.jar:../build/linux/work/lib/oro.jar:../build/linux/work/java/lib/rt.jar:../build/linux/work/lib/RXTXcomm.jar

perl ../bagel/buzz.pl "../build/linux/work/jikes +D -classpath $CLASSPATH -d ../build/linux/work/classes" -dJDK13 -dRXTX *.java jeditsyntax/*.java preprocessor/*.java

cd ../build/linux/work/classes
rm -f ../lib/pde.jar
zip -0rq ../lib/pde.jar .
cd ../..


### -- BUILD STUB -----------------------------------------------

install -m 755 stub.sh work/processing
