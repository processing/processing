#!/bin/sh

echo Creating P5 distribution...

# remove any old boogers
rm -rf processing
rm -f processing.zip

# use 'shared' files as starting point
cp -r ../shared processing
# something like the following might be better:
# find / -name "*.mp3" -exec rm -f {}\;
# and same for cvsignore
rm -rf processing/lib/CVS
rm -rf processing/fonts/CVS
rm -rf processing/reference/CVS
rm -rf processing/reference/images/CVS
rm -rf processing/sketchbook/CVS
rm -rf processing/sketchbook/default/CVS
rm -f processing/sketchbook/default/.cvsignore
# will need to add a zillion of these for the reference..

# add java (jre) files
unzip -q -d processing jre.zip

# directories used by the app
mkdir processing/lib/build

# grab pde.jar and export from the working dir
cp work/lib/pde.jar processing/lib/
cp -r work/lib/export processing/lib/
rm -rf processing/lib/export/CVS

# get platform-specific goodies from the dist dir
cp dist/run.bat 
cp dist/run95.bat
cp dist/lib/pde.properties_windows processing/lib/

# convert notes.txt to windows LFs

# zip it all up for release
echo Zipping and finishing...
zip -rq processing.zip processing
#rm -rf processing

echo Done.