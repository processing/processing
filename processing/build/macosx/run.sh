#!/bin/sh

# -Dcom.apple.hwaccel=false 
#cd work && java -Dcom.apple.awt.antialiasing=off PdeBase

# old rxtx
#CLASSPATH=/System/Library/Frameworks/JavaVM.framework/Classes/classes.jar:/System/Library/Frameworks/JavaVM.framework/Classes/ui.jar:/System/Library/Frameworks/JavaVM.framework/Home/lib/ext/comm.jar:/System/Library/Java/Extensions/QTJava.zip:lib:lib/build:lib/pde.jar:lib/kjc.jar:lib/oro.jar:../comm.jar

# rxtx 2.1.6
#CLASSPATH=/System/Library/Frameworks/JavaVM.framework/Classes/classes.jar:/System/Library/Frameworks/JavaVM.framework/Classes/ui.jar:/System/Library/Frameworks/JavaVM.framework/Home/lib/ext/comm.jar:/System/Library/Java/Extensions/QTJava.zip:lib:lib/build:lib/pde.jar:lib/kjc.jar:lib/oro.jar:lib/RXTXcomm.jar

# is qt java already included tho?
CLASSPATH=/System/Library/Java/Extensions/QTJava.zip:lib:lib/build:lib/pde.jar:lib/core.jar:lib/antlr.jar:lib/oro.jar:lib/RXTXcomm.jar

export CLASSPATH

#cd work && /System/Library/Frameworks/JavaVM.framework/Versions/1.3.1/Commands/java -Dcom.apple.macos.useScreenMenuBar=true PdeBase

cd work && java -Dapple.laf.useScreenMenuBar=true -Dapple.awt.showGrowBox=false PdeBase
