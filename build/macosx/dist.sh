#!/bin/sh


if test -f /sw/bin/head
then
  # old 4 char version.. osx only uses the two chars
  #REVISION=`head -c 4 ../../todo.txt`
  # a more useful version of head than what's included with osx
  REVISION=`head -c 4 ../../todo.txt | tail -c 2`
else
  # can't get four bytes of head (osx doesn't support -c)
  REVISION=0000
fi

VERSIONED=`cat ../../app/PdeBase.java | grep 00$REVISION`
if [ -z "$VERSIONED" ]
then
  echo Fix the revision number in PdeBase.java
  exit
fi


./make.sh

echo Creating P5 distribution for revision $REVISION...

# remove any old boogers
rm -rf processing 
rm -rf Processing*
#rm -rf processing-*

# use 'shared' files as starting point
cp -r ../shared processing

# add the libraries folder with source
cp -r ../../lib processing/libraries

# new style examples thing ala reas
cd processing
unzip -q examples.zip
rm examples.zip
cd ..

# new style reference
cd processing
unzip -q reference.zip
rm reference.zip
cd ..

# clean out the cvs entries
find processing -name "CVS" -exec rm -rf {} ';'
#find processing -name "CVS" -exec echo {} ';'

# get serial stuff
#cp dist/serial_setup.command processing/
#chmod a+x processing/serial_setup.command
#cp ../../bagel/serial/RXTXcomm.jar processing/lib/
#cp ../../bagel/serial/libSerial.jnilib processing/

# get ds_store file (!)
cp dist/DS_Store processing/.DS_Store

# get package from the dist dir
cp -a dist/Processing.app processing/
chmod +x processing/Processing.app/Contents/MacOS/JavaApplicationStub

# put jar files into the resource dir, leave the rest in lib
RES=processing/Processing.app/Contents/Resources/Java/
mkdir -p $RES
mv processing/lib/*.jar $RES/

# directories used by the app
mkdir processing/lib/build

# grab pde.jar and export from the working dir
cp work/lib/pde.jar $RES/
cp work/lib/core.jar $RES/
#cp -r work/lib/export processing/lib/
#rm -rf processing/lib/export/CVS

# get platform-specific goodies from the dist dir
#cp `which jikes` processing
#gunzip < dist/jikes.gz > processing/jikes
cp dist/jikes processing/
chmod a+x processing/jikes
 
#cp dist/lib/pde_macosx.properties processing/lib/

# convert notes.txt to windows LFs
# the 2> is because the app is a little chatty
#unix2dos processing/notes.txt 2> /dev/null
#unix2dos processing/lib/pde.properties 2> /dev/null
#unix2dos processing/lib/pde.properties_macosx 2> /dev/null

# something like the following might be better:
# find / -name "*.mp3" -exec rm -f {}\;
# and same for cvsignore, ~ files, .DS_Store
find processing -name "*~" -exec rm -f {} ';'
#find processing -name ".DS_Store" -exec rm -f {} ';'  # one is legit
find processing -name "._*" -exec rm -f {} ';'
find processing -name "CVS" -exec rm -rf {} ';'

# zip it all up for release
mv processing "Processing $REVISION"

# if there is a command line tool to make a dmg from this dir.. hmm

# actually, could probably use:
# open processing-uncomp.dmg
# rm -rf /Volumes/Processing/Processing*
# mv "Processing $REVISION" /Volumes/Processing
# umount /Volumes/Processing

echo Done.