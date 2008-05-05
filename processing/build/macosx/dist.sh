#!/bin/sh


#SHORT_REVISION=`head -1 ../../todo.txt | cut -c 2-4`
REVISION=`head -1 ../../todo.txt | cut -c 1-4`
# as of 100, just use 0100 everywhere to avoid confusion
SHORT_REVISION=$REVISION

VERSIONED=`cat ../../app/src/processing/app/Base.java | grep $REVISION`
if [ -z "$VERSIONED" ]
then
  echo Fix the revision number in Base.java
  exit
fi

./make.sh

echo Creating P5 distribution for revision $REVISION...

# remove any old boogers
rm -rf processing 
rm -rf Processing*
rm -rf processing-*

# use 'shared' files as starting point
cp -r ../shared processing
# tools.jar not needed on osx
#rm -f processing/lib/tools.jar

# add the libraries folder with source
cp -r ../../net processing/libraries/
cp -r ../../opengl processing/libraries/
cp -r ../../serial processing/libraries/
cp -r ../../video processing/libraries/
cp -r ../../pdf processing/libraries/
cp -r ../../dxf processing/libraries/
cp -r ../../xml processing/libraries/
cp -r ../../candy processing/libraries/

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

# get ds_store file (!)
cp dist/DS_Store processing/.DS_Store

# get package from the dist dir
cp -pR dist/Processing.app processing/
chmod +x processing/Processing.app/Contents/MacOS/JavaApplicationStub

# put jar files into the resource dir, leave the rest in lib
RES=processing/Processing.app/Contents/Resources/Java
mkdir -p $RES
mv processing/lib/*.jar $RES/

# directories used by the app
#mkdir processing/lib/build

# grab pde.jar and export from the working dir
cp work/lib/pde.jar $RES/
cp work/lib/core.jar processing/lib/

# get platform-specific goodies from the dist dir
#cp `which jikes` processing
#gunzip < dist/jikes.gz > processing/jikes
cp dist/jikes processing/
chmod a+x processing/jikes

chmod a+x processing/Processing.app/Contents/MacOS/JavaApplicationStub

#cd ../..
#javadoc -public -d doc app/*.java app/preproc/*.java app/syntax/*.java core/*.java opengl/*.java net/*.java video/*.java serial/*.java
#cd build/macosx

# remove boogers
find processing -name "*~" -exec rm -f {} ';'
# need to leave ds store stuff cuz one of those is important
#find processing -name ".DS_Store" -exec rm -f {} ';'
find processing -name "._*" -exec rm -f {} ';'
find processing -name "Thumbs.db" -exec rm -f {} ';'

# clean out the cvs entries
find processing -name "CVS" -exec rm -rf {} ';' 2> /dev/null
find processing -name ".cvsignore" -exec rm -rf {} ';'
find processing -name ".svn" -exec rm -rf {} ';'

mv processing/Processing.app "processing/Processing $SHORT_REVISION.app"
#mv processing processing-$REVISION
#mv processing "Processing $SHORT_REVISION"

# don't have deluxe on my laptop right now
#stuff -f sitx processing-$REVISION

#NICE_FOLDER="Processing $SHORT_REVISION"
#mv processing "$NICE_FOLDER"
#chmod +x mkdmg2
#./mkdmg2 "$NICE_FOLDER"

NICE_FOLDER="Processing $SHORT_REVISION"
mv processing "$NICE_FOLDER"
mkdir processing-$REVISION
mv "$NICE_FOLDER" processing-$REVISION/

chmod +x mkdmg
./mkdmg processing-$REVISION
rm -rf processing-$REVISION

# actually, could probably use:
# open processing-uncomp.dmg
# rm -rf /Volumes/Processing/Processing*
# mv "Processing $REVISION" /Volumes/Processing
# umount /Volumes/Processing

echo Done.