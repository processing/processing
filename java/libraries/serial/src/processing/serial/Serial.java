/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  PSerial - class for serial port goodness
  Part of the Processing project - http://processing.org

  Copyright (c) 2004-05 Ben Fry & Casey Reas

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

import gnu.io.*;

import java.io.*;
import java.util.*;
import java.lang.reflect.*;

/**
 * @generate Serial.xml
 * @webref net
 * @usage application
 */
public class Serial implements SerialPortEventListener {

  PApplet parent;
  Method serialEventMethod;

  // properties can be passed in for default values
  // otherwise defaults to 9600 N81

  // these could be made static, which might be a solution
  // for the classloading problem.. because if code ran again,
  // the static class would have an object that could be closed

  public SerialPort port;

  public int rate;
  public int parity;
  public int databits;
  public int stopbits;


  // read buffer and streams

  public InputStream input;
  public OutputStream output;

  byte buffer[] = new byte[32768];
  int bufferIndex;
  int bufferLast;

  //boolean bufferUntil = false;
  int bufferSize = 1;  // how big before reset or event firing
  boolean bufferUntil;
  int bufferUntilByte;


  // defaults

  static String dname = "COM1";
  static int drate = 9600;
  static char dparity = 'N';
  static int ddatabits = 8;
  static float dstopbits = 1;


  public void setProperties(Properties props) {
    dname =
      props.getProperty("serial.port", dname);
    drate =
      Integer.parseInt(props.getProperty("serial.rate", "9600"));
    dparity =
      props.getProperty("serial.parity", "N").charAt(0);
    ddatabits =
      Integer.parseInt(props.getProperty("serial.databits", "8"));
    dstopbits =
      new Float(props.getProperty("serial.stopbits", "1")).floatValue();
  }

/**
 * @param parent typically use "this"
 */
  public Serial(PApplet parent) {
    this(parent, dname, drate, dparity, ddatabits, dstopbits);
  }
  
/**
 * @param irate 9600 is the default
 */
  public Serial(PApplet parent, int irate) {
    this(parent, dname, irate, dparity, ddatabits, dstopbits);
  }

/**
 * @param iname name of the port (COM1 is the default)
 */
  public Serial(PApplet parent, String iname, int irate) {
    this(parent, iname, irate, dparity, ddatabits, dstopbits);
  }

  public Serial(PApplet parent, String iname) {
    this(parent, iname, drate, dparity, ddatabits, dstopbits);
  }

/**
 * @param iparity 'N' for none, 'E' for even, 'O' for odd ('N' is the default)
 * @param idatabits 8 is the default
 * @param istopbits 1.0, 1.5, or 2.0 (1.0 is the default)
 */
  public Serial(PApplet parent, String iname, int irate,
                 char iparity, int idatabits, float istopbits) {
    //if (port != null) port.close();
    this.parent = parent;
    //parent.attach(this);

    this.rate = irate;

    parity = SerialPort.PARITY_NONE;
    if (iparity == 'E') parity = SerialPort.PARITY_EVEN;
    if (iparity == 'O') parity = SerialPort.PARITY_ODD;

    this.databits = idatabits;

    stopbits = SerialPort.STOPBITS_1;
    if (istopbits == 1.5f) stopbits = SerialPort.STOPBITS_1_5;
    if (istopbits == 2) stopbits = SerialPort.STOPBITS_2;

    try {
      Enumeration<?> portList = CommPortIdentifier.getPortIdentifiers();
      while (portList.hasMoreElements()) {
        CommPortIdentifier portId =
          (CommPortIdentifier) portList.nextElement();

        if (portId.getPortType() == CommPortIdentifier.PORT_SERIAL) {
          //System.out.println("found " + portId.getName());
          if (portId.getName().equals(iname)) {
            port = (SerialPort)portId.open("serial madness", 2000);
            input = port.getInputStream();
            output = port.getOutputStream();
            port.setSerialPortParams(rate, databits, stopbits, parity);
            port.addEventListener(this);
            port.notifyOnDataAvailable(true);
            //System.out.println("opening, ready to roll");
          }
        }
      }

    } catch (Exception e) {
      errorMessage("<init>", e);
      //exception = e;
      //e.printStackTrace();
      port = null;
      input = null;
      output = null;
    }

    parent.registerDispose(this);

    // reflection to check whether host applet has a call for
    // public void serialEvent(processing.serial.Serial)
    // which would be called each time an event comes in
    try {
      serialEventMethod =
        parent.getClass().getMethod("serialEvent",
                                    new Class[] { Serial.class });
    } catch (Exception e) {
      // no such method, or an error.. which is fine, just ignore
    }
  }


