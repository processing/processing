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
   * ( begin auto-generated from Client.xml )
   * 
   * A client connects to a server and sends data back and forth. If anything 
   * goes wrong with the connection, for example the host is not there or is 
   * listening on a different port, an exception is thrown.
   * 
   * ( end auto-generated )
 * @webref net
 * @brief The client class is used to create client Objects which connect to a server to exchange data. 
 * @instanceName client any variable of type Client
 * @usage Application
 * @see_external LIB_net/clientEvent
 */
public class Client implements Runnable {

  protected static final int MAX_BUFFER_SIZE = 1 << 27; // 128 MB

  PApplet parent;
  Method clientEventMethod;
  Method disconnectEventMethod;

  volatile Thread thread;
  Socket socket;
  int port;
  String host;

  public InputStream input;
  public OutputStream output;

  final Object bufferLock = new Object[0];

  byte buffer[] = new byte[32768];
  int bufferIndex;
  int bufferLast;

  boolean disposeRegistered = false;
  
  
  /**
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

      parent.registerMethod("dispose", this);
      disposeRegistered = true;

      // reflection to check whether host sketch has a call for
      // public void clientEvent(processing.net.Client)
      // which would be called each time an event comes in
      try {
        clientEventMethod =
          parent.getClass().getMethod("clientEvent", Client.class);
      } catch (Exception e) {
        // no such method, or an error.. which is fine, just ignore
      }
      // do the same for disconnectEvent(Client c);
      try {
        disconnectEventMethod =
          parent.getClass().getMethod("disconnectEvent", Client.class);
      } catch (Exception e) {
        // no such method, or an error.. which is fine, just ignore
      }

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
    this.parent = parent;
    this.socket = socket;

    input = socket.getInputStream();
    output = socket.getOutputStream();

    thread = new Thread(this);
    thread.start();

    // reflection to check whether host sketch has a call for
    // public void clientEvent(processing.net.Client)
    // which would be called each time an event comes in
    try {
      clientEventMethod =
          parent.getClass().getMethod("clientEvent", Client.class);
    } catch (Exception e) {
      // no such method, or an error.. which is fine, just ignore
    }
    // do the same for disconnectEvent(Client c);
    try {
      disconnectEventMethod =
        parent.getClass().getMethod("disconnectEvent", Client.class);
    } catch (Exception e) {
      // no such method, or an error.. which is fine, just ignore
    }
  }


  /**
   * ( begin auto-generated from Client_stop.xml )
   * 
   * Disconnects from the server. Use to shut the connection when you're 
   * finished with the Client.
   * 
   * ( end auto-generated )
   * @webref client:client
   * @brief Disconnects from the server
   * @usage application
   */
  public void stop() {    
    if (disconnectEventMethod != null && thread != null){
      try {
        disconnectEventMethod.invoke(parent, this);
      } catch (Exception e) {
        Throwable cause = e;
        // unwrap the exception if it came from the user code
        if (e instanceof InvocationTargetException && e.getCause() != null) {
          cause = e.getCause();
        }
        cause.printStackTrace();
        disconnectEventMethod = null;
      }
    }
    if (disposeRegistered) {
      parent.unregisterMethod("dispose", this);
      disposeRegistered = false;
    }
    dispose();
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
      if (input != null) {
        input.close();
        input = null;
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

    try {
      if (output != null) {
        output.close();
        output = null;
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    
    try {
      if (socket != null) {
        socket.close();
        socket = null;
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }


  @Override
  public void run() {
    byte[] readBuffer;
    { // make the read buffer same size as socket receive buffer so that
      // we don't waste cycles calling listeners when there is more data waiting
      int readBufferSize = 1 << 16; // 64 KB (default socket receive buffer size)
      try {
        readBufferSize = socket.getReceiveBufferSize();
      } catch (SocketException ignore) { }
      readBuffer = new byte[readBufferSize];
    }
    while (Thread.currentThread() == thread) {
      try {
        while (input != null) {
          int readCount;

          // try to read a byte using a blocking read. 
          // An exception will occur when the sketch is exits.
          try {
            readCount = input.read(readBuffer, 0, readBuffer.length);
          } catch (SocketException e) {
             System.err.println("Client SocketException: " + e.getMessage());
             // the socket had a problem reading so don't try to read from it again.
             stop();
             return;
          }
        
          // read returns -1 if end-of-stream occurs (for example if the host disappears)
          if (readCount == -1) {
            System.err.println("Client got end-of-stream.");
            stop();
            return;
          }

          synchronized (bufferLock) {
            int freeBack = buffer.length - bufferLast;
            if (readCount > freeBack) {
              // not enough space at the back
              int bufferLength = bufferLast - bufferIndex;
              byte[] targetBuffer = buffer;
              if (bufferLength + readCount > buffer.length) {
                // can't fit even after compacting, resize the buffer
                // find the next power of two which can fit everything in
                int newSize = Integer.highestOneBit(bufferLength + readCount - 1) << 1;
                if (newSize > MAX_BUFFER_SIZE) {
                  // buffer is full because client is not reading (fast enough)
                  System.err.println("Client: can't receive more data, buffer is full. " +
                                         "Make sure you read the data from the client.");
                  stop();
                  return;
                }
                targetBuffer = new byte[newSize];
              }
              // compact the buffer (either in-place or into the new bigger buffer)
              System.arraycopy(buffer, bufferIndex, targetBuffer, 0, bufferLength);
              bufferLast -= bufferIndex;
              bufferIndex = 0;
              buffer = targetBuffer;
            }
            // copy all newly read bytes into the buffer
            System.arraycopy(readBuffer, 0, buffer, bufferLast, readCount);
            bufferLast += readCount;
          }

          // now post an event
          if (clientEventMethod != null) {
            try {
              clientEventMethod.invoke(parent, this);
            } catch (Exception e) {
              System.err.println("error, disabling clientEvent() for " + host);
              Throwable cause = e;
              // unwrap the exception if it came from the user code
              if (e instanceof InvocationTargetException && e.getCause() != null) {
                cause = e.getCause();
              }
              cause.printStackTrace();
              clientEventMethod = null;
            }
          }
        }
      } catch (IOException e) {
        //errorMessage("run", e);
        e.printStackTrace();
      }
    }
  }


  /**
   * ( begin auto-generated from Client_active.xml )
   * 
   * Returns true if this client is still active and hasn't run
   * into any trouble.
   * 
   * ( end auto-generated )
   * @webref client:client
   * @brief Returns true if this client is still active
   * @usage application
   */
  public boolean active() {
    return (thread != null);
  }


  /**
   * ( begin auto-generated from Client_ip.xml )
   * 
   * Returns the IP address of the computer to which the Client is attached.
   * 
   * ( end auto-generated )
   * @webref client:client
   * @usage application
   * @brief Returns the IP address of the machine as a String
   */
  public String ip() {
    if (socket != null){
      return socket.getInetAddress().getHostAddress();
    }
    return null;
  }


  /**
   * ( begin auto-generated from Client_available.xml )
   * 
   * Returns the number of bytes available. When any client has bytes 
   * available from the server, it returns the number of bytes.
   * 
   * ( end auto-generated )
   * @webref client:client
   * @usage application
   * @brief Returns the number of bytes in the buffer waiting to be read
   */
  public int available() {
    synchronized (bufferLock) {
      return (bufferLast - bufferIndex);
    }
  }


  /**
   * ( begin auto-generated from Client_clear.xml )
   * 
   * Empty the buffer, removes all the data stored there.
   * 
   * ( end auto-generated )
   * @webref client:client
   * @usage application
   * @brief Clears the buffer
   */
  public void clear() {
    synchronized (bufferLock) {
      bufferLast = 0;
      bufferIndex = 0;
    }
  }


  /**
   * ( begin auto-generated from Client_read.xml )
   * 
   * Returns a number between 0 and 255 for the next byte that's waiting in 
   * the buffer. Returns -1 if there is no byte, although this should be 
   * avoided by first cheacking <b>available()</b> to see if any data is available.
   * 
   * ( end auto-generated )
   * @webref client:client
   * @usage application
   * @brief Returns a value from the buffer
   */
  public int read() {
    synchronized (bufferLock) {
      if (bufferIndex == bufferLast) return -1;

      int outgoing = buffer[bufferIndex++] & 0xff;
      if (bufferIndex == bufferLast) {  // rewind
        bufferIndex = 0;
        bufferLast = 0;
      }
      return outgoing;
    }
  }


  /**
   * ( begin auto-generated from Client_readChar.xml )
   * 
   * Returns the next byte in the buffer as a char. Returns -1 or 0xffff if 
   * nothing is there.
   * 
   * ( end auto-generated )
   * @webref client:client
   * @usage application
   * @brief Returns the next byte in the buffer as a char
   */
  public char readChar() {
    synchronized (bufferLock) {
      if (bufferIndex == bufferLast) return (char) (-1);
      return (char) read();
    }
  }


  /**
   * ( begin auto-generated from Client_readBytes.xml )
   * 
   * Reads a group of bytes from the buffer. The version with no parameters 
   * returns a byte array of all data in the buffer. This is not efficient, 
   * but is easy to use. The version with the <b>byteBuffer</b> parameter is 
   * more memory and time efficient. It grabs the data in the buffer and puts 
   * it into the byte array passed in and returns an int value for the number 
   * of bytes read. If more bytes are available than can fit into the 
   * <b>byteBuffer</b>, only those that fit are read.
   * 
   * ( end auto-generated )
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
    synchronized (bufferLock) {
      if (bufferIndex == bufferLast) return null;

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
   * Return a byte array of anything that's in the serial buffer
   * up to the specified maximum number of bytes.
   * Not particularly memory/speed efficient, because it creates
   * a byte array on each read, but it's easier to use than
   * readBytes(byte b[]) (see below).
   *
   * @param max the maximum number of bytes to read
   */
  public byte[] readBytes(int max) {
    synchronized (bufferLock) {
      if (bufferIndex == bufferLast) return null;

      int length = bufferLast - bufferIndex;
      if (length > max) length = max;
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
    synchronized (bufferLock) {
      if (bufferIndex == bufferLast) return 0;

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
   * ( begin auto-generated from Client_readBytesUntil.xml )
   * 
   * Reads from the port into a buffer of bytes up to and including a 
   * particular character. If the character isn't in the buffer, 'null' is 
   * returned. The version with no <b>byteBuffer</b> parameter returns a byte 
   * array of all data up to and including the <b>interesting</b> byte. This 
   * is not efficient, but is easy to use. The version with the 
   * <b>byteBuffer</b> parameter is more memory and time efficient. It grabs 
   * the data in the buffer and puts it into the byte array passed in and 
   * returns an int value for the number of bytes read. If the byte buffer is 
   * not large enough, -1 is returned and an error is printed to the message 
   * area. If nothing is in the buffer, 0 is returned.
   * 
   * ( end auto-generated )
   * @webref client:client
   * @usage application
   * @brief Reads from the buffer of bytes up to and including a particular character
   * @param interesting character designated to mark the end of the data
   */
  public byte[] readBytesUntil(int interesting) {
    byte what = (byte)interesting;

    synchronized (bufferLock) {
      if (bufferIndex == bufferLast) return null;

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
    byte what = (byte)interesting;

    synchronized (bufferLock) {
      if (bufferIndex == bufferLast) return 0;

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
   * ( begin auto-generated from Client_readString.xml )
   * 
   * Returns the all the data from the buffer as a String. This method 
   * assumes the incoming characters are ASCII. If you want to transfer 
   * Unicode data, first convert the String to a byte stream in the 
   * representation of your choice (i.e. UTF8 or two-byte Unicode data), and 
   * send it as a byte array.
   * 
   * ( end auto-generated )
   * @webref client:client
   * @usage application
   * @brief Returns the buffer as a String
   */
  public String readString() {
    byte b[] = readBytes();
    if (b == null) return null;
    return new String(b);
  }


  /**
   * ( begin auto-generated from Client_readStringUntil.xml )
   * 
   * Combination of <b>readBytesUntil()</b> and <b>readString()</b>. Returns 
   * <b>null</b> if it doesn't find what you're looking for.
   * 
   * ( end auto-generated )
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
   * ( begin auto-generated from Client_write.xml )
   * 
   * Writes data to a server specified when constructing the client.
   * 
   * ( end auto-generated )
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
   * General error reporting, all corralled here just in case
   * I think of something slightly more intelligent to do.
   */
  //public void errorMessage(String where, Exception e) {
  //parent.die("Error inside Client." + where + "()", e);
  //e.printStackTrace(System.err);
  //}
}
