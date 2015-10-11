TARGET := libprocessing-io.so
OBJS := impl.o
CC := gcc

CFLAGS := -std=c99 -fPIC -g
CFLAGS += -I$(shell dirname $(shell realpath $(shell which javac)))/../include
CFLAGS += -I$(shell dirname $(shell realpath $(shell which javac)))/../include/linux
LDFLAGS := -shared

$(TARGET): $(OBJS)
	$(CC) $(CFLAGS) $(LDFLAGS) $^ -o $@

iface.h:
	javah -classpath .. -o iface.h processing.io.NativeInterface

clean:
	rm -f $(TARGET) $(OBJS)
