#!/bin/sh

CLASSPATH=java/lib/rt.jar:lib:lib/build:lib/pde.jar:lib/kjc.jar:lib/antlr.jar:lib/oro.jar:lib/RXTXcomm.jar
export CLASSPATH

exec java/bin/java PdeBase
