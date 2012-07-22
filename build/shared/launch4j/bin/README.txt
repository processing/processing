The MinGW binutils for Mac OS were built on OS X 10.6 by Ben Fry.

The 64-bit Linux version was built on Ubuntu 12.04.

The other binaries come from the original downloads on SourceForge.


Ben Fry


. . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . 


Mac OS X build instructions
12 November 2011

# Grab the launch4j download
wget http://downloads.sourceforge.net/launch4j/launch4j-3.0.2-macosx.tgz
tar xvfz launch4j-3.0.2-macosx.tgz
# Rename it so that we remember it's an Intel version
mv launch4j launch4j-3.0.2-macosx-intel

# Find MinGW files here: http://sourceforge.net/projects/mingw/files/
# Specifically this file:
wget http://downloads.sourceforge.net/mingw/binutils-2.19-src.tar.gz

# To build the 32-bit version on Mac OS X
tar xvfz binutils-2.19-src.tar.gz
mv binutils-2.19 binutils-2.19-i386
cd binutils-2.19-i386
CXX='g++ -m32' CC='gcc -m32' CFLAGS=-m32 CXXFLAGS=-m32 LDFLAGS=-m32 ./configure --disable-werror --target=i686-pc-mingw32 --with-gnu-ld
make
# Lots of gibberish, followed by "make[1]: Nothing to be done for `all-target'."
# Files will be at ld/ld-new and binutils/windres
cd ..

# Now build the 64-bit version
tar xvfz binutils-2.19-src.tar.gz
mv binutils-2.19 binutils-2.19-x64
cd binutils-2.19-x64
./configure --disable-werror --target=i686-pc-mingw32
make
cd ..

# Finally, merge the 32- and 64-bit versions together
lipo -create binutils-2.19-i386/ld/ld-new binutils-2.19-x64/ld/ld-new -output launch4j-3.0.2-macosx-intel/bin/ld
lipo -create binutils-2.19-i386/binutils/windres binutils-2.19-x64/binutils/windres -output launch4j-3.0.2-macosx-intel/bin/windres


. . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . 


Linux 64-bit build instructions
21 July 2012

# Find MinGW files here: http://sourceforge.net/projects/mingw/files/

wget http://downloads.sourceforge.net/mingw/binutils-2.19-src.tar.gz
tar xvfz binutils-2.19-src.tar.gz
cd binutils-2.19
./configure --disable-werror --target=i686-pc-mingw32
make
cd ..

# Then copy them out
cp binutils-2.19/ld/ld-new ld-linux64
cp binutils-2.19/binutils/windres windres-linux64

