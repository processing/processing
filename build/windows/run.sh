#!/bin/sh

if test -d /cygdrive/c/WINNT
then
  # windows 2000 or nt
  QT_JAVA_PATH=C:\\WINNT\\system32\\QTJava.zip
else
  # other versions of windows, including xp
  QT_JAVA_PATH=C:\\WINDOWS\\system32\\QTJava.zip
fi
# another alternative
#QT_JAVA_PATH=..\\build\\shared\\lib\\qtjava.zip


CLASSPATH=lib\;lib\\build\;lib\\pde.jar\;lib\\kjc.jar\;lib\\oro.jar\;java\\lib\\ext\\comm.jar\;${QT_JAVA_PATH}

cd work && ./java/bin/java -cp ${CLASSPATH} PdeBase
