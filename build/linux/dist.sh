#!/bin/sh

REVISION=`head -c 4 ../../todo.txt`

./make.sh

echo Creating linux distribution for revision $REVISION...

# remove any old boogers
rm -rf processing
rm -rf processing-*
#rm -f processing-*.tgz

# use 'shared' files as starting point
cp -r ../shared processing
# something like the following might be better:
# find / -name "*.mp3" -exec rm -f {}\;
# and same for cvsignore
rm -rf processing/CVS
rm -rf processing/lib/CVS
rm -rf processing/fonts/CVS
rm -rf processing/reference/CVS
rm -rf processing/reference/images/CVS
rm -rf processing/sketchbook/CVS
rm -rf processing/sketchbook/default/CVS
rm -f  processing/sketchbook/default/.cvsignore

# new style examples thing ala reas
cd processing/sketchbook
unzip -q examples.zip
rm examples.zip
cd ../..

cd processing
unzip -q reference.zip
rm reference.zip
cd ..

# add java (jre) files
#tar --extract --verbose --file=jre.tgz --ungzip --directory=processing
tar --extract --file=jre.tgz --ungzip --directory=processing

# directories used by the app
mkdir processing/lib/build

# grab pde.jar and export from the working dir
cp work/lib/pde.jar processing/lib/
cp -r work/lib/export processing/lib/
rm -rf processing/lib/export/CVS

# get platform-specific goodies from the dist dir
#cp work/Proce55ing processing/
#cp work/processing processing/
install -m 755 stub.sh processing/processing
cp dist/lib/pde_linux.properties processing/lib/

# make sure notes.txt is unix LFs
# the 2> is because the app is a little chatty
dos2unix processing/readme.txt 2> /dev/null
dos2unix processing/revisions.txt 2> /dev/null
dos2unix processing/lib/pde.properties 2> /dev/null
dos2unix processing/lib/pde_linux.properties 2> /dev/null

# get the serial stuff
echo Copying serial support from bagel dir
cp ../../bagel/serial/RXTXcomm.jar processing/lib/
mkdir processing/lib/i386
cp ../../bagel/serial/librxtxSerial.so processing/lib/i386/libSerial.so

# get jikes and depedencies
gunzip < dist/jikes.gz > processing/jikes
chmod +x processing/jikes

# zip it all up for release
echo Creating tarball and finishing...
P5=processing-$REVISION
mv processing $P5
tar cfz $P5.tgz $P5
# nah, keep the new directory around
#rm -rf $P5

echo Done.
