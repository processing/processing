#!/bin/sh

#REVISION=`head -c 4 ../../todo.txt`
# 'head' for osx doesn't support -c.. what a pisser
REVISION=0000

./make.sh

echo Creating P5 distribution for revision $REVISION...

# remove any old boogers
rm -rf processing 
rm -rf processing-*

# use 'shared' files as starting point
cp -r ../shared processing
#cp -r ../shared/fonts processing/
#cp -r ../shared/reference processing/
#cp -r ../shared/sketchbook processing/

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

# new style reference
cd processing
unzip -q reference.zip
rm reference.zip
cd ..

# get package from the dist dir
cp -r dist/Proce55ing.app processing/
rm -rf processing/Proce55ing.app/CVS
rm -rf processing/Proce55ing.app/Contents/CVS
rm -rf processing/Proce55ing.app/Contents/MacOS/CVS
rm -rf processing/Proce55ing.app/Contents/Resources/CVS
rm -rf processing/Proce55ing.app/Contents/Resources/Java/CVS

cp -r dist/serial processing/
rm -rf processing/serial/CVS
rm -rf processing/serial/RXTX.pkg/CVS
rm -rf processing/serial/RXTX.pkg/Contents/CVS
rm -rf processing/serial/RXTX.pkg/Contents/Resources/CVS
rm -rf processing/serial/RXTX.pkg/Contents/Resources/English.lproj/CVS

# put jar files into the resource dir, leave the rest in lib
RES=processing/Proce55ing.app/Contents/Resources/Java/
mv processing/lib/*.jar $RES/
#cp comm.jar $RES/
#cp ../shared/dist/lib/*.jar $RES/
#cp ../shared/dist/lib/pde.properties $RES/
#cp ../shared/dist/lib/buttons.gif $RES/

# directories used by the app
mkdir processing/lib/build

# grab pde.jar and export from the working dir
cp work/lib/pde.jar $RES/
cp -r work/lib/export processing/lib/
rm -rf processing/lib/export/CVS

# get platform-specific goodies from the dist dir
#cp dist/Proce55ing processing/
cp dist/lib/pde_macosx.properties processing/lib/
#cp dist/lib/comm.jar processing/lib/

# convert notes.txt to windows LFs
# the 2> is because the app is a little chatty
#unix2dos processing/notes.txt 2> /dev/null
#unix2dos processing/lib/pde.properties 2> /dev/null
#unix2dos processing/lib/pde.properties_macosx 2> /dev/null

# zip it all up for release
#echo Zipping and finishing...
P5=processing-$REVISION
mv processing $P5-macosx
#zip -rq $P5.zip $P5
# nah, keep the new directory around
#rm -rf $P5
# if there is a command line tool to make a dmg from this dir.. hmm

echo Done.