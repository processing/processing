#!/bin/sh

REVISION=`head -c 4 ../../../todo.txt`

./make.sh

echo Creating linux distribution for revision $REVISION...

# remove any old boogers
rm -rf processing
rm -f processing-*.tgz

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
rm -f processing/sketchbook/default/.cvsignore
# will need to add a zillion of these for the reference..

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
#cp dist/run.bat processing/
#cp dist/run95.bat processing/
cp work/Proce55ing processing/
cp dist/lib/pde.properties_linux processing/lib/

# make sure notes.txt is unix LFs
# the 2> is because the app is a little chatty
dos2unix processing/notes.txt 2> /dev/null
dos2unix processing/lib/pde.properties 2> /dev/null
dos2unix processing/lib/pde.properties_windows 2> /dev/null

# zip it all up for release
echo Creating tarball and finishing...
P5=processing-$REVISION
mv processing $P5
tar cfz $P5.tgz $P5
#zip -rq $P5.zip $P5
# nah, keep the new directory around
#rm -rf $P5

echo Done.