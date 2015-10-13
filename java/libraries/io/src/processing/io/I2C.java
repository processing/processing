/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Copyright (c) The Processing Foundation 2015
  I/O library developed by Gottfried Haider as part of GSOC 2015

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

package processing.io;

import processing.io.NativeInterface;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;


public class I2C {

  protected String dev;
  protected int handle;
  protected int slave;
  protected byte[] out;
  protected boolean transmitting;


  /**
   *  Opens an I2C device as master
   *
   *  @param dev device name
   *  @see list
   */
  public I2C(String dev) {
    NativeInterface.loadLibrary();
    this.dev = dev;
    handle = NativeInterface.openDevice("/dev/" + dev);
    if (handle < 0) {
      throw new RuntimeException(NativeInterface.getError(handle));
    }
  }


  /**
   *  Close the I2C device
   */
  public void close() {
    NativeInterface.closeDevice(handle);
    handle = 0;
  }


  protected void finalize() throws Throwable {
    try {
      close();
    } finally {
      super.finalize();
    }
  }


  /**
   *  Begins a transmission to an attached device
   *
   *  I2C addresses consist of 7 bits plus one bit that indicates whether
   *  the device is being read from or written to. Some datasheets list
   *  the address in an 8 bit form (7 address bits + R/W bit), while others
   *  provide the address in a 7 bit form, with the address in the lower
   *  7 bits. This function expects the address in the lower 7 bits, the
   *  same way as in Arduino's Wire library, and as shown in the output
   *  of the i2cdetect tool.
   *  If the address provided in a datasheet is greater than 127 (hex 0x7f)
   *  or there are separate addresses for read and write operations listed,
   *  which vary exactly by one, then you want to shif the this number by
   *  one bit to the right before passing it as an argument to this function.
   *  @param slave 7 bit address of slave device
   *  @see write
   *  @see read
   *  @see endTransmission
   */
  public void beginTransmission(int slave) {
    // addresses 120 (0x78) to 127 are additionally reserved
    if (0x78 <= slave) {
      System.err.println("beginTransmission expects a 7 bit address, try shifting one bit to the right");
      throw new IllegalArgumentException("Illegal address");
    }
    this.slave = slave;
    transmitting = true;
    out = null;
  }


  /**
   *  Ends the current transmissions
   *
   *  This executes any queued writes.
   *  @see beginTransmission
   *  @see write
   */
  public void endTransmission() {
    if (!transmitting) {
      // silently ignore this case
      return;
    }

    // implement these flags if needed: https://github.com/raspberrypi/linux/blob/rpi-patches/Documentation/i2c/i2c-protocol
    int ret = NativeInterface.transferI2c(handle, slave, out, null);
    transmitting = false;
    out = null;
    if (ret < 0) {
      if (ret == -5) {    // EIO
        System.err.println("The device did not respond. Check the cabling and whether you are using the correct address.");
      }
      throw new RuntimeException(NativeInterface.getError(ret));
    }
  }


  /**
   *  Lists all available I2C devices
   *  @return String array
   */
  public static String[] list() {
    ArrayList<String> devs = new ArrayList<String>();
    File dir = new File("/dev");
    File[] files = dir.listFiles();
    if (files != null) {
      for (File file : files) {
        if (file.getName().startsWith("i2c-")) {
          devs.add(file.getName());
        }
      }
    }
    // listFiles() does not guarantee ordering
    String[] tmp = devs.toArray(new String[devs.size()]);
    Arrays.sort(tmp);
    return tmp;
  }


  /**
   *  Reads bytes from the attached device
   *
   *  You must call beginTransmission() before calling this function. This function
   *  also ends the current transmisison and sends any data that was queued using
   *  write() before.
   *  @param len number of bytes to read
   *  @return bytes read from device
   *  @see beginTransmission
   *  @see write
   *  @see endTransmission
   */
  public byte[] read(int len) {
    if (!transmitting) {
      throw new RuntimeException("beginTransmisson has not been called");
    }

    byte[] in = new byte[len];

    int ret = NativeInterface.transferI2c(handle, slave, out, in);
    transmitting = false;
    out = null;
    if (ret < 0) {
      if (ret == -5) {    // EIO
        System.err.println("The device did not respond. Check the cabling and whether you are using the correct address.");
      }
      throw new RuntimeException(NativeInterface.getError(ret));
    }

    return in;
  }


  /**
   *  Adds bytes to be written to the device
   *
   *  You must call beginTransmission() before calling this function.
   *  The actual writing takes part when read() or endTransmission() is being
   *  called.
   *  @param out bytes to be written
   *  @see beginTransmission
   *  @see read
   *  @see endTransmission
   */
  public void write(byte[] out) {
    if (!transmitting) {
      throw new RuntimeException("beginTransmisson has not been called");
    }

    if (this.out == null) {
      this.out = out;
    } else {
      byte[] tmp = new byte[this.out.length + out.length];
      System.arraycopy(this.out, 0, tmp, 0, this.out.length);
      System.arraycopy(out, 0, tmp, this.out.length, out.length);
      this.out = tmp;
    }
  }


  /**
   *  Adds bytes to be written to the device
   *
   *  You must call beginTransmission() before calling this function.
   *  The actual writing takes part when read() or endTransmission() is being
   *  called.
   *  @param out string to be written
   *  @see beginTransmission
   *  @see read
   *  @see endTransmission
   */
  public void write(String out) {
    write(out.getBytes());
  }


  /**
   *  Adds a byte to be written to the device
   *
   *  You must call beginTransmission() before calling this function.
   *  The actual writing takes part when read() or endTransmission() is being
   *  called.
   *  @param out single byte to be written (0-255)
   *  @see beginTransmission
   *  @see read
   *  @see endTransmission
   */
  public void write(int out) {
    if (out < 0 || 255 < out) {
      System.err.println("The write function can only operate on a single byte at a time. Call it with a value from 0 to 255.");
      throw new RuntimeException("Argument does not fit into a single byte");
    }
    byte[] tmp = new byte[1];
    tmp[0] = (byte)out;
    write(tmp);
  }
}
