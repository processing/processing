CXXFLAGS = -mwindows -mno-cygwin -O2 -Wall
OBJS = launcher.o launcher-rc.o

processing.exe: $(OBJS)
	$(LINK.cc) $(CXXFLAGS) -o $@ $(OBJS)
	cp processing.exe ../work/

application.exe: $(OBJS)
	$(LINK.cc) $(CXXFLAGS) -DEXPORT -o $@ $(OBJS)
	cp application.exe ../work/lib/export/
	cp application.exe ../../shared/lib/export/

$(OBJS): Makefile 

launcher-rc.o: launcher.rc
	windres -i $< -o $@

clean:
	$(RM) $(OBJS) processing.exe application.exe
