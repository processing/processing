CXXFLAGS = -mwindows -mno-cygwin -O2 -Wall
POBJS = launcher.o launcher-rc.o
# don't want to include the p5 icons for the exported feller
AOBJS = launcher.o 

# this is a fairly ugly Makefile, if you're a Makefile guru,
# please pipe up and post us a fixed version in the bugs db!
# for instance, i always want both of these to build at the
# same time, rather than doing "make" then "make application.exe"

processing.exe: $(POBJS)
	$(LINK.cc) $(CXXFLAGS) -o $@ $(POBJS)
	cp processing.exe ../work/

application.exe: $(AOBJS)
	$(LINK.cc) $(CXXFLAGS) -DEXPORT -o $@ $(AOBJS)
	cp application.exe ../work/lib/export/
	cp application.exe ../../shared/lib/export/

$(POBJS): Makefile 

launcher-rc.o: launcher.rc
	windres -i $< -o $@

clean:
	$(RM) $(OBJS) processing.exe application.exe
