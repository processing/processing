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
  cd work/sketchbook
  unzip -q examples.zip
  rm examples.zip
  cd ../..

  echo Extracting reference...
  cd work
  unzip -q reference.zip
  rm reference.zip
  cd ..

  echo Extracting enormous JRE...
  unzip -q -d work jre.zip
  # cygwin requires this because of unknown weirdness
  # it was not formerly this anal retentive
  chmod +x work/java/bin/*.exe
  chmod +x work/java/bin/*.dll
  chmod +x work/java/bin/client/*.dll

  mkdir work/lib/export
  mkdir work/lib/build
  # this will copy cvs files intact, meaning that changes
  # could be made and checked back in.. interesting
  mkdir work/classes

  cp dist/lib/pde_windows.properties work/lib/
  echo Compiling processing.exe
  cd launcher && make && cp processing.exe ../work/ && cd ..

  echo
fi


### -- START BUILDING -------------------------------------------

# move to base 'processing' directory
cd ../..


### -- BUILD BAGEL ----------------------------------------------

# make sure bagel exists, if not, check it out of cvs
if test -d bagel
then 
else
  echo Doing CVS checkout of bagel...
  cvs co bagel
  cd bagel
  cvs update -P
  cd ..
fi

cd bagel

# clear jikespath to avoid problems if it is defined elsewhere 
unset JIKESPATH

if test -d /cygdrive/c/WINNT
then
  # windows 2000 or nt
  QT_JAVA_PATH="C:\\WINNT\\system32\\QTJava.zip"
else
  # other versions of windows, including xp
  QT_JAVA_PATH="C:\\WINDOWS\\system32\\QTJava.zip"
fi
# another alternative
#QT_JAVA_PATH=../build/shared/lib/qtjava.zip

CLASSPATH="..\\build\\windows\\work\\java\\lib\\rt.jar;..\\build\\windows\\work\\java\\lib\\ext\\comm.jar;${QT_JAVA_PATH}"

### --- make version with serial for the application
echo Building bagel with serial, sonic and video support
perl make.pl SERIAL SONIC VIDEO
cp classes/*.class ../build/windows/work/classes/

### --- make simpler version for applet exporting
echo Building bagel for export with sonic support
perl make.pl SONIC
cp classes/*.class ../build/windows/work/lib/export/

cd ..


### -- BUILD PDE ------------------------------------------------

echo Building PDE for JDK 1.4

cd app

CLASSPATH="..\\build\\windows\\work\\classes;..\\build\\windows\\work\\lib\\kjc.jar;..\\build\\windows\\work\\lib\\oro.jar;..\\build\\windows\\work\\java\\lib\\rt.jar;..\\build\\windows\\work\\java\\lib\\ext\\comm.jar;${QT_JAVA_PATH}"


# version for javac/1.1 testing
#CLASSPATH="..\\build\\windows\\work\\classes;..\\build\\windows\\work\\lib\\kjc.jar;..\\build\\windows\\work\\lib\\oro.jar;..\\build\\windows\\work\\java\\lib\\rt.jar;..\\build\\windows\\work\\java\\lib\\ext\\comm.jar;..\\build\\macos9\\JDKClasses.zip;..\\build\\macos9\\JDKToolsClasses.zip"

perl ../bagel/buzz.pl "jikes +D -classpath \"$CLASSPATH\" -d \"..\\build\\windows\\work/classes\"" -dJDK13 -dJDK14 *.java jeditsyntax/*.java

# version for javac/1.1 testing
#perl ../bagel/buzz.pl "jikes -target 1.1 +D -classpath \"$CLASSPATH\" -d \"..\\build\\windows\\work/classes\"" -dJAVAC *.java jeditsyntax/*.java
#perl ../bagel/buzz.pl "javac -classpath \"$CLASSPATH\" -d \"..\\build\\windows\\work/classes\"" -dJAVAC *.java jeditsyntax/*.java

cd ../build/windows/work/classes
rm -f ../lib/pde.jar
zip -0q ../lib/pde.jar *.class

# back to 'build' dir
cd ../../..
