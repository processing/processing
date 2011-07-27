/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Client - basic network client implementation
  Part of the Processing project - http://processing.org

  Copyright (c) 2004-2007 Ben Fry and Casey Reas
  The previous version of this code was developed by Hernando Barragan

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

package processing.net;
import processing.core.*;

import java.io.*;
import java.lang.reflect.*;
import java.net.*;

/**
 * @generate Client.xml 
 * @webref net
 * @brief The client class is used to create client Objects which connect to a server to exchange data. 
 * @instanceName client any variable of type Client
 * @usage Application
 */
public class Client implements Runnable {

  PApplet parent;
  Method clientEventMethod;
  Method disconnectEventMethod;

  Thread thread;
  Socket socket;
  String ip;
  int port;
  String host;

  public InputStream input;
  public OutputStream output;

  byte buffer[] = new byte[32768];
  int bufferIndex;
  int bufferLast;

  /**
   * 
   * @param parent typically use "this"
   * @param host address of the server
   * @param port port to read/write from on the server
   */
  public Client(PApplet parent, String host, int port) {
    this.parent = parent;
    this.host = host;
    this.port = port;

    try {
      socket = new Socket(this.host, this.port);
      input = socket.getInputStream();
      output = socket.getOutputStream();

      thread = new Thread(this);
      thread.start();

      parent.registerDispose(this);

      // reflection to check whether host applet has a call for
      // public void clientEvent(processing.net.Client)
      // which would be called each time an event comes in
      try {
        clientEventMethod =
          parent.getClass().getMethod("clientEvent",
                                      new Class[] { Client.class });
      } catch (Exception e) {
        // no such method, or an error.. which is fine, just ignore
      }
      // do the same for disconnectEvent(Client c);
      try {
        disconnectEventMethod =
          parent.getClass().getMethod("disconnectEvent",
                                      new Class[] { Client.class });
      } catch (Exception e) {
        // no such method, or an error.. which is fine, just ignore
      }

    } catch (ConnectException ce) {
      ce.printStackTrace();
      dispose();

    } catch (IOException e) {
      e.printStackTrace();
      dispose();
    }
  }

  /**
   * @param socket any object of type Socket
   * @throws IOException
   */
  public Client(PApplet parent, Socket socket) throws IOException {
    this.socket = socket;

    input = socket.getInputStream();
    output = socket.getOutputStream();

    thread = new Thread(this);
    thread.start();
  }


  /**
   * @generate Client_stop.xml
   * @webref client:client
   * @brief Disconnects from the server
   * @usage application
   */
  public void stop() {
    dispose();
    if (disconnectEventMethod != null) {
      try {
        disconnectEventMethod.invoke(parent, new Object[] { this });
      } catch (Exception e) {
        e.printStackTrace();
        disconnectEventMethod = null;
      }
    }
  }


