@echo off

REM --- if you need more ram, change the 64m (which means 
REM --- 64 megabytes) to something higher. 

java -ms64m -mx64m -cp lib;lib\build;lib\pde.jar;lib\kjc.jar;lib\oro.jar;lib\comm.jar PdeBase
