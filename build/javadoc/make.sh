#!/bin/sh

javadoc -public -notimestamp -d core \
    ../../core/src/processing/core/*.java \
    ../../core/src/processing/data/*.java \
    ../../core/src/processing/event/*.java \
    ../../core/src/processing/opengl/*.java \

# These have to be done in a certain order... Most classes need to know about
# core, and SVG needs to have the XML library opened earler. I'm probably not
# setting this up right, so if anyone knows how to do it without specifying
# all the directories like this, please let us know.
javadoc -public -notimestamp -d everything \
    -classpath ../../app/lib/antlr.jar:../../app/lib/jna.jar:../../serial/library/RXTXcomm.jar:../../opengl/library/jogl.jar:../../pdf/library/itext.jar:../../app/lib/ecj.jar \
    ../../core/src/processing/core/*.java \
    ../../core/src/processing/data/*.java \
    ../../core/src/processing/event/*.java \
    ../../core/src/processing/opengl/*.java \
    ../../app/src/antlr/*.java \
    ../../app/src/processing/app/*.java \
    ../../app/src/processing/app/exec/*.java \
    ../../app/src/processing/app/linux/*.java \
    ../../app/src/processing/app/macosx/*.java \
    ../../app/src/processing/app/syntax/*.java \
    ../../app/src/processing/app/tools/*.java \
    ../../app/src/processing/app/windows/*.java \
    ../../app/src/processing/mode/android/*.java \
    ../../app/src/processing/mode/java/*.java \
    ../../app/src/processing/mode/java/preproc/*.java \
    ../../app/src/processing/mode/java/runner/*.java \
    ../../app/src/processing/mode/javascript/*.java \
    ../../java/libraries/dxf/src/processing/dxf/*.java \
    ../../java/libraries/net/src/processing/net/*.java \
    ../../java/libraries/opengl/src/processing/opengl/*.java \
    ../../java/libraries/pdf/src/processing/pdf/*.java \
    ../../java/libraries/serial/src/processing/serial/*.java \
    ../../java/libraries/video/src/processing/video/*.java 

cp stylesheet.css core/
cp stylesheet.css everything/
cp index.html core/
cp index.html everything/
