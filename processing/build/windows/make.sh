#!/bin/sh


### -- SETUP WORK DIR -------------------------------------------

if test -d work
then
  BUILD_PREPROC=false
else
  echo Setting up directories to build P5...
  BUILD_PREPROC=true
  cp -r ../shared work
  rm -rf work/.svn
  rm -f work/.DS_Store 
  # in case one of those little mac poopers show up

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
  #chmod +x *.exe *.dll 
  #chmod +x client/*.dll
  cd ../../..

  mkdir work/lib/build
  #mkdir work/classes

  echo Compiling processing.exe
  cd launcher
  #chmod +x make.sh
  #./make.sh # && cp processing.exe ../work/
  make 
  make application.exe
  cd ..

  # get jikes and depedencies
  cp dist/jikes.exe work/
  #chmod +x work/jikes.exe

  cp dist/ICE_JNIRegistry.dll work/

  # chmod +x the crew
  find work -name "*.dll" -exec chmod +x {} ';'
  find work -name "*.exe" -exec chmod +x {} ';'
  find work -name "*.html" -exec chmod +x {} ';'
fi
  BUILD_PREPROC=true
cd ../..


### -- BUILD CORE ----------------------------------------------

echo Building processing.core

# move to core inside base 'processing' directory
cd core
#rm -f processing/core/*.class

CLASSPATH="..\\build\\windows\\work\\java\\lib\\rt.jar"
export CLASSPATH

perl preproc.pl

mkdir -p bin
../build/windows/work/jikes -d bin +D -target 1.1 src/processing/core/*.java
# use this from time to time to test 1.1 savviness
#/cygdrive/c/msjdk-4.0/bin/jvc /d . src/processing/core/*.java

rm -f ../build/windows/work/lib/core.jar
find bin -name "*~" -exec rm -f {} ';'

# package this folder into core.jar
cd bin && zip -rq ../../build/windows/work/lib/core.jar processing && cd ..

# back to base processing dir
cd ..


### -- BUILD PREPROC ---------------------------------------------

# i suck at shell scripting
#if [ $1 = "preproc" ] 
#then 
#BUILD_PREPROC=true
#fi

if $BUILD_PREPROC
then

echo Building PDE for JDK 1.4

cd app

# first build the default java goop
../build/windows/work/java/bin/java \
    -cp "..\\build\\windows\\work\\lib\\antlr.jar" antlr.Tool \
    -o src/antlr/java \
    src/antlr/java/java.g

# now build the pde stuff that extends the java classes
#../../build/windows/work/java/bin/java \
#    -cp "..\\..\\build\\windows\\work\\lib\\antlr.jar" antlr.Tool \
#    -o src/processing/app/preproc \
#    -glib antlr/java/java.g processing/app/preproc/pde.g

# this is totally ugly and needs to be fixed
# the problem is that -glib doesn't set the main path properly, 
# so it's necessary to cd into the antlr/java folder, otherwise
# the JavaTokenTypes.txt file won't be found

# this is the eventual hack to make things work
# why this is required on windows and not the others is beyond me
cp src/antlr/java/JavaTokenTypes.txt src/processing/app/preproc/

# this is a total disaster...fix for next release
cd src/processing/app/preproc
../../../../../build/windows/work/java/bin/java \
  -cp "..\\..\\..\\..\\..\\build\\windows\\work\\lib\\antlr.jar" antlr.Tool \
  -glib ../../../antlr/java/java.g \
  pde.g
cd ../../../..


# back to base processing dir
cd ..

fi


### -- BUILD PDE ------------------------------------------------

cd app

CLASSPATH="..\\build\\windows\\work\\lib\\core.jar;..\\build\\windows\\work\\lib\\apple.jar;..\\build\\windows\\work\\lib\antlr.jar;..\\build\\windows\\work\\lib\\oro.jar;..\\build\\windows\\work\\lib\\registry.jar;..\\build\\windows\\work\\lib\\tools.jar;..\\build\\windows\\work\\java\\lib\\rt.jar"

# compile the code as java 1.3, so that the application will run and
# show the user an error, rather than crapping out with some strange
# "class not found" crap
../build/windows/work/jikes -target 1.3 +D -classpath "$CLASSPATH;..\\build\\windows\\work/classes" -d ..\\build\\windows\\work/classes src/processing/app/*.java src/processing/app/debug/*.java src/processing/app/preproc/*.java src/processing/app/syntax/*.java src/processing/app/tools/*.java src/antlr/*.java src/antlr/java/*.java
#/cygdrive/c/jdk-1.4.2_05/bin/javac.exe -classpath $CLASSPATH -d ..\\build\\windows\\work/classes *.java jeditsyntax/*.java preprocessor/*.java

cd ../build/windows/work/classes
rm -f ../lib/pde.jar
zip -0rq ../lib/pde.jar .

# back to build/windows
cd ../..


### -- BUILD LIBRARIES ------------------------------------------------

PLATFORM=windows


CLASSPATH="..\\build\\$PLATFORM\\work\\lib\\core.jar;..\\build\\$PLATFORM\\work\\java\\lib\\rt.jar"
JIKES=../build/$PLATFORM/work/jikes
CORE="..\\build\\$PLATFORM\\work\\lib\\core.jar"
LIBRARIES="..\\build\\$PLATFORM\\work\\libraries"

# move to processing/build 
cd ..


# SERIAL LIBRARY
echo Building serial library...
cd ../serial
mkdir -p bin
$JIKES -target 1.1 +D \
    -classpath "library\\RXTXcomm.jar;$CORE;$CLASSPATH" \
    -d bin src/processing/serial/*.java 
rm -f library/serial.jar
find bin -name "*~" -exec rm -f {} ';'
cd bin && zip -r0q ../library/serial.jar processing/serial/*.class && cd ..
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


# VIDEO LIBRARY
echo Building video library...
if test -f "${QTJAVA}"
then 
  echo "Found Quicktime 7 installation"
else
  QTJAVA="$WINDIR\\system32\\QTJava.zip"
  if test -f "${QTJAVA}"
  then
    echo "Found Quicktime 6 at $QTJAVA"
  else 
    echo "Could not find QuickTime for Java,"
    echo "you'll need to install it before building."
    exit 1;
  fi
fi
cd ../video
mkdir -p bin
$JIKES -target 1.1 +D \
    -classpath "$QTJAVA;$CLASSPATH" \
    -d bin src/processing/video/*.java 
rm -f library/video.jar
find bin -name "*~" -exec rm -f {} ';'
cd bin && zip -r0q ../library/video.jar processing/video/*.class && cd ..
mkdir -p $LIBRARIES/video/library/
cp library/video.jar $LIBRARIES/video/library/


# OPENGL LIBRARY
echo Building OpenGL library...
cd ../opengl
mkdir -p bin
$JIKES -target 1.1 +D \
    -classpath "library\\jogl.jar;$CLASSPATH" \
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
    -classpath "library\\itext.jar;$CLASSPATH" \
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
    -classpath "..\\xml\\library\\xml.jar;$CLASSPATH" \
    -d bin src/processing/candy/*.java 
rm -f library/candy.jar
find bin -name "*~" -exec rm -f {} ';'
cd bin && zip -r0q ../library/candy.jar processing/candy/*.class && cd ..
mkdir -p $LIBRARIES/candy/library/
cp library/candy.jar $LIBRARIES/candy/library/


echo
echo Done.

