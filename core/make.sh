#!/bin/sh

#C:\jdk-1.4.2_05\bin
#/cygdrive/c/jdk-1.4.2_05/bin/javadoc -d doc processing.core *.java
/cygdrive/c/jdk-1.4.2_05/bin/javadoc -d doc *.java
jikes -d . +D *.java
#jikes -d . +D PApplet.java
