


# Processing MethCla Interface

This is a processing interface and a collection of plugins for MethCla, a leight-weight, efficient sound engine for mobile devices [methcla](http://methc.la). 

## Executable

Download the current version of the library here:

[Sound](https://github.com/wirsing/ProcessingSound/releases)


## Building the libMethClaInterface

The library requires a compiled shared library of MethCla for each platform. There are specific Makefile in the src folder which compile the JNI library. For the moment this library is OsX only. 

The Java Library is to be compiled with ant. Please install the latest version on ant on your computer. The build.xml file is in resources. To compile do

$ cd resources
$ ant