  /**
   * @generate Serial_stop.xml
   * @webref serial:serial
   * @usage web_application
   */
  public void stop() {
    dispose();
  }


  /**
   * Used by PApplet to shut things down.
   */
  public void dispose() {
    try {
      // do io streams need to be closed first?
      if (input != null) input.close();
      if (output != null) output.close();

    } catch (Exception e) {
      e.printStackTrace();
    }
    input = null;
    output = null;

    try {
      if (port != null) port.close();  // close the port

    } catch (Exception e) {
      e.printStackTrace();
    }
    port = null;
  }


  /**
  * Set the DTR line. Addition from Tom Hulbert.
  */
  public void setDTR(boolean state) {
        port.setDTR(state);
  }

/**
 * @generate serialEvent.xml
 * @webref serial:events
 * @usage web_application
 * @param serialEvent the port where new data is available
 */
  synchronized public void serialEvent(SerialPortEvent serialEvent) {
    if (serialEvent.getEventType() == SerialPortEvent.DATA_AVAILABLE) {
      try {
        while (input.available() > 0) {
          synchronized (buffer) {
            if (bufferLast == buffer.length) {
              byte temp[] = new byte[bufferLast << 1];
              System.arraycopy(buffer, 0, temp, 0, bufferLast);
              buffer = temp;
            }
            buffer[bufferLast++] = (byte) input.read();
            if (serialEventMethod != null) {
              if ((bufferUntil &&
                   (buffer[bufferLast-1] == bufferUntilByte)) ||
                  (!bufferUntil &&
                   ((bufferLast - bufferIndex) >= bufferSize))) {
                try {
                  serialEventMethod.invoke(parent, new Object[] { this });
                } catch (Exception e) {
                  String msg = "error, disabling serialEvent() for " + port;
                  System.err.println(msg);
                  e.printStackTrace();
                  serialEventMethod = null;
                }
              }
            }
          }
        }

      } catch (IOException e) {
        errorMessage("serialEvent", e);
      }
    }
  }


  /**
   * @generate Serial_buffer.xml
   * @webref serial:serial
   * @usage web_application
   * @param count number of bytes to buffer
   */
  public void buffer(int count) {
    bufferUntil = false;
    bufferSize = count;
  }


  /**
   * @generate Serial_bufferUntil.xml
   * @webref serial:serial
   * @usage web_application
   * @param what the value to buffer until
   */
  public void bufferUntil(int what) {
    bufferUntil = true;
    bufferUntilByte = what;
  }


  /**
   * @generate Serial_available.xml
   * @webref serial:serial
   * @usage web_application
   */
  public int available() {
    return (bufferLast - bufferIndex);
  }


  /**
   * @generate Serial_clear.xml
   * @webref serial:serial
   * @usage web_application
   */
  public void clear() {
    bufferLast = 0;
    bufferIndex = 0;
  }


  /**
   * @generate Serial_read.xml
   * @webref serial:serial
   * @usage web_application
   */
  public int read() {
    if (bufferIndex == bufferLast) return -1;

    synchronized (buffer) {
      int outgoing = buffer[bufferIndex++] & 0xff;
      if (bufferIndex == bufferLast) {  // rewind
        bufferIndex = 0;
        bufferLast = 0;
      }
      return outgoing;
    }
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
    if (bufferIndex == bufferLast) return -1;
    synchronized (buffer) {
      int outgoing = buffer[bufferLast-1];
      bufferIndex = 0;
      bufferLast = 0;
      return outgoing;
    }
  }


  /**
   * @generate Serial_readChar.xml
   * @webref serial:serial
   * @usage web_application
   */
  public char readChar() {
    if (bufferIndex == bufferLast) return (char)(-1);
    return (char) read();
  }


