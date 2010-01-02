#!/bin/sh
#remove everything old
rm -rf ../tmp
#generate everything new
javadoc -doclet ProcessingWeblet -docletpath bin/ -public \
	-webref ../tmp/web \
	-localref ../tmp/local \
	-templatedir ../templates \
	-examplesdir ../api_examples \
	-includedir ../api_examples/include \
	-imagedir images \
	-corepackage processing.xml \
    ../../../processing/core/src/processing/core/*.java \
    ../../../processing/core/src/processing/xml/*.java
	# ../../../processing/serial/src/processing/serial/*.java
    # ../../../processing/net/src/processing/net/*.java \
    # ../../../processing/video/src/processing/video/*.java \

cp -r ../../css	 ../tmp/web
cp -r ../../css	 ../tmp/local
mkdir ../tmp/web/images
mkdir ../tmp/local/images
cp -r ../../content/api_media/*.jpg ../tmp/web/images/
cp -r ../../content/api_media/*.gif ../tmp/web/images/
cp -r ../../content/api_media/*.png ../tmp/web/images/
cp -r ../../content/api_media/*.jpg ../tmp/local/images/
cp -r ../../content/api_media/*.gif ../tmp/local/images/
cp -r ../../content/api_media/*.png ../tmp/local/images/