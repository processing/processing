#!/bin/sh


### -- CHECK TO MAKE SURE BAGEL EXISTS -------------------------

# move to base 'processing' directory
cd ../..

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

# back to where we came from
cd build/windows


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
  chmod -R +x *.exe *.dll
  cd ../../..
  pwd
  #chmod -R +x work/java/bin/*.exe
  #chmod +x work/java/bin/*.dll
  #chmod +x work/java/bin/client/*.dll

  mkdir work/lib/export
  mkdir work/lib/build
  # this will copy cvs files intact, meaning that changes
  # could be made and checked back in.. interesting
  mkdir work/classes

  cp dist/lib/pde_windows.properties work/lib/
  echo Compiling processing.exe
  cd launcher && make && cp processing.exe ../work/ && cd ..

  # get the serial stuff
  echo Copying serial support from bagel dir
  cp ../../bagel/serial/comm.jar work/lib/
  cp ../../bagel/serial/javax.comm.properties work/lib/
  cp ../../bagel/serial/win32com.dll work/
  chmod +x work/win32com.dll

  # get jikes and depedencies
  gunzip < dist/jikes.gz > work/jikes.exe
  chmod +x work/jikes.exe
fi


### -- START BUILDING -------------------------------------------

# move to base 'processing' directory
cd ../..


### -- BUILD BAGEL ----------------------------------------------

cd bagel

# clear jikespath to avoid problems if it is defined elsewhere 
unset JIKESPATH

if test -d /cygdrive/c/WINNT
then
  # Windows 2000 or NT
  QT_JAVA_PATH="C:\\WINNT\\system32\\QTJava.zip"
else
  # Windows 95, 98, ME and XP (does it really run on 95?)
  QT_JAVA_PATH="C:\\WINDOWS\\system32\\QTJava.zip"
fi

# new regular version
CLASSPATH="..\\build\\windows\\work\\java\\lib\\rt.jar;..\\build\\windows\\work\\lib\\comm.jar;${QT_JAVA_PATH}"

# make version with serial for the application
echo Building bagel with serial, sonic, video and jdk13 support
perl make.pl SERIAL SONIC VIDEO JDK13
cp classes/*.class ../build/windows/work/classes/

# make simpler version for applet exporting, only 1.1 functions
echo Building bagel for export with sonic support
perl make.pl SONIC
cp classes/*.class ../build/windows/work/lib/export/

cd ..


### -- BUILD PDE ------------------------------------------------

echo Building PDE for JDK 1.4

cd app

cd preprocessor

# first build the default java goop
java -cp "..\\..\\build\\windows\\work\\lib\\antlr.jar" antlr.Tool java.g
java -cp "..\\..\\build\\windows\\work\\lib\\antlr.jar" antlr.Tool java.tree.g 

# now build the pde stuff that extends the java classes
java -cp "..\\..\\build\\windows\\work\\lib\\antlr.jar" antlr.Tool \
    -glib java.g pde.g
java -cp "..\\..\\build\\windows\\work\\lib\\antlr.jar" antlr.Tool \
    -glib java.tree.g pde.tree.g
cd ..

CLASSPATH="..\\build\\windows\\work\\classes;..\\build\\windows\\work\\lib\\kjc.jar;..\\build\\windows\\work\\lib\antlr.jar;..\\build\\windows\\work\\lib\\oro.jar;..\\build\\windows\\work\\java\\lib\\rt.jar;..\\build\\windows\\work\\lib\\comm.jar;${QT_JAVA_PATH}"

perl ../bagel/buzz.pl "jikes +D -classpath \"$CLASSPATH\" -d \"..\\build\\windows\\work/classes\"" -dJDK13 -dJDK14 *.java jeditsyntax/*.java preprocessor/*.java

cd ../build/windows/work/classes
rm -f ../lib/pde.jar
zip -0rq ../lib/pde.jar .

