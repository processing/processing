#!/bin/sh


# move to base 'processing' directory
#cd ../..


### -- CHECK TO MAKE SURE BAGEL EXISTS -------------------------

# make sure bagel exists, if not, check it out of cvs
#if test -d bagel
#then 
#else
#  echo Doing CVS checkout of bagel...
#  cvs co bagel
#  cd bagel
#  cvs update -P
#  cd ..
#fi


### -- SETUP WORK DIR -------------------------------------------

# back to where we came from
#cd build/windows

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

#  mkdir work/lib/export
  mkdir work/lib/build
  # this will copy cvs files intact, meaning that changes
  # could be made and checked back in.. interesting
  mkdir work/classes

  # no longer needed with megabucket
  #cp dist/lib/pde_windows.properties work/lib/

  # java application stubs
  #cp dist/lib/mrj.jar work/lib/

  echo Compiling processing.exe
  cd launcher
  make && cp processing.exe ../work/
  cd ..

  # get the serial stuff
#  echo Copying serial support from processing.lib dir...
#  cp ../../lib/serial/comm.jar work/lib/
#  cp ../../lib/serial/javax.comm.properties work/lib/
#  cp ../../lib/serial/win32com.dll work/
#  chmod +x work/win32com.dll

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

# clear jikespath to avoid problems if it is defined elsewhere 
#unset JIKESPATH

#QT_JAVA_PATH="$WINDIR\\system32\\QTJava.zip"
#if test -f "${QT_JAVA_PATH}"
#then
#  #echo "Found Quicktime at $QT_JAVA_PATH"
#else 
#  QT_JAVA_PATH="$WINDIR\\system\\QTJava.zip"
#  if test -f "${QT_JAVA_PATH}"
#    echo "could not find qtjava.zip in either"
#    echo "${WINDIR}\\system32\\qtjava.zip or"
#    echo "${WINDIR}\\system\\qtjava.zip"
#    echo "quicktime for java must be installed before building."
#    exit 1;
#  then
#    #echo "Found Quicktime at $QT_JAVA_PATH"
#  else
#  fi
#fi

# new regular version
#CLASSPATH="..\\build\\windows\\work\\java\\lib\\rt.jar;..\\build\\windows\\work\\lib\\comm.jar;${QT_JAVA_PATH}"
CLASSPATH="..\\build\\windows\\work\\java\\lib\\rt.jar"
export CLASSPATH

perl preproc.pl
../build/windows/work/jikes -d . +D -target 1.1 *.java
zip -rq ../build/windows/work/lib/core.jar processing

#perl make.pl JIKES=../build/windows/work/jikes JDK13
#cp classes/*.class ../build/windows/work/classes/

#echo Building export classes for 1.1
#rm -f classes/*.class
#perl make.pl JIKES=../build/windows/work/jikes
#cd classes
#zip -0q ../../build/windows/work/lib/export11.jar *.class
#cd ..

#echo Building export classes for 1.3
#rm -f classes/*.class
#perl make.pl JIKES=../build/windows/work/jikes
#cd classes
#zip -0q ../../build/windows/work/lib/export13.jar *.class
#cd ..



# back to base processing dir
cd ..

#################### TEMPORARY #####################
#if false
#then
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
#fi
#################### TEMPORARY #####################


### -- BUILD PDE ------------------------------------------------

cd app

#CLASSPATH="..\\build\\windows\\work\\lib\\core.jar;..\\build\\windows\\work\\lib\\mrj.jar;..\\build\\windows\\work\\lib\antlr.jar;..\\build\\windows\\work\\lib\\oro.jar;..\\build\\windows\\work\\java\\lib\\rt.jar;..\\build\\windows\\work\\lib\\comm.jar"
CLASSPATH="..\\build\\windows\\work\\lib\\core.jar;..\\build\\windows\\work\\lib\\mrj.jar;..\\build\\windows\\work\\lib\antlr.jar;..\\build\\windows\\work\\lib\\oro.jar;..\\build\\windows\\work\\java\\lib\\rt.jar"

#perl ../bagel/buzz.pl "../build/windows/work/jikes +D -classpath \"$CLASSPATH\" -d \"..\\build\\windows\\work/classes\"" -dJDK13 -dJDK14 *.java jeditsyntax/*.java preprocessor/*.java
../build/windows/work/jikes +D -classpath $CLASSPATH -d ..\\build\\windows\\work/classes *.java jeditsyntax/*.java preprocessor/*.java

cd ../build/windows/work/classes
rm -f ../lib/pde.jar
zip -0rq ../lib/pde.jar .

# back to build/windows
cd ../..


### -- BUILD LIBRARIES ------------------------------------------------

CLASSPATH="..\\..\\build\\windows\\work\\lib\\core.jar;..\\..\\build\\windows\\work\\java\\lib\\rt.jar"


cd ../../lib/serial
../../build/windows/work/jikes +D -classpath "RXTXcomm.jar;$CLASSPATH" -d . *.java 
zip -r0q serial.jar processing
rm -rf processing
cp serial.jar "C:\\Documents and Settings\\fry\\My Documents\\sketchbook\\rxtx_work\\code"


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
../../build/windows/work/jikes +D -classpath "$QTJAVA;$CLASSPATH" -d . PCamera.java 
zip -r0q video.jar processing
rm -rf processing
cp video.jar "C:\\Documents and Settings\\fry\\My Documents\\sketchbook\\new_camera_action\\code"

