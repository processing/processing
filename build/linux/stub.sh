#!/bin/sh

CLASSPATH=java/lib/rt.jar:java/lib/jaws.jar:lib:lib/build:lib/pde.jar:lib/kjc.jar:lib/antlr.jar:lib/oro.jar:lib/RXTXcomm.jar
export CLASSPATH

# put the directory where this file lives in the front of the path, because
# that directory also contains jikes, which we will need at runtime.
#
PATH=`pwd`/`dirname $0`:`pwd`/java/bin:${PATH}
export PATH

#exec java/bin/java PdeBase
java PdeBase
