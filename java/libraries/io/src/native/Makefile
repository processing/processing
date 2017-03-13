TARGET := libprocessing-io.so
OBJS := impl.o
CC := gcc

# prefix with -m32 to compile for linux32
CFLAGS := -std=gnu99 -fPIC -g -ffast-math
CFLAGS += -I$(shell dirname $(shell realpath $(shell which javac)))/../include
CFLAGS += -I$(shell dirname $(shell realpath $(shell which javac)))/../include/linux
LDFLAGS := -shared

$(TARGET): $(OBJS)
	$(CC) $(CFLAGS) $(LDFLAGS) $^ -o $@

iface.h:
	javah -classpath .. -o iface.h processing.io.NativeInterface

clean:
	rm -f $(TARGET) $(OBJS)

.PHONY: iface.h clean
