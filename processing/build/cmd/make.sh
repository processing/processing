#!/bin/sh


### -- SETUP WORK DIR -------------------------------------------

if test -d work
then
  BUILD_PREPROC=false
else
  echo Setting up directories to build Processing for the command line...
  BUILD_PREPROC=true

  mkdir work
  cp -r ../shared/lib work/
  cp -r ../shared/libraries work/

  cp ../../app/lib/antlr.jar work/lib/
  cp ../../app/lib/ecj.jar work/lib/
  cp ../../app/lib/jna.jar work/lib/

  cp -r ../../net work/libraries/
  cp -r ../../opengl work/libraries/
  cp -r ../../serial work/libraries/
  cp -r ../../video work/libraries/
  cp -r ../../pdf work/libraries/
  cp -r ../../dxf work/libraries/
  cp -r ../../xml work/libraries/
  cp -r ../../candy work/libraries/

  install -m 755 dist/processing work/processing
fi

cd ../..


### -- BUILD CORE ----------------------------------------------


echo Building processing.core

cd core

perl preproc.pl
mkdir -p bin
javac -d bin -source 1.5 -target 1.5 src/processing/core/*.java
rm -f ../build/linux/work/lib/core.jar
cd bin && zip -rq ../../build/linux/work/lib/core.jar processing/core/*.class && cd ..

# back to base processing dir
cd ..


### -- BUILD PREPROC ------------------------------------------------

echo Building PDE for JDK 1.5...

cd app

# long path is to avoid requiring java to be in your PATH
  echo Building antlr grammar code...

  # first build the default java goop
java \
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
java \
  -cp ../../../../build/cmd/work/lib/antlr.jar antlr.Tool \
  -o ../../processing/app/preproc \
  -glib java.g \
  ../../processing/app/preproc/pde.g
cd ../../..

# return to the root of the p5 folder
cd ..


### -- BUILD PDE ------------------------------------------------

cd app

rm -rf ../build/cmd/work/classes
mkdir ../build/cmd/work/classes

javac \
    -source 1.5 -target 1.5 \
    -classpath ../build/cmd/work/lib/core.jar:../build/cmd/work/lib/antlr.jar:../build/cmd/work/lib/ecj.jar:../build/cmd/work/lib/jna.jar:../build/cmd/work/java/lib/tools.jar \
    -d ../build/cmd/work/classes \
    src/processing/app/*.java \
    src/processing/app/debug/*.java \
    src/processing/app/preproc/*.java \
    src/processing/app/syntax/*.java \
    src/processing/app/tools/*.java \
    src/antlr/*.java \
    src/antlr/java/*.java 

cd ../build/cmd/work/classes
rm -f ../lib/pde.jar
zip -0rq ../lib/pde.jar .
cd ../../../..


### -- BUILD LIBRARIES ------------------------------------------------

cd build/cmd

PLATFORM=cmd
JAVAC="javac -source 1.5 -target 1.5"
CORE=../build/$PLATFORM/work/lib/core.jar
LIBRARIES=../build/$PLATFORM/work/libraries

# move to processing/build 
cd ..

# SERIAL LIBRARY
echo Building serial library...
cd ../serial
mkdir -p bin
$JAVAC \
    -classpath "library/RXTXcomm.jar:$CORE" \
    -d bin src/processing/serial/*.java 
rm -f library/serial.jar
cd bin && zip -r0q ../library/serial.jar processing/serial/*.class && cd ..
mkdir -p $LIBRARIES/serial/library/
cp library/serial.jar $LIBRARIES/serial/library/


# NET LIBRARY
echo Building net library...
cd ../net
mkdir -p bin
$JAVAC \
    -classpath "$CORE" \
    -d bin src/processing/net/*.java 
rm -f library/net.jar
cd bin && zip -r0q ../library/net.jar processing/net/*.class && cd ..
mkdir -p $LIBRARIES/net/library/
cp library/net.jar $LIBRARIES/net/library/


# OPENGL LIBRARY
echo Building OpenGL library...
cd ../opengl
mkdir -p bin
$JAVAC \
    -classpath "library/jogl.jar:$CORE" \
    -d bin src/processing/opengl/*.java 
rm -f library/opengl.jar
cd bin && zip -r0q ../library/opengl.jar processing/opengl/*.class && cd ..
mkdir -p $LIBRARIES/opengl/library/
cp library/opengl.jar $LIBRARIES/opengl/library/


# PDF LIBRARY
echo Building PDF library...
cd ../pdf
mkdir -p bin
$JAVAC \
    -classpath "library/itext.jar:$CORE" \
    -d bin src/processing/pdf/*.java 
rm -f library/pdf.jar
cd bin && zip -r0q ../library/pdf.jar processing/pdf/*.class && cd ..
mkdir -p $LIBRARIES/pdf/library/
cp library/pdf.jar $LIBRARIES/pdf/library/


# DXF LIBRARY
echo Building DXF library...
cd ../dxf
mkdir -p bin
$JAVAC \
    -classpath "$CORE" \
    -d bin src/processing/dxf/*.java 
rm -f library/dxf.jar
#find bin -name "*~" -exec rm -f {} ';'
cd bin && zip -r0q ../library/dxf.jar processing/dxf/*.class && cd ..
mkdir -p $LIBRARIES/dxf/library/
cp library/dxf.jar $LIBRARIES/dxf/library/


# XML LIBRARY
echo Building XML library...
cd ../xml
mkdir -p bin
$JAVAC \
    -classpath "$CORE" \
    -d bin src/processing/xml/*.java 
rm -f library/xml.jar
#find bin -name "*~" -exec rm -f {} ';'
cd bin && zip -r0q ../library/xml.jar processing/xml/*.class && cd ..
mkdir -p $LIBRARIES/xml/library/
cp library/xml.jar $LIBRARIES/xml/library/


# CANDY SVG LIBRARY
echo Building Candy SVG library...
cd ../candy
mkdir -p bin
$JAVAC \
    -classpath "../xml/library/xml.jar:$CORE" \
    -d bin src/processing/candy/*.java 
rm -f library/candy.jar
#find bin -name "*~" -exec rm -f {} ';'
cd bin && zip -r0q ../library/candy.jar processing/candy/*.class && cd ..
mkdir -p $LIBRARIES/candy/library/
cp library/candy.jar $LIBRARIES/candy/library/


echo
echo Done.
