#!/bin/sh

CLASSPATH=/System/Library/Frameworks/JavaVM.framework/Versions/1.3.1/Classes/classes.jar:/System/Library/Frameworks/JavaVM.framework/Versions/1.3.1/Classes/ui.jar:/System/Library/Frameworks/JavaVM.framework/Versions/1.3.1/Home/lib/ext/comm.jar:/System/Library/Java/Extensions/QTJava.zip:lib:lib/build:lib/pde.jar:lib/kjc.jar:lib/oro.jar:../comm.jar

cd work && /System/Library/Frameworks/JavaVM.framework/Versions/1.3.1/Commands/java -cp PdeBase

# -Dcom.apple.hwaccel=false 
#cd work && java -cp lib:lib/build:lib/pde.jar:lib/kjc.jar:lib/oro.jar:../comm.jar PdeBase
