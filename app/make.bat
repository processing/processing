@echo off

REM -- standard Proce55ing application build

REM -- BUG need to test if lib/export exists, create it if not
REM --     may exist but is being pruned by cvs


REM -- BUILD BAGEL ----------------------------------------------
cd ..
cd bagel

set SAVED_CLASSPATH = %CLASSPATH%
set CLASSPATH = java\lib\ext\comm.jar;%CLASSPATH%

REM --- make version with serial for the application
echo Building bagel with serial support
perl make.pl SERIAL
cp classes/*.class ../app/classes/

REM --- make version without serial for applet exporting
echo Building bagel for export
perl make.pl
cp classes/*.class ../app/lib/export/

cd ..
cd app

set CLASSPATH = %SAVED_CLASSPATH%


REM -- BUILD PDE ------------------------------------------------

echo Building PDE for JDK 1.3
set CLASSPATH2=%CLASSPATH%
set CLASSPATH=classes;lib\kjc.jar;lib\oro.jar;java\lib\rt.jar;java\lib\ext\comm.jar

REM rm -f classes/*.class

perl buzz.pl "jikes +D -d classes" -dJDK13 *.java kjc\*.java 

cd classes
rm -f ..\lib\pde.jar
zip -0q ..\lib\pde.jar *.class
cd ..

set CLASSPATH=%CLASSPATH2%


REM -- BUILD PDE for 1.1 ----------------------------------------

if "%1" == "jdk11" goto buildjdk11
goto skipjdk11

:buildjdk11
echo Building PDE for JDK 1.1
set CLASSPATH2=%CLASSPATH%
set CLASSPATH=mac\classes;lib\kjc.jar;lib\oro.jar;mac\rt.jar;java\lib\ext\comm.jar

cp ../bagel/classes/*.class mac/classes

perl buzz.pl "jikes +D -d mac/classes" *.java kjc\*.java 

cd mac\classes
rm -f ..\pde.jar
zip -0q ..\pde.jar *.class
cd ..\..

set CLASSPATH=%CLASSPATH2%

:skipjdk11


REM -------------------------------------------------------------

rem -- build exe from the classes folder
REM cd classes
REM jexegen /w /main:PdeApplication /out:..\pde.exe *.class
REM cd ..

