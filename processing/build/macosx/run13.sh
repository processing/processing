#!/bin/sh

cd work && /System/Library/Frameworks/JavaVM.framework/Versions/1.3.1/Commands/java -cp lib:lib/build:lib/pde.jar:lib/kjc.jar:lib/oro.jar:lib/gl4java.jar:../comm.jar PdeBase

# -Dcom.apple.hwaccel=false 
#cd work && java -cp lib:lib/build:lib/pde.jar:lib/kjc.jar:lib/oro.jar:../comm.jar PdeBase
