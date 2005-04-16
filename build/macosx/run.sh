#!/bin/sh

# is qt java already included tho?
CLASSPATH=/System/Library/Java/Extensions/QTJava.zip:lib:lib/build:lib/pde.jar:lib/core.jar:lib/antlr.jar:lib/oro.jar:lib/registry.jar

export CLASSPATH

#cd work && /System/Library/Frameworks/JavaVM.framework/Versions/1.3.1/Commands/java -Dcom.apple.macos.useScreenMenuBar=true PdeBase
cd work && java -Dapple.laf.useScreenMenuBar=true -Dapple.awt.showGrowBox=false processing.app.Base
