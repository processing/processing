#!/bin/sh

CLASSPATH=java\\lib\\rt.jar\;lib\;lib\\build\;lib\\pde.jar\;lib\\kjc.jar\;lib\\oro.jar\;java\\lib\\ext\\comm.jar
export CLASSPATH

cd work && ./java/bin/java -Djava.compiler=NONE PdeBase
