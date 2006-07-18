#!/bin/sh


### -- SETUP WORK DIR -------------------------------------------

if test -d work
then
  BUILD_PREPROC=false
else
  echo Setting up directories to build for linux...
  BUILD_PREPROC=true
  cp -r ../shared work
  rm -rf work/.svn

  # needs to make the dir because of packaging goofiness
  mkdir -p work/classes/processing/app/preproc
  mkdir -p work/classes/processing/app/syntax
  mkdir -p work/classes/processing/app/tools

  cp -r ../../net work/libraries/
  cp -r ../../opengl work/libraries/
  cp -r ../../serial work/libraries/
  cp -r ../../video work/libraries/
  cp -r ../../pdf work/libraries/
  cp -r ../../dxf work/libraries/

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

  tar --extract --file=jre.tgz --ungzip --directory=work

  #mkdir work/lib/export
  mkdir work/lib/build
  #mkdir work/classes

  # get the serial stuff
  #echo Copying serial support from bagel dir
  #cp ../../bagel/serial/RXTXcomm.jar work/lib/
  #mkdir work/lib/i386
  #cp ../../bagel/serial/librxtxSerial.so work/lib/i386/libSerial.so
  #chmod +x work/librxtxSerial.so

  # get jikes and depedencies
  cp dist/jikes work/
  chmod +x work/jikes

  install -m 755 dist/processing work/processing
fi

cd ../..


### -- BUILD CORE ----------------------------------------------


echo Building processing.core

cd core

CLASSPATH="../build/linux/work/java/lib/rt.jar"
export CLASSPATH

perl preproc.pl
../build/linux/work/jikes -d bin +D -target 1.1 src/processing/core/*.java
cd bin && zip -rq ../../build/linux/work/lib/core.jar processing && cd ..


# back to base processing dir
cd ..


### -- BUILD PREPROC ------------------------------------------------

echo Building PDE for JDK 1.3

cd app/preproc

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

CLASSPATH="../build/linux/work/lib/core.jar:../build/linux/work/lib/mrj.jar:../build/linux/work/lib/antlr.jar:../build/linux/work/lib/oro.jar:../build/linux/work/lib/registry.jar:../build/linux/work/java/lib/rt.jar"

../build/linux/work/jikes -target 1.3 +D -classpath $CLASSPATH:../build/linux/work/classes -d ../build/linux/work/classes *.java preproc/*.java syntax/*.java tools/*.java

cd ../build/linux/work/classes
rm -f ../lib/pde.jar
zip -0rq ../lib/pde.jar .
cd ../../../..


### -- BUILD LIBRARIES ------------------------------------------------

cd build/linux

PLATFORM=linux

#CLASSPATH="../../build/linux/work/lib/core.jar:../../build/linux/work/java/lib/rt.jar"
CLASSPATH=../build/$PLATFORM/work/lib/core.jar:$CLASSPATH
JIKES=../build/$PLATFORM/work/jikes
CORE=../build/$PLATFORM/work/lib/core.jar
LIBRARIES=../build/$PLATFORM/work/libraries

# move to processing/build 
cd ..


# SERIAL LIBRARY
echo Building serial library...
cd ../serial
mkdir -p bin
$JIKES -target 1.1 +D \
    -classpath "library/RXTXcomm.jar:$CORE:$CLASSPATH" \
    -d bin src/processing/serial/*.java 
rm -f library/serial.jar
cd bin && zip -r0q ../library/serial.jar processing && cd ..
mkdir -p $LIBRARIES/serial/library/
cp library/serial.jar $LIBRARIES/serial/library/


# NET LIBRARY
echo Building net library...
cd ../net
mkdir -p bin
$JIKES -target 1.1 +D -d bin src/processing/net/*.java 
rm -f library/net.jar
cd bin && zip -r0q ../library/net.jar processing/net/*.class && cd ..
mkdir -p $LIBRARIES/net/library/
cp library/net.jar $LIBRARIES/net/library/


# OPENGL LIBRARY
echo Building OpenGL library...
cd ../opengl
mkdir -p bin
$JIKES -target 1.1 +D \
    -classpath "library/jogl.jar:$CLASSPATH" \
    -d bin src/processing/opengl/*.java 
rm -f library/opengl.jar
cd bin && zip -r0q ../library/opengl.jar processing/opengl/*.class && cd ..
mkdir -p $LIBRARIES/opengl/library/
cp library/opengl.jar $LIBRARIES/opengl/library/


# PDF LIBRARY
echo Building PDF library...
cd ../pdf
mkdir -p bin
$JIKES -target 1.1 +D \
    -classpath "library/itext.jar:$CLASSPATH" \
    -d bin src/processing/pdf/*.java 
rm -f library/pdf.jar
cd bin && zip -r0q ../library/pdf.jar processing/pdf/*.class && cd ..
mkdir -p $LIBRARIES/pdf/library/
cp library/pdf.jar $LIBRARIES/pdf/library/


# DXF LIBRARY
echo Building DXF library...
cd ../dxf
mkdir -p bin
$JIKES -target 1.1 +D -d bin src/processing/dxf/*.java 
rm -f library/dxf.jar
cd bin && zip -r0q ../library/dxf.jar processing/dxf/*.class && cd ..
mkdir -p $LIBRARIES/dxf/library/
cp library/dxf.jar $LIBRARIES/dxf/library/


echo
echo Done.
