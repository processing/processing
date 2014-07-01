all: 	
	g++ -I/System/Library/Frameworks/JavaVM.framework/Versions/A/Headers -I./include -std=c++11 -g -c processing_sound_MethClaInterface.cpp;		
	g++ -dynamiclib -lmethcla -L../../library/macosx/ -o libMethClaInterface.jnilib *.o;

clean:
	rm *.o
	rm *.jnilib

install:
	cp libMethClaInterface.jnilib ../../library/macosx
