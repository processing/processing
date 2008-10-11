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

echo Creating P5 distribution for revision $REVISION...

# remove any old boogers
mv processing ~/.Trash/
mv Processing* ~/.Trash/
mv processing-* ~/.Trash/
mv work ~/.Trash/

./make.sh

#mkdir processing
#cp -r ../shared/lib processing/
#cp -r ../shared/libraries processing/
#cp -r ../shared/tools processing/
#cp ../../app/lib/antlr.jar processing/lib/
#cp ../../app/lib/ecj.jar processing/lib/
#cp ../../app/lib/jna.jar processing/lib/
#cp ../shared/revisions.txt processing/

#echo Extracting examples...
#unzip -q -d processing/ ../shared/examples.zip

#echo Extracting reference...
#unzip -q -d processing/ ../shared/reference.zip

# add the libraries folder with source
#cp -r ../../net processing/libraries/
#cp -r ../../opengl processing/libraries/
#cp -r ../../serial processing/libraries/
#cp -r ../../video processing/libraries/
#cp -r ../../pdf processing/libraries/
#cp -r ../../dxf processing/libraries/

# get ds_store file (!)
#cp dist/DS_Store processing/.DS_Store

# get package from the dist dir
#cp -pR dist/Processing.app processing/
#chmod +x processing/Processing.app/Contents/MacOS/JavaApplicationStub

# put jar files into the resource dir, leave the rest in lib
#RES=processing/Processing.app/Contents/Resources/Java
#mkdir -p $RES
#mv processing/lib/*.jar $RES/

# grab pde.jar and export from the working dir
#cp work/lib/pde.jar $RES/
#cp work/lib/core.jar processing/lib/

# get platform-specific goodies from the dist dir
#chmod a+x processing/Processing.app/Contents/MacOS/JavaApplicationStub

# remove boogers
find work -name "*~" -exec rm -f {} ';'
# need to leave ds store stuff cuz one of those is important
#find processing -name ".DS_Store" -exec rm -f {} ';'
find work -name "._*" -exec rm -f {} ';'
find work -name "Thumbs.db" -exec rm -f {} ';'

# clean out the cvs entries
find work -name "CVS" -exec rm -rf {} ';' 2> /dev/null
find work -name ".cvsignore" -exec rm -rf {} ';'
find work -name ".svn" -exec rm -rf {} 2> /dev/null ';'

NICE_APP="work/Processing $SHORT_REVISION.app"
mv work/Processing.app "$NICE_APP"

#NICE_FOLDER="Processing $SHORT_REVISION"
#mv processing "$NICE_FOLDER"
mkdir processing-$REVISION
#mv "$NICE_FOLDER" processing-$REVISION/
mv "$NICE_APP" processing-$REVISION/

chmod +x mkdmg
./mkdmg processing-$REVISION
mv processing-$REVISION ~/.Trash/

echo Done.