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
rm -rf processing
rm -rf Processing*
rm -rf processing-*
rm -rf work

./make.sh

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

#NICE_APP="work/Processing $SHORT_REVISION.app"
#cp -rp work/Processing.app "$NICE_APP"
# for 1.0 release, removing the revision from the app name
NICE_APP="work/Processing.app"

DMG_NAME="processing-$REVISION"
mkdir $DMG_NAME
mv "$NICE_APP" $DMG_NAME/

mkdir $DMG_NAME/.background
cp dist/background.jpg $DMG_NAME/.background/background.jpg 
ln -s /Applications $DMG_NAME/Applications
cp dist/DS_Store $DMG_NAME/.DS_Store

chmod +x mkdmg
./mkdmg $DMG_NAME
rm -rf $DMG_NAME

echo Done.