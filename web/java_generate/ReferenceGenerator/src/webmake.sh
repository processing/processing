#!/bin/sh

javadoc -doclet ProcessingWeblet -public -webref ../tmp/web -localref ../tmp/local -templatedir ../data/website/templates -examplesdir ../data/website/examples \
	-includedir ../data/website/examples/include -imagedir images \
	-corepackage processing.xml \
    ../../../processing/core/src/processing/core/*.java \
    ../../../processing/core/src/processing/xml/*.java \
    ../../../processing/net/src/processing/net/*.java \
    ../../../processing/video/src/processing/video/*.java \
    ../../../processing/serial/src/processing/serial/*.java
#encountering svn permission issues when overwriting stuff   
#cp -r ../data/website/css	 ../generated_reference
#cp -r ../data/images ../generated_reference