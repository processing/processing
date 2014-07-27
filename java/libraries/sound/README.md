## Processing MethCla Interface

This is a processing interface and a collection of plugins for MethCla, a leight-weight, efficient sound engine for mobile devices [methcla](http://methc.la). 


## Building the libMethClaInterface

The library requires a compiled shared library of MethCla for each platform. There are specific Makefile in the src folder which compile the JNI library. For the moment this library is OSX + Linux only. To build the JNI Lib simply rename the respective Makefile_x to Makefile and do 

$make 
$make install 

in the src/cpp folder.

The Java Library is to be compiled with ant. Please install the latest version on ant on your computer. The build.xml file is in in the root folder. Core.jar needs to be compiled and ready in ../../../core/library. To compile do

$ ant

in the root folder.