  /**
   * @generate Serial_lastChar.xml
   * @webref serial:serial
   * @usage web_application
   */
  public char lastChar() {
    if (bufferIndex == bufferLast) return (char)(-1);
    return (char) last();
  }


  /**
   * @generate Serial_readBytes.xml 
   * @webref serial:serial
   * @usage web_application
   */
  public byte[] readBytes() {
    if (bufferIndex == bufferLast) return null;

    synchronized (buffer) {
      int length = bufferLast - bufferIndex;
      byte outgoing[] = new byte[length];
      System.arraycopy(buffer, bufferIndex, outgoing, 0, length);

      bufferIndex = 0;  // rewind
      bufferLast = 0;
      return outgoing;
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
  public int readBytes(byte outgoing[]) {
    if (bufferIndex == bufferLast) return 0;

    synchronized (buffer) {
      int length = bufferLast - bufferIndex;
      if (length > outgoing.length) length = outgoing.length;
      System.arraycopy(buffer, bufferIndex, outgoing, 0, length);

      bufferIndex += length;
      if (bufferIndex == bufferLast) {
        bufferIndex = 0;  // rewind
        bufferLast = 0;
      }
      return length;
    }
  }


  /**
   * @generate Serial_readBytesUntil.xml
   * @webref serial:serial
   * @usage web_application
   * @param interesting character designated to mark the end of the data
   */
  public byte[] readBytesUntil(int interesting) {
    if (bufferIndex == bufferLast) return null;
    byte what = (byte)interesting;

    synchronized (buffer) {
      int found = -1;
      for (int k = bufferIndex; k < bufferLast; k++) {
        if (buffer[k] == what) {
          found = k;
          break;
        }
      }
      if (found == -1) return null;

      int length = found - bufferIndex + 1;
      byte outgoing[] = new byte[length];
      System.arraycopy(buffer, bufferIndex, outgoing, 0, length);

      bufferIndex += length;
      if (bufferIndex == bufferLast) {
        bufferIndex = 0;  // rewind
        bufferLast = 0;
      }
      return outgoing;
    }
  }


  /**
   * <h3>Advanced</h3>
   * If outgoing[] is not big enough, then -1 is returned,
   *   and an error message is printed on the console.
   * If nothing is in the buffer, zero is returned.
   * If 'interesting' byte is not in the buffer, then 0 is returned.
   * @param outgoing passed in byte array to be altered
   */
  public int readBytesUntil(int interesting, byte outgoing[]) {
    if (bufferIndex == bufferLast) return 0;
    byte what = (byte)interesting;

    synchronized (buffer) {
      int found = -1;
      for (int k = bufferIndex; k < bufferLast; k++) {
        if (buffer[k] == what) {
          found = k;
          break;
        }
      }
      if (found == -1) return 0;

      int length = found - bufferIndex + 1;
      if (length > outgoing.length) {
        System.err.println("readBytesUntil() byte buffer is" +
                           " too small for the " + length +
                           " bytes up to and including char " + interesting);
        return -1;
      }
      //byte outgoing[] = new byte[length];
      System.arraycopy(buffer, bufferIndex, outgoing, 0, length);

      bufferIndex += length;
      if (bufferIndex == bufferLast) {
        bufferIndex = 0;  // rewind
        bufferLast = 0;
      }
      return length;
    }
  }


  /**
   * @generate Serial_readString.xml
   * @webref serial:serial
   * @usage web_application
   */
  public String readString() {
    if (bufferIndex == bufferLast) return null;
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
   * @param interesting character designated to mark the end of the data
   */
  public String readStringUntil(int interesting) {
    byte b[] = readBytesUntil(interesting);
    if (b == null) return null;
    return new String(b);
  }


  /**
   * <h3>Advanced</h3>
   * This will handle both ints, bytes and chars transparently.
   * @param what data to write
   */
  public void write(int what) {  // will also cover char
    try {
      output.write(what & 0xff);  // for good measure do the &
      output.flush();   // hmm, not sure if a good idea

    } catch (Exception e) { // null pointer or serial port dead
      errorMessage("write", e);
    }
  }

 /**
  * @param bytes[] data to write
  */
  public void write(byte bytes[]) {
    try {
      output.write(bytes);
      output.flush();   // hmm, not sure if a good idea

    } catch (Exception e) { // null pointer or serial port dead
      //errorMessage("write", e);
      e.printStackTrace();
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
   * @param what data to write
   */
  public void write(String what) {
    write(what.getBytes());
  }


  /**
   * @generate Serial_list.xml
   * <h3>Advanced</h3>
   * If this just hangs and never completes on Windows,
   * it may be because the DLL doesn't have its exec bit set.
   * Why the hell that'd be the case, who knows.
   * 
   * @webref serial
   * @usage web_application
   */
  static public String[] list() {
    Vector<String> list = new Vector<String>();
    try {
      //System.err.println("trying");
      Enumeration<?> portList = CommPortIdentifier.getPortIdentifiers();
      //System.err.println("got port list");
      while (portList.hasMoreElements()) {
        CommPortIdentifier portId =
          (CommPortIdentifier) portList.nextElement();
        //System.out.println(portId);

        if (portId.getPortType() == CommPortIdentifier.PORT_SERIAL) {
          String name = portId.getName();
          list.addElement(name);
        }
      }

    } catch (UnsatisfiedLinkError e) {
      //System.err.println("1");
      errorMessage("ports", e);

    } catch (Exception e) {
      //System.err.println("2");
      errorMessage("ports", e);
    }
    //System.err.println("move out");
    String outgoing[] = new String[list.size()];
    list.copyInto(outgoing);
    return outgoing;
  }


  /**
   * General error reporting, all corraled here just in case
   * I think of something slightly more intelligent to do.
   */
  static public void errorMessage(String where, Throwable e) {
    e.printStackTrace();
    throw new RuntimeException("Error inside Serial." + where + "()");
  }
}


  /*
  class SerialMenuListener implements ItemListener {
    //public SerialMenuListener() { }

    public void itemStateChanged(ItemEvent e) {
      int count = serialMenu.getItemCount();
      for (int i = 0; i < count; i++) {
        ((CheckboxMenuItem)serialMenu.getItem(i)).setState(false);
      }
      CheckboxMenuItem item = (CheckboxMenuItem)e.getSource();
      item.setState(true);
      String name = item.getLabel();
      //System.out.println(item.getLabel());
      PdeBase.properties.put("serial.port", name);
      //System.out.println("set to " + get("serial.port"));
    }
  }
  */


  /*
  protected Vector buildPortList() {
    // get list of names for serial ports
    // have the default port checked (if present)
    Vector list = new Vector();

    //SerialMenuListener listener = new SerialMenuListener();
    boolean problem = false;

    // if this is failing, it may be because
    // lib/javax.comm.properties is missing.
    // java is weird about how it searches for java.comm.properties
    // so it tends to be very fragile. i.e. quotes in the CLASSPATH
    // environment variable will hose things.
    try {
      //System.out.println("building port list");
      Enumeration portList = CommPortIdentifier.getPortIdentifiers();
      while (portList.hasMoreElements()) {
        CommPortIdentifier portId =
          (CommPortIdentifier) portList.nextElement();
        //System.out.println(portId);

        if (portId.getPortType() == CommPortIdentifier.PORT_SERIAL) {
          //if (portId.getName().equals(port)) {
          String name = portId.getName();
          //CheckboxMenuItem mi =
          //new CheckboxMenuItem(name, name.equals(defaultName));

          //mi.addItemListener(listener);
          //serialMenu.add(mi);
          list.addElement(name);
        }
      }
    } catch (UnsatisfiedLinkError e) {
      e.printStackTrace();
      problem = true;

    } catch (Exception e) {
      System.out.println("exception building serial menu");
      e.printStackTrace();
    }

    //if (serialMenu.getItemCount() == 0) {
      //System.out.println("dimming serial menu");
    //serialMenu.setEnabled(false);
    //}

    // only warn them if this is the first time
    if (problem && PdeBase.firstTime) {
      JOptionPane.showMessageDialog(this, //frame,
                                    "Serial port support not installed.\n" +
                                    "Check the readme for instructions\n" +
                                    "if you need to use the serial port.    ",
                                    "Serial Port Warning",
                                    JOptionPane.WARNING_MESSAGE);
    }
    return list;
  }
  */


