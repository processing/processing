@echo off

REM -- standard Proce55ing application build

REM set ME=application
REM set JRE=\jre-1.3.1_04-intl

rm -f classes/*.class

REM -- BUG need to test if lib/export exists, create it if not
REM --     may exist but is being pruned by cvs

REM -- need to remove sketchbook\standard from here
REM -- sketchbook needs to be compiled on startup or for
REM -- each compile of any given app

REM -- compile bagel and copy in its classes
REM -- also an extra set of files for the export-to-applet

cd ..
cd bagel

REM -- attempting to get serial to work
REM set CLASSPATH=d:\fry\processing\app\application\classes;%JRE%\lib\rt.jar;%JRE%\lib\ext\comm.jar;%CLASSPATH%
REM set CLASSPATH=..\app\application\classes;%JRE%\lib\ext\comm.jar;%CLASSPATH%
REM perl make.pl SERIAL

perl make.pl 
cp classes/*.class ../app/classes/
rm -f ../app/lib/export/*.class
cp classes/*.class ../app/lib/export/
cd ..
cd app

REM -- needs to happen before building b/c classpath needs to be set
set CLASSPATH2=%CLASSPATH%
REM set CLASSPATH=lib\kjc.jar;lib\oro.jar;java\lib\rt.jar;java\lib\ext\comm.jar;%CLASSPATH%
set CLASSPATH=classes;lib\kjc.jar;lib\oro.jar;java\lib\rt.jar;java\lib\ext\comm.jar

REM cd ..
REM perl buzz.pl "jikes +D -nowarn -d classes" -dJDK13 *.java kjc\*.java 
perl buzz.pl "jikes +D -deprecation -d classes" -dJDK13 *.java kjc\*.java 
REM cd application

REM rm -f lib/version

rem -- make pde.jar
cd classes
rm -f ..\lib\pde.jar
zip -0q ..\lib\pde.jar *.class
cd ..

rem -- build exe from the classes folder
REM cd classes
REM jexegen /w /main:PdeApplication /out:..\pde.exe *.class
REM cd ..

set CLASSPATH=%CLASSPATH2%