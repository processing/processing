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

  cp dist/lib/pde_linux.properties work/lib/

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

CLASSPATH=/opt/java/lib/rt.jar:/opt/java/lib/ext/comm.jar
#CLASSPATH=../app/build/linux/work/java/lib/rt.jar:../app/build/linux/work/java/lib/ext/comm.jar
export CLASSPATH

### --- make version with serial for the application
echo Building bagel with serial and sonic support
perl make.pl SERIAL SONIC JDK13
cp classes/*.class ../build/linux/work/classes/

### --- make version without serial for applet exporting
echo Building bagel for export with sonic
perl make.pl SONIC
cp classes/*.class ../build/linux/work/lib/export/

cd ..
cd app


### -- BUILD PDE ------------------------------------------------

echo Building PDE for JDK 1.3

CLASSPATH=../build/linux/work/classes:../build/linux/work/lib/kjc.jar:../build/linux/work/lib/oro.jar:../build/linux/work/java/lib/rt.jar:../build/linux/work/java/lib/ext/comm.jar

perl ../bagel/buzz.pl "jikes +D -classpath $CLASSPATH -d ../build/linux/work/classes" -dJDK13 *.java jeditsyntax/*.java

cd ../build/linux/work/classes
rm -f ../lib/pde.jar
zip -0q ../lib/pde.jar *.class
cd ../..


### -- BUILD STUB -----------------------------------------------

install -m 755 stub.sh work/Processing

