CXXFLAGS = -mwindows -mno-cygwin -O2 -Wall
OBJS = launcher.o launcher-rc.o

launcher.exe: $(OBJS)
	$(LINK.cc) $(CXXFLAGS) -o $@ $(OBJS)

$(OBJS): Makefile

launcher-rc.o: launcher.rc
	windres -i $< -o $@

clean:
	$(RM) $(OBJS) launcher.exe
