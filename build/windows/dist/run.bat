@echo off

REM --- if you need more ram, change the 64m (which means 
REM --- 64 megabytes) to something higher. 

set SAVEDCP=%CLASSPATH%
REM set CLASSPATH=%CLASSPATH%;java\lib\rt.jar;lib;lib\build;lib\pde.jar;lib\kjc.jar;lib\antlr.jar;lib\oro.jar;lib\comm.jar;c:\winnt\system32\qtjava.zip;c:\windows\system32\qtjava.zip
set CLASSPATH=%CLASSPATH%;java\lib\rt.jar;lib;lib\build;lib\pde.jar;lib\kjc.jar;lib\antlr.jar;lib\oro.jar;lib\comm.jar;%QTJAVA%

java -ms64m -mx64m PdeBase

set CLASSPATH=%SAVEDCP%
