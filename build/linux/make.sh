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
  cp -r ../../xml work/libraries/
  cp -r ../../candy work/libraries/

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

  #tar --extract --file=jre.tgz --ungzip --directory=work
  #chmod +x jre.sfx
  #./jre.sfx
  #mv jre1.5.0_15 work/java

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
find bin -name "*~" -exec rm -f {} ';'
rm -f ../build/linux/work/lib/core.jar
cd bin && zip -rq ../../build/linux/work/lib/core.jar processing && cd ..


# back to base processing dir
cd ..


### -- BUILD PREPROC ------------------------------------------------

echo Building PDE for JDK 1.4

cd app

# long path is to avoid requiring java to be in your PATH
  echo Building antlr grammar code...

  # first build the default java goop
../build/linux/work/java/bin/java \
  -cp ../build/linux/work/lib/antlr.jar antlr.Tool \
  -o src/antlr/java \
  src/antlr/java/java.g

  # hack to get around path mess
  cp src/antlr/java/JavaTokenTypes.txt src/processing/app/preproc/

# now build the pde stuff that extends the java classes
# this is totally ugly and needs to be fixed
# the problem is that -glib doesn't set the main path properly, 
# so it's necessary to cd into the antlr/java folder, otherwise
# the JavaTokenTypes.txt file won't be found
cd src/antlr/java
../../../../build/linux/work/java/bin/java \
  -cp ../../../../build/linux/work/lib/antlr.jar antlr.Tool \
  -o ../../processing/app/preproc \
  -glib java.g \
  ../../processing/app/preproc/pde.g
cd ../../..

# return to the root of the p5 folder
cd ..


### -- BUILD PDE ------------------------------------------------

cd app

CLASSPATH="../build/linux/work/lib/core.jar:../build/linux/work/lib/apple.jar:../build/linux/work/lib/antlr.jar:../build/linux/work/lib/oro.jar:../build/linux/work/lib/registry.jar:../build/linux/work/lib/tools.jar:../build/linux/work/java/lib/rt.jar"

../build/linux/work/jikes -target 1.3 +D -classpath $CLASSPATH:../build/linux/work/classes -d ../build/linux/work/classes src/processing/app/*.java src/processing/app/debug/*.java src/processing/app/preproc/*.java src/processing/app/syntax/*.java src/processing/app/tools/*.java src/antlr/*.java src/antlr/java/*.java

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
find bin -name "*~" -exec rm -f {} ';'
cd bin && zip -r0q ../library/serial.jar processing && cd ..
mkdir -p $LIBRARIES/serial/library/
cp library/serial.jar $LIBRARIES/serial/library/


# NET LIBRARY
echo Building net library...
cd ../net
mkdir -p bin
$JIKES -target 1.1 +D -d bin src/processing/net/*.java 
rm -f library/net.jar
find bin -name "*~" -exec rm -f {} ';'
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
find bin -name "*~" -exec rm -f {} ';'
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
find bin -name "*~" -exec rm -f {} ';'
cd bin && zip -r0q ../library/pdf.jar processing/pdf/*.class && cd ..
mkdir -p $LIBRARIES/pdf/library/
cp library/pdf.jar $LIBRARIES/pdf/library/


# DXF LIBRARY
echo Building DXF library...
cd ../dxf
mkdir -p bin
$JIKES -target 1.1 +D -d bin src/processing/dxf/*.java 
rm -f library/dxf.jar
find bin -name "*~" -exec rm -f {} ';'
cd bin && zip -r0q ../library/dxf.jar processing/dxf/*.class && cd ..
mkdir -p $LIBRARIES/dxf/library/
cp library/dxf.jar $LIBRARIES/dxf/library/


# XML LIBRARY
echo Building XML library...
cd ../xml
mkdir -p bin
$JIKES -target 1.1 +D -d bin src/processing/xml/*.java 
rm -f library/xml.jar
find bin -name "*~" -exec rm -f {} ';'
cd bin && zip -r0q ../library/xml.jar processing/xml/*.class && cd ..
mkdir -p $LIBRARIES/xml/library/
cp library/xml.jar $LIBRARIES/xml/library/


# CANDY SVG LIBRARY
echo Building Candy SVG library...
cd ../candy
mkdir -p bin
$JIKES -target 1.1 +D \
    -classpath "../xml/library/xml.jar:$CLASSPATH" \
    -d bin src/processing/candy/*.java 
rm -f library/candy.jar
find bin -name "*~" -exec rm -f {} ';'
cd bin && zip -r0q ../library/candy.jar processing/candy/*.class && cd ..
mkdir -p $LIBRARIES/candy/library/
cp library/candy.jar $LIBRARIES/candy/library/


echo
echo Done.
