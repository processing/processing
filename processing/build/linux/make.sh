#!/bin/sh


### -- SETUP WORK DIR -------------------------------------------

if test -d work
then

else
  echo Setting up directories to build for linux...
  mkdir work
  tar --extract --verbose --file=jre.tgz --ungzip --directory=work
  cp -r ../shared/lib work/
  mkdir work/lib/export
  mkdir work/lib/build
  cp -r ../shared/sketchbook work/
  mkdir work/classes
  # this will copy cvs files intact, meaning that changes
  # could be made and checked back in.. interesting
fi


### -- START BUILDING -------------------------------------------

# move to 'app' directory
cd ../..


PLATFORM_CLASSPATH=java/lib/rt.jar:java/lib/ext/comm.jar


### -- BUILD BAGEL ----------------------------------------------
cd ..
cd bagel

# hmm?
#CLASSPATH=java\lib\ext\comm.jar\;$CLASSPATH

### --- make version with serial for the application
echo Building bagel with serial support
perl make.pl SERIAL
cp classes/*.class ../app/build/windows/work/classes/

### --- make version without serial for applet exporting
echo Building bagel for export
perl make.pl
cp classes/*.class ../app/build/windows/work/lib/export/

cd ..
cd app


### -- BUILD PDE ------------------------------------------------

echo Building PDE for JDK 1.3
CLASSPATH=classes:lib/kjc.jar:lib/oro.jar:$PLATFORM_CLASSPATH

perl buzz.pl "jikes +D -classpath $CLASSPATH -d classes" -dJDK13 *.java kjc/*.java 

cd classes
rm -f ../lib/pde.jar
zip -0q ../lib/pde.jar *.class
cd ..

