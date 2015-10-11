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


public class NativeInterface {

  protected static boolean loaded = false;

  public static void loadLibrary() {
    if (!loaded) {
      if (!"Linux".equals(System.getProperty("os.name"))) {
        throw new RuntimeException("The Processing I/O library is only supported on Linux");
      }
      System.loadLibrary("processing-io");
      loaded = true;
    }
  }


  public static native int openDevice(String fn);
  public static native String getError(int errno);
  public static native int closeDevice(int handle);

  // the following two functions were done in native code to get access to the
  // specifc error number (errno) that might occur
  public static native int readFile(String fn, byte[] in);
  public static native int writeFile(String fn, byte[] out);
  public static int writeFile(String fn, String out) {
    return writeFile(fn, out.getBytes());
  }

  /* GPIO */
  public static native int pollDevice(String fn, int timeout);
  /* I2C */
  public static native int transferI2c(int handle, int slave, byte[] out, byte[] in);
  /* SPI */
  public static native int setSpiSettings(int handle, int maxSpeed, int dataOrder, int mode);
  public static native int transferSpi(int handle, byte[] out, byte[] in);
}
