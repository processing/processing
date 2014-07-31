#!/bin/bash
# this is a small script to create the .h file for the JNIlib and put it into the right directory.

cd ../processing/sound 
javac MethClaInterface.java
cd ../../
javah processing.sound.MethClaInterface
mv processing_sound_MethClaInterface.h cpp/include
rm processing/sound/MethClaInterface.class
