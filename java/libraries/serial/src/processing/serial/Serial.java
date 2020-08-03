/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  PSerial - class for serial port goodness
  Part of the Processing project - http://processing.org

  Copyright (c) 2004-05 Ben Fry & Casey Reas
  Reworked by Gottfried Haider as part of GSOC 2013

  This library is free software; you can redistribute it and/or
  modify it under the terms of the GNU Lesser General Public
  License as published by the Free Software Foundation; either
  version 2.1 of the License, or (at your option) any later version.

  This library is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
  Lesser General Public License for more details.

  You should have received a copy of the GNU Lesser General
  Public License along with this library; if not, write to the
  Free Software Foundation, Inc., 59 Temple Place, Suite 330,
  Boston, MA  02111-1307  USA
*/

package processing.serial;

import processing.core.*;

import java.lang.reflect.*;
import java.util.Map;

import jssc.*;


/**
 * ( begin auto-generated from Serial.xml )
 * 
 * Class for sending and receiving data using the serial communication protocol.
 * 
 * ( end auto-generated )
 * @webref serial
 * @brief Class for sending and receiving data using the serial communication protocol.
 * @instanceName serial any variable of type Serial
 * @usage Application
 * @see_external LIB_serial/serialEvent
 */
public class Serial implements SerialPortEventListener {
  PApplet parent;
  public SerialPort port;
  Method serialAvailableMethod;
  Method serialEventMethod;

  byte[] buffer = new byte[32768];
  int inBuffer = 0;
  int readOffset = 0;

  int bufferUntilSize = 1;
  byte bufferUntilByte = 0;

  volatile boolean invokeSerialAvailable = false;

  // Things we are currently not exposing:
  // * hardware flow control
  // * state of the RING, RLSD line
  // * sending breaks

  /**
   * @param parent typically use "this"
   */
  public Serial(PApplet parent) {
    this(parent, "COM1", 9600, 'N', 8, 1);
  }


  /**
   * @param baudRate 9600 is the default
   */
  public Serial(PApplet parent, int baudRate) {
    this(parent, "COM1", baudRate, 'N', 8, 1);
  }


  /**
   * @param portName name of the port (COM1 is the default)
   */
  public Serial(PApplet parent, String portName) {
    this(parent, portName, 9600, 'N', 8, 1);
  }


  public Serial(PApplet parent, String portName, int baudRate) {
    this(parent, portName, baudRate, 'N', 8, 1);
  }


  /**
   * @param parity 'N' for none, 'E' for even, 'O' for odd, 'M' for mark, 'S' for space ('N' is the default)
   * @param dataBits 8 is the default
   * @param stopBits 1.0, 1.5, or 2.0 (1.0 is the default)
   */
  public Serial(PApplet parent, String portName, int baudRate, char parity, int dataBits, float stopBits) {
    this.parent = parent;
    parent.registerMethod("dispose", this);
    parent.registerMethod("pre", this);

    // setup parity
    if (parity == 'O') {
      parity = SerialPort.PARITY_ODD;
    } else if (parity == 'E') {
      parity = SerialPort.PARITY_EVEN;
    } else if (parity == 'M') {
      parity = SerialPort.PARITY_MARK;
    } else if (parity == 'S') {
      parity = SerialPort.PARITY_SPACE;
    } else {
      parity = SerialPort.PARITY_NONE;
    }

    // setup stop bits
    int stopBitsIdx = SerialPort.STOPBITS_1;
    if (stopBits == 1.5f) {
      stopBitsIdx = SerialPort.STOPBITS_1_5;
    } else if (stopBits == 2) {
      stopBitsIdx = SerialPort.STOPBITS_2;
    }

    port = new SerialPort(portName);
    try {
      // the native open() call is not using O_NONBLOCK, so this might block for certain operations (see write())
      port.openPort();
      port.setParams(baudRate, dataBits, stopBitsIdx, parity);
      // we could register more events here
      port.addEventListener(this, SerialPort.MASK_RXCHAR);
    } catch (SerialPortException e) {
      // this used to be a RuntimeException before, so stick with it
      throw new RuntimeException("Error opening serial port " + e.getPortName() + ": " + e.getExceptionType());
    }

    serialEventMethod = findCallback("serialEvent");
    serialAvailableMethod = findCallback("serialAvailable");
  }

  private Method findCallback(final String name) {
    try {
      return parent.getClass().getMethod(name, this.getClass());
    } catch (Exception e) {
    }
    // Permit callback(Object) as alternative to callback(Serial).
    try {
      return parent.getClass().getMethod(name, Object.class);
    } catch (Exception e) {
    }
    return null;
  }


