#!/bin/sh
# -Dcom.apple.hwaccel=false 

# old rxtx
#CLASSPATH=/System/Library/Frameworks/JavaVM.framework/Classes/classes.jar:/System/Library/Frameworks/JavaVM.framework/Classes/ui.jar:/System/Library/Frameworks/JavaVM.framework/Home/lib/ext/comm.jar:/System/Library/Java/Extensions/QTJava.zip:lib:lib/build:lib/pde.jar:lib/kjc.jar:lib/oro.jar:../comm.jar

# rxtx 2.1.6
#CLASSPATH=/System/Library/Frameworks/JavaVM.framework/Classes/classes.jar:/System/Library/Frameworks/JavaVM.framework/Classes/ui.jar:/System/Library/Frameworks/JavaVM.framework/Home/lib/ext/comm.jar:/System/Library/Java/Extensions/QTJava.zip:lib:lib/build:lib/pde.jar:lib/kjc.jar:lib/oro.jar:lib/RXTXcomm.jar

# is qt java already included tho?
CLASSPATH=/System/Library/Java/Extensions/QTJava.zip:lib:lib/build:lib/pde.jar:lib/kjc.jar:lib/oro.jar:lib/RXTXcomm.jar

export CLASSPATH

#cd work && java -Dcom.apple.awt.antialiasing=off PdeBase
cd work && java PdeBase
