#!/bin/sh


### -- BUILD BAGEL ----------------------------------------------
cd ..
cd bagel

# hmm?
#CLASSPATH=java\lib\ext\comm.jar\;$CLASSPATH

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


### -- BUILD PDE ------------------------------------------------

echo Building PDE for JDK 1.3
#CLASSPATH=classes:lib/kjc.jar:lib/oro.jar:java/lib/rt.jar:java/lib/ext/comm.jar
CLASSPATH=classes:lib/kjc.jar:lib/oro.jar:$CLASSPATH

perl buzz.pl "jikes +D -classpath $CLASSPATH -d classes" -dJDK13 *.java kjc/*.java 

cd classes
rm -f ../lib/pde.jar
zip -0q ../lib/pde.jar *.class
cd ..