  /**
   * Used by PApplet to shut things down.
   */
  public void dispose() {
    stop();
  }


  /**
   * Return true if this port is still active and hasn't run
   * into any trouble.
   */
  public boolean active() {
    return port.isOpened();
  }


  public void pre() {
    if (serialAvailableMethod != null && invokeSerialAvailable) {
      invokeSerialAvailable = false;
      try {
        serialAvailableMethod.invoke(parent, this);
      } catch (Exception e) {
        System.err.println("Error, disabling serialAvailable() for "+port.getPortName());
        System.err.println(e.getLocalizedMessage());
        serialAvailableMethod = null;
      }
    }
  }


  /**
   * @generate Serial_available.xml
   * @webref serial:serial
   * @usage web_application
   */
  public int available() {
    return (inBuffer-readOffset);
  }


  /**
   * @generate Serial_buffer.xml
   * @webref serial:serial
   * @usage web_application
   * @param size number of bytes to buffer
   */
  public void buffer(int size) {
    bufferUntilSize = size;
  }


  /**
   * @generate Serial_bufferUntil.xml
   * @webref serial:serial
   * @usage web_application
   * @param inByte the value to buffer until
   */
  public void bufferUntil(int inByte) {
    bufferUntilSize = 0;
    bufferUntilByte = (byte)inByte;
  }


  /**
   * @generate Serial_clear.xml
   * @webref serial:serial
   * @usage web_application
   */
  public void clear() {
    synchronized (buffer) {
      inBuffer = 0;
      readOffset = 0;
    }
  }


  public boolean getCTS() {
    try {
      return port.isCTS();
    } catch (SerialPortException e) {
      throw new RuntimeException("Error reading the CTS line: " + e.getExceptionType());
    }
  }


  public boolean getDSR() {
    try {
      return port.isDSR();
    } catch (SerialPortException e) {
      throw new RuntimeException("Error reading the DSR line: " + e.getExceptionType());
    }
  }


  public static Map<String, String> getProperties(String portName) {
    return SerialPortList.getPortProperties(portName);
  }


  /**
   * @generate Serial_last.xml
   * <h3>Advanced</h3>
   * Same as read() but returns the very last value received
   * and clears the buffer. Useful when you just want the most
   * recent value sent over the port.
   * @webref serial:serial
   * @usage web_application
   */
  public int last() {
    if (inBuffer == readOffset) {
      return -1;
    }

    synchronized (buffer) {
      int ret = buffer[inBuffer-1] & 0xFF;
      inBuffer = 0;
      readOffset = 0;
      return ret;
    }
  }


  /**
   * @generate Serial_lastChar.xml
   * @webref serial:serial
   * @usage web_application
   */
  public char lastChar() {
    return (char)last();
  }


  /**
   * @generate Serial_list.xml
   * @webref serial
   * @usage web_application
   */
  public static String[] list() {
    // returns list sorted alphabetically, thus cu.* comes before tty.*
    // this was different with RXTX
    return SerialPortList.getPortNames();
  }


  /**
   * @generate Serial_read.xml
   * @webref serial:serial
   * @usage web_application
   */
  public int read() {
    if (inBuffer == readOffset) {
      return -1;
    }

    synchronized (buffer) {
      int ret = buffer[readOffset++] & 0xFF;
      if (inBuffer == readOffset) {
        inBuffer = 0;
        readOffset = 0;
      }
      return ret;
    }
  }


  /**
   * @generate Serial_readBytes.xml
   * @webref serial:serial
   * @usage web_application
   */
  public byte[] readBytes() {
    if (inBuffer == readOffset) {
      return null;
    }

    synchronized (buffer) {
      byte[] ret = new byte[inBuffer-readOffset];
      System.arraycopy(buffer, readOffset, ret, 0, ret.length);
      inBuffer = 0;
      readOffset = 0;
      return ret;
    }
  }
  

  /**
   * <h3>Advanced</h3>
   * Return a byte array of anything that's in the serial buffer
   * up to the specified maximum number of bytes.
   * Not particularly memory/speed efficient, because it creates
   * a byte array on each read, but it's easier to use than
   * readBytes(byte b[]) (see below).
   *
   * @param max the maximum number of bytes to read
   */
  public byte[] readBytes(int max) {
    if (inBuffer == readOffset) {
      return null;
    }

    synchronized (buffer) {
      int length = inBuffer - readOffset;
      if (length > max) length = max;
      byte[] ret = new byte[length];
      System.arraycopy(buffer, readOffset, ret, 0, length);

      readOffset += length;
      if (inBuffer == readOffset) {
        inBuffer = 0;
        readOffset = 0;
      }
      return ret;
    }
  }


