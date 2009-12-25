#!/bin/sh

javadoc -doclet ProcessingWeblet -public -webref ../generated_reference -localref ../local_reference -templatedir ../data/website/templates -examplesdir ../data/website/examples \
	-includedir ../data/website/examples/include -imagedir images \
	-corepackage processing.xml \
    ../data/core/src/processing/core/*.java \
    ../data/core/src/processing/xml/*.java \
    ../data/net/src/processing/net/*.java \
    ../data/video/src/processing/video/*.java \
    ../data/serial/src/processing/serial/*.java
#encountering svn permission issues when overwriting stuff   
#cp -r ../data/website/css	 ../generated_reference
#cp -r ../data/images ../generated_reference