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

rm -rf processing/sketchbook/examples/CVS
rm -rf processing/sketchbook/examples/calculate/CVS
rm -rf processing/sketchbook/examples/calculate/calculate03/CVS
rm -rf processing/sketchbook/examples/calculate/calculate04/CVS
rm -rf processing/sketchbook/examples/calculate/calculate05/CVS
rm -rf processing/sketchbook/examples/calculate/calculate06/CVS
rm -rf processing/sketchbook/examples/calculate/calculate07/CVS
rm -rf processing/sketchbook/examples/calculate/calculate08/CVS
rm -rf processing/sketchbook/examples/color/CVS
rm -rf processing/sketchbook/examples/color/color01/CVS
rm -rf processing/sketchbook/examples/control/CVS
rm -rf processing/sketchbook/examples/control/control00/CVS
rm -rf processing/sketchbook/examples/control/control01/CVS
rm -rf processing/sketchbook/examples/control/control02/CVS
rm -rf processing/sketchbook/examples/data/CVS
rm -rf processing/sketchbook/examples/data/data00/CVS
rm -rf processing/sketchbook/examples/form/CVS
rm -rf processing/sketchbook/examples/form/form00/CVS
rm -rf processing/sketchbook/examples/form/form04/CVS
rm -rf processing/sketchbook/examples/form/form06/CVS
rm -rf processing/sketchbook/examples/gui/CVS
rm -rf processing/sketchbook/examples/gui/gui00/CVS
rm -rf processing/sketchbook/examples/image/CVS
rm -rf processing/sketchbook/examples/image/image01/CVS
rm -rf processing/sketchbook/examples/image/image01/data/CVS
rm -rf processing/sketchbook/examples/image/image02/CVS
rm -rf processing/sketchbook/examples/image/image02/data/CVS
rm -rf processing/sketchbook/examples/image/image03/CVS
rm -rf processing/sketchbook/examples/image/image03/data/CVS
rm -rf processing/sketchbook/examples/image/image08/CVS
rm -rf processing/sketchbook/examples/image/image08/data/CVS
rm -rf processing/sketchbook/examples/image/image09/CVS
rm -rf processing/sketchbook/examples/image/image09/data/CVS
rm -rf processing/sketchbook/examples/input/CVS
rm -rf processing/sketchbook/examples/input/input02/CVS
rm -rf processing/sketchbook/examples/input/input03/CVS
rm -rf processing/sketchbook/examples/input/input04/CVS
rm -rf processing/sketchbook/examples/input/input05/CVS
rm -rf processing/sketchbook/examples/input/input06/CVS
rm -rf processing/sketchbook/examples/input/input07/CVS
rm -rf processing/sketchbook/examples/input/input08/CVS
rm -rf processing/sketchbook/examples/motion/CVS
rm -rf processing/sketchbook/examples/motion/motion00/CVS
rm -rf processing/sketchbook/examples/motion/motion01/CVS
rm -rf processing/sketchbook/examples/motion/motion02/CVS
rm -rf processing/sketchbook/examples/motion/motion05/CVS
rm -rf processing/sketchbook/examples/motion/motion06/CVS
rm -rf processing/sketchbook/examples/simulate/CVS
rm -rf processing/sketchbook/examples/simulate/simulate00/CVS
rm -rf processing/sketchbook/examples/simulate/simulate01/CVS
rm -rf processing/sketchbook/examples/structure/CVS
rm -rf processing/sketchbook/examples/structure/structure00/CVS
rm -rf processing/sketchbook/examples/structure/structure01/CVS
rm -rf processing/sketchbook/examples/structure/structure04/CVS
rm -rf processing/sketchbook/examples/structure/structure05/CVS
rm -rf processing/sketchbook/examples/transform/CVS
rm -rf processing/sketchbook/examples/transform/transform05/CVS

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
cp work/Proce55ing processing/
cp dist/lib/pde_linux.properties processing/lib/

# make sure notes.txt is unix LFs
# the 2> is because the app is a little chatty
dos2unix processing/readme.txt 2> /dev/null
dos2unix processing/revisions.txt 2> /dev/null
dos2unix processing/lib/pde.properties 2> /dev/null
dos2unix processing/lib/pde_linux.properties 2> /dev/null

# zip it all up for release
echo Creating tarball and finishing...
P5=processing-$REVISION
mv processing $P5
tar cfz $P5.tgz $P5
# nah, keep the new directory around
#rm -rf $P5

echo Done.