  /**
   * <h3>Advanced</h3>
   * Grab whatever is in the serial buffer, and stuff it into a
   * byte buffer passed in by the user. This is more memory/time
   * efficient than readBytes() returning a byte[] array.
   *
   * Returns an int for how many bytes were read. If more bytes
   * are available than can fit into the byte array, only those
   * that will fit are read.
   */
  public int readBytes(byte[] dest) {
    if (inBuffer == readOffset) {
      return 0;
    }

    synchronized (buffer) {
      int toCopy = inBuffer-readOffset;
      if (dest.length < toCopy) {
        toCopy = dest.length;
      }
      System.arraycopy(buffer, readOffset, dest, 0, toCopy);
      readOffset += toCopy;
      if (inBuffer == readOffset) {
        inBuffer = 0;
        readOffset = 0;
      }
      return toCopy;
    }
  }
  

  /**
   * @generate Serial_readBytesUntil.xml
   * @webref serial:serial
   * @usage web_application
   * @param inByte character designated to mark the end of the data
   */
  public byte[] readBytesUntil(int inByte) {
    if (inBuffer == readOffset) {
      return null;
    }

    synchronized (buffer) {
      // look for needle in buffer
      int found = -1;
      for (int i=readOffset; i < inBuffer; i++) {
        if (buffer[i] == (byte)inByte) {
          found = i;
          break;
        }
      }
      if (found == -1) {
        return null;
      }

      int toCopy = found-readOffset+1;
      byte[] dest = new byte[toCopy];
      System.arraycopy(buffer, readOffset, dest, 0, toCopy);
      readOffset += toCopy;
      if (inBuffer == readOffset) {
        inBuffer = 0;
        readOffset = 0;
      }
      return dest;
    }
  }


  /**
   * <h3>Advanced</h3>
   * If dest[] is not big enough, then -1 is returned,
   *   and an error message is printed on the console.
   * If nothing is in the buffer, zero is returned.
   * If 'interesting' byte is not in the buffer, then 0 is returned.
   * @param dest passed in byte array to be altered
   */
  public int readBytesUntil(int inByte, byte[] dest) {
    if (inBuffer == readOffset) {
      return 0;
    }

    synchronized (buffer) {
      // look for needle in buffer
      int found = -1;
      for (int i=readOffset; i < inBuffer; i++) {
        if (buffer[i] == (byte)inByte) {
          found = i;
          break;
        }
      }
      if (found == -1) {
        return 0;
      }

      // check if bytes to copy fit in dest
      int toCopy = found-readOffset+1;
      if (dest.length < toCopy) {
        System.err.println( "The buffer passed to readBytesUntil() is to small " +
                  "to contain " + toCopy + " bytes up to and including " +
                  "char " + (byte)inByte);
        return -1;
      }
      System.arraycopy(buffer, readOffset, dest, 0, toCopy);
      readOffset += toCopy;
      if (inBuffer == readOffset) {
        inBuffer = 0;
        readOffset = 0;
      }
      return toCopy;
    }
  }


  /**
   * @generate Serial_readChar.xml
   * @webref serial:serial
   * @usage web_application
   */
  public char readChar() {
    return (char) read();
  }


  /**
   * @generate Serial_readString.xml
   * @webref serial:serial
   * @usage web_application
   */
  public String readString() {
    if (inBuffer == readOffset) {
      return null;
    }
    return new String(readBytes());
  }


  /**
   * @generate Serial_readStringUntil.xml
   *<h3>Advanced</h3>
   * If you want to move Unicode data, you can first convert the
   * String to a byte stream in the representation of your choice
   * (i.e. UTF8 or two-byte Unicode data), and send it as a byte array.
   *
   * @webref serial:serial
   * @usage web_application
   * @param inByte character designated to mark the end of the data
   */
  public String readStringUntil(int inByte) {
    byte temp[] = readBytesUntil(inByte);
    if (temp == null) {
      return null;
    } else {
      return new String(temp);
    }
  }