  /**
   * Disconnect from the server: internal use only.
   * <P>
   * This should only be called by the internal functions in PApplet,
   * use stop() instead from within your own applets.
   */
  public void dispose() {
    thread = null;
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
      if (socket != null) socket.close();

    } catch (Exception e) {
      e.printStackTrace();
    }
    socket = null;
  }


  public void run() {
    while (Thread.currentThread() == thread) {
      try {
        while ((input != null) &&
               (input.available() > 0)) {  // this will block
          synchronized (buffer) {
            if (bufferLast == buffer.length) {
              byte temp[] = new byte[bufferLast << 1];
              System.arraycopy(buffer, 0, temp, 0, bufferLast);
              buffer = temp;
            }
            buffer[bufferLast++] = (byte) input.read();
          }
        }
        // now post an event
        if (clientEventMethod != null) {
          try {
            clientEventMethod.invoke(parent, new Object[] { this });
          } catch (Exception e) {
            System.err.println("error, disabling clientEvent() for " + host);
            e.printStackTrace();
            clientEventMethod = null;
          }
        }

        try {
          // uhh.. not sure what's best here.. since blocking,
          // do we need to worry about sleeping much? or is this
          // gonna try to slurp cpu away from the main applet?
          Thread.sleep(10);
        } catch (InterruptedException ex) { }

      } catch (IOException e) {
        //errorMessage("run", e);
        e.printStackTrace();
      }
    }
  }


  /**
   * Return true if this client is still active and hasn't run
   * into any trouble.
   */
  public boolean active() {
    return (thread != null);
  }


  /**
   * @generate Client_ip.xml
   * @webref client:client
   * @usage application
   * @brief Returns the IP address of the machine as a String
   */
  public String ip() {
    return socket.getInetAddress().getHostAddress();
  }


  /**
   * @generate Client_available.xml
   * @webref client:client
   * @usage application
   * @brief Returns the number of bytes in the buffer waiting to be read
   */
  public int available() {
    return (bufferLast - bufferIndex);
  }


  /**
   * @generate Client_clear.xml 
   * @webref client:client
   * @usage application
   * @brief Clears the buffer
   */
  public void clear() {
    bufferLast = 0;
    bufferIndex = 0;
  }


  /**
   * @generate Client_read.xml
   * @webref client:client
   * @usage application
   * @brief Returns a value from the buffer
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
   * @generate Client_readChar.xml
   * @webref client:client
   * @usage application
   * @brief Returns the next byte in the buffer as a char
   */
  public char readChar() {
    if (bufferIndex == bufferLast) return (char)(-1);
    return (char) read();
  }


  /**
   * @generate Client_readBytes.xml
   * <h3>Advanced</h3>
   * Return a byte array of anything that's in the serial buffer.
   * Not particularly memory/speed efficient, because it creates
   * a byte array on each read, but it's easier to use than
   * readBytes(byte b[]) (see below).
   * 
   * @webref client:client
   * @usage application
   * @brief Reads everything in the buffer
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
   * 
   * @param bytebuffer passed in byte array to be altered
   */
  public int readBytes(byte bytebuffer[]) {
    if (bufferIndex == bufferLast) return 0;

    synchronized (buffer) {
      int length = bufferLast - bufferIndex;
      if (length > bytebuffer.length) length = bytebuffer.length;
      System.arraycopy(buffer, bufferIndex, bytebuffer, 0, length);

      bufferIndex += length;
      if (bufferIndex == bufferLast) {
        bufferIndex = 0;  // rewind
        bufferLast = 0;
      }
      return length;
    }
  }


  /**
   * @generate Client_readBytesUntil.xml 
   * @webref client:client
   * @usage application
   * @brief Reads from the buffer of bytes up to and including a particular character
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
        bufferIndex = 0; // rewind
        bufferLast = 0;
      }
      return outgoing;
    }
  }


  /**
   * <h3>Advanced</h3>
   * Reads from the serial port into a buffer of bytes until a
   * particular character. If the character isn't in the serial
   * buffer, then 'null' is returned.
   *
   * If outgoing[] is not big enough, then -1 is returned,
   *   and an error message is printed on the console.
   * If nothing is in the buffer, zero is returned.
   * If 'interesting' byte is not in the buffer, then 0 is returned.
   * 
   * @param byteBuffer passed in byte array to be altered
   */
  public int readBytesUntil(int interesting, byte byteBuffer[]) {
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
      if (length > byteBuffer.length) {
        System.err.println("readBytesUntil() byte buffer is" +
                           " too small for the " + length +
                           " bytes up to and including char " + interesting);
        return -1;
      }
      //byte outgoing[] = new byte[length];
      System.arraycopy(buffer, bufferIndex, byteBuffer, 0, length);

      bufferIndex += length;
      if (bufferIndex == bufferLast) {
        bufferIndex = 0;  // rewind
        bufferLast = 0;
      }
      return length;
    }
  }


  /**
   * @generate Client_readString.xml 
   * @webref client:client
   * @usage application
   * @brief Returns the buffer as a String
   */
  public String readString() {
    if (bufferIndex == bufferLast) return null;
    return new String(readBytes());
  }


  /**
   * @generate Client_readStringUntil.xml
   * <h3>Advanced</h3>
   * <p/>
   * If you want to move Unicode data, you can first convert the
   * String to a byte stream in the representation of your choice
   * (i.e. UTF8 or two-byte Unicode data), and send it as a byte array.
   * 
   * @webref client:client
   * @usage application
   * @brief Returns the buffer as a String up to and including a particular character
   * @param interesting character designated to mark the end of the data
   */
  public String readStringUntil(int interesting) {
    byte b[] = readBytesUntil(interesting);
    if (b == null) return null;
    return new String(b);
  }


  /**
   * @generate Client_write.xml 
   * @webref client:client
   * @usage application
   * @brief  	Writes bytes, chars, ints, bytes[], Strings
   * @param data data to write
   */
  public void write(int data) {  // will also cover char
    try {
      output.write(data & 0xff);  // for good measure do the &
      output.flush();   // hmm, not sure if a good idea

    } catch (Exception e) { // null pointer or serial port dead
      //errorMessage("write", e);
      //e.printStackTrace();
      //dispose();
      //disconnect(e);
      e.printStackTrace();
      stop();
    }
  }


  public void write(byte data[]) {
    try {
      output.write(data);
      output.flush();   // hmm, not sure if a good idea

    } catch (Exception e) { // null pointer or serial port dead
      //errorMessage("write", e);
      //e.printStackTrace();
      //disconnect(e);
      e.printStackTrace();
      stop();
    }
  }


  /**
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
   */
  public void write(String data) {
    write(data.getBytes());
  }


  /**
   * Handle disconnect due to an Exception being thrown.
   */
  /*
    protected void disconnect(Exception e) {
    dispose();
    if (e != null) {
    e.printStackTrace();
    }
    }
  */


  /**
   * General error reporting, all corraled here just in case
   * I think of something slightly more intelligent to do.
   */
  //public void errorMessage(String where, Exception e) {
  //parent.die("Error inside Client." + where + "()", e);
  //e.printStackTrace(System.err);
  //}
}
