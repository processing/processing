#!/bin/sh

### standard Proce55ing application build

### -- BUG need to test if lib/export exists, create it if not
### --     may exist but is being pruned by cvs


### -- BUILD BAGEL ----------------------------------------------
cd ..
cd bagel

set SAVED_CLASSPATH=$CLASSPATH
set CLASSPATH=java\lib\ext\comm.jar\;$CLASSPATH

### --- make version with serial for the application
echo Building bagel with serial support
perl make.pl SERIAL
cp classes/*.class ../app/classes/

### --- make version without serial for applet exporting
echo Building bagel for export
perl make.pl
cp classes/*.class ../app/lib/export/

cd ..
cd app

#set CLASSPATH = %SAVED_CLASSPATH%
set CLASSPATH=$SAVED_CLASSPATH


### -- BUILD PDE ------------------------------------------------

echo Building PDE for JDK 1.3
#CLASSPATH2=$CLASSPATH
CLASSPATH=classes:lib/kjc.jar:lib/oro.jar:java/lib/rt.jar:java/lib/ext/comm.jar
#echo cp2 is now ${CLASSPATH2}
#echo classpath is now $CLASSPATH

#rm -f classes/*.class

perl buzz.pl "jikes +D -classpath $CLASSPATH -d classes" -dJDK13 *.java kjc/*.java 
#perl buzz.pl "jikes +D -d classes" -dJDK13 *.java kjc/*.java 

cd classes
rm -f ../lib/pde.jar
zip -0q ../lib/pde.jar *.class
cd ..

#CLASSPATH=$CLASSPATH2