  /**
   * @generate serialEvent.xml
   * @webref serial:events
   * @usage web_application
   * @param event the port where new data is available
   */
  public void serialEvent(SerialPortEvent event) {
    if (event.getEventType() == SerialPortEvent.RXCHAR) {
      int toRead;
      try {
        while (0 < (toRead = port.getInputBufferBytesCount())) {
          // this method can be called from the context of another thread
          synchronized (buffer) {
            // read one byte at a time if the sketch is using serialEvent
            if (serialEventMethod != null) {
              toRead = 1;
            }
            // enlarge buffer if necessary
            if (buffer.length < inBuffer+toRead) {
              byte temp[] = new byte[buffer.length<<1];
              System.arraycopy(buffer, 0, temp, 0, inBuffer);
              buffer = temp;
            }
            // read an array of bytes and copy it into our buffer
            byte[] read = port.readBytes(toRead);
            System.arraycopy(read, 0, buffer, inBuffer, read.length);
            inBuffer += read.length;
          }
          if (serialEventMethod != null) {
            if ((0 < bufferUntilSize && bufferUntilSize <= inBuffer-readOffset) ||
              (0 == bufferUntilSize && bufferUntilByte == buffer[inBuffer-1])) {
              try {
                // serialEvent() is invoked in the context of the current (serial) thread
                // which means that serialization and atomic variables need to be used to
                // guarantee reliable operation (and better not draw() etc..)
                // serialAvailable() does not provide any real benefits over using
                // available() and read() inside draw - but this function has no
                // thread-safety issues since it's being invoked during pre in the context
                // of the Processing applet
                serialEventMethod.invoke(parent, this);
              } catch (Exception e) {
                System.err.println("Error, disabling serialEvent() for "+port.getPortName());
                System.err.println(e.getLocalizedMessage());
                serialEventMethod = null;
              }
            }
          }
          invokeSerialAvailable = true;
        }
      } catch (SerialPortException e) {
        throw new RuntimeException("Error reading from serial port " + e.getPortName() + ": " + e.getExceptionType());
      }
    }
  }


  /**
   * Set the DTR line
   */
  public void setDTR(boolean state) {
    // there is no way to influence the behavior of the DTR line when opening the serial port
    // this means that at least on Linux and OS X, Arduino devices are always reset
    try {
      port.setDTR(state);
    } catch (SerialPortException e) {
      throw new RuntimeException("Error setting the DTR line: " + e.getExceptionType());
    }
  }


  /**
   * Set the RTS line
   */
  public void setRTS(boolean state) {
    try {
      port.setRTS(state);
    } catch (SerialPortException e) {
      throw new RuntimeException("Error setting the RTS line: " + e.getExceptionType());
    }
  }


  /**
   * @generate Serial_stop.xml
   * @webref serial:serial
   * @usage web_application
   */
  public void stop() {
    try {
      port.closePort();
    } catch (SerialPortException e) {
      // ignored
    }
    inBuffer = 0;
    readOffset = 0;
  }


 /**
  * @param src data to write
  */
  public void write(byte[] src) {
    try {
      // this might block if the serial device is not yet ready (esp. tty devices under OS X)
      port.writeBytes(src);
      // we used to call flush() here
    } catch (SerialPortException e) {
      throw new RuntimeException("Error writing to serial port " + e.getPortName() + ": " + e.getExceptionType());
    }
  }


  /**
   * <h3>Advanced</h3>
   * This will handle both ints, bytes and chars transparently.
   * @param src data to write
   */
  public void write(int src) {
    try {
      port.writeInt(src);
    } catch (SerialPortException e) {
      throw new RuntimeException("Error writing to serial port " + e.getPortName() + ": " + e.getExceptionType());
    }
  }


  /**
   * @generate Serial_write.xml
   * <h3>Advanced</h3>
   * Write a String to the output. Note that this doesn't account
   * for Unicode (two bytes per char), nor will it send UTF8
   * characters.. It assumes that you mean to send a byte buffer
   * (most often the case for networking and serial i/o) and
   * will only use the bottom 8 bits of each char in the string.
   * (Meaning that internally it uses String.getBytes)
   *
   * If you want to move Unicode data, you can first convert the
   * String to a byte stream in the representation of your choice
   * (i.e. UTF8 or two-byte Unicode data), and send it as a byte array.
   *
   * @webref serial:serial
   * @usage web_application
   * @param src data to write
   */
  public void write(String src) {
    try {
      port.writeString(src);
    } catch (SerialPortException e) {
      throw new RuntimeException("Error writing to serial port " + e.getPortName() + ": " + e.getExceptionType());
    }
  }
}
