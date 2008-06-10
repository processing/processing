#!/bin/sh

if test -f "${QTJAVA}"
then 
  echo "Found Quicktime 7 installation"
else
  QTJAVA="$WINDIR\\system32\\QTJava.zip"
  if test -f "${QTJAVA}"
  then
    echo "Found Quicktime 6 at $QTJAVA"
  else 
    echo "Could not find QuickTime for Java,"
    echo "you'll need to install it before running."
    exit 1;
  fi
fi

CLASSPATH=lib\\pde.jar\;lib\\core.jar\;lib\\jna.jar\;lib\\antlr.jar\;java\\lib\\tools.jar\;${QTJAVA}
export CLASSPATH

cd work && ./java/bin/java processing.app.Base
