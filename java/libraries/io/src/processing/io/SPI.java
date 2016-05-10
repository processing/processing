/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Copyright (c) The Processing Foundation 2015
  Hardware I/O library developed by Gottfried Haider as part of GSoC 2015

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
import java.util.HashMap;
import java.util.Map;


/**
 *  @webref
 */
public class SPI {

  /**
   *  CPOL=0, CPHA=0, most common
   */
  public static final int MODE0 = 0;
  /**
   *  CPOL=0, CPHA=1
   */
  public static final int MODE1 = 1;
  /**
   *  CPOL=1, CPHA=0
   */
  public static final int MODE2 = 2;
  /**
   *  CPOL=1, CPHA=1
   */
  public static final int MODE3 = 3;
  /**
   *  most significant bit first, most common
   */
  public static final int MSBFIRST = 0;
  /**
   *  least significant bit first
   */
  public static final int LSBFIRST = 1;

  protected int dataOrder = 0;
  protected String dev;
  protected int handle;
  protected int maxSpeed = 500000;
  protected int mode = 0;
  protected static Map<String, String> settings = new HashMap<String, String>();


  /**
   *  Opens an SPI interface as master
   *  @param dev device name
   *  @see list
   *  @webref
   */
  public SPI(String dev) {
    NativeInterface.loadLibrary();
    this.dev = dev;

    if (NativeInterface.isSimulated()) {
      return;
    }

    handle = NativeInterface.openDevice("/dev/" + dev);
    if (handle < 0) {
      throw new RuntimeException(NativeInterface.getError(handle));
    }
  }


  /**
   *  Closes the SPI interface
   *  @webref
   */
  public void close() {
    if (NativeInterface.isSimulated()) {
      return;
    }

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
   *  Lists all available SPI interfaces
   *  @return String array
   *  @webref
   */
  public static String[] list() {
    if (NativeInterface.isSimulated()) {
      // as on the Raspberry Pi
      return new String[]{ "spidev0.0", "spidev0.1" };
    }

    ArrayList<String> devs = new ArrayList<String>();
    File dir = new File("/dev");
    File[] files = dir.listFiles();
    if (files != null) {
      for (File file : files) {
        if (file.getName().startsWith("spidev")) {
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
   *  Configures the SPI interface
   *  @param maxSpeed maximum transmission rate in Hz, 500000 (500 kHz) is a resonable default
   *  @param dataOrder whether data is send with the first- or least-significant bit first (SPI.MSBFIRST or SPI.LSBFIRST, the former is more common)
   *  @param mode <a href="https://en.wikipedia.org/wiki/Serial_Peripheral_Interface_Bus#Clock_polarity_and_phase">SPI.MODE0 to SPI.MODE3</a>
   *  @webref
   */
  public void settings(int maxSpeed, int dataOrder, int mode) {
    this.maxSpeed = maxSpeed;
    this.dataOrder = dataOrder;
    this.mode = mode;
  }


  /**
   *  Transfers data over the SPI bus
   *  @param out bytes to send
   *  @return bytes read in (array is the same length as out)
   *  @webref
   */
  public byte[] transfer(byte[] out) {
    if (NativeInterface.isSimulated()) {
      return new byte[out.length];
    }

    // track the current setting per device across multiple instances
    String curSettings = maxSpeed + "-" + dataOrder + "-" + mode;
    if (!curSettings.equals(settings.get(dev))) {
      int ret = NativeInterface.setSpiSettings(handle, maxSpeed, dataOrder, mode);
      if (ret < 0) {
        System.err.println(NativeInterface.getError(handle));
        throw new RuntimeException("Error updating device configuration");
      }
      settings.put(dev, curSettings);
    }

    byte[] in = new byte[out.length];
    int transferred = NativeInterface.transferSpi(handle, out, in);
    if (transferred < 0) {
      throw new RuntimeException(NativeInterface.getError(transferred));
    } else if (transferred < out.length) {
      throw new RuntimeException("Fewer bytes transferred than requested: " + transferred);
    }
    return in;
  }


  /**
   *  Transfers data over the SPI bus
   *  @param out string to send
   *  @return bytes read in (array is the same length as out)
   */
  public byte[] transfer(String out) {
    return transfer(out.getBytes());
  }


  /**
   *  Transfers data over the SPI bus
   *  @param out single byte to send, e.g. numeric literal (0 to 255, or -128 to 127)
   *  @return bytes read in (array is the same length as out)
   */
  public byte[] transfer(int out) {
    if (out < -128 || 255 < out) {
      System.err.println("The transfer function can only operate on a single byte at a time. Call it with a value from 0 to 255, or -128 to 127.");
      throw new RuntimeException("Argument does not fit into a single byte");
    }
    byte[] tmp = new byte[1];
    tmp[0] = (byte)out;
    return transfer(tmp);
  }


  /**
   *  Transfers data over the SPI bus
   *  @param out single byte to send
   *  @return bytes read in (array is the same length as out)
   */
  public byte[] transfer(byte out) {
    // cast to (unsigned) int
    return transfer(out & 0xff);
  }
}
