#!/bin/sh

#C:\jdk-1.4.2_05\bin
#/cygdrive/c/jdk-1.4.2_05/bin/javadoc -d doc *.java
#javadoc -public -d doc *.java
./preproc.pl
jikes -d . +D *.java
