#!/bin/sh

CLASSPATH=java\\lib\\rt.jar\;lib\;lib\\build\;lib\\pde.jar\;lib\\kjc.jar\;lib\\antlr.jar\;lib\\oro.jar\;lib\\comm.jar\;lib\\RXTXcomm.jar
export CLASSPATH

cd work && ./java/bin/java -Djava.compiler=NONE PdeBase
