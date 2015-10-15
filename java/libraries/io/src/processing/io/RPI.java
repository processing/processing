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


/**
 *  @webref
 */
public class RPI {

  /*
   * The Raspberry Pi has a 2x20 pin header for connecting various peripherals.
   * The following constants describe how the pins of this header correspond
   * to the pin numbering used by the CPU and by software ("GPIO pin number").
   *
   * You can use this class to refer to a pin by its location on the header:
   * e.g. GPIO.digitalWrite(RPI.PIN7, GPIO.HIGH)
   *
   * Alternatively, if you know a pins "true" pin number (GPIO number), you
   * can use it directly. The following is equivalent to the example above:
   * GPIO.digitalWrite(4, GPIO.HIGH)
   *
   * PIN1 is located on the "top left" of the column of pins, close to the two
   * LEDs. PIN2 is next to it. PIN3 is below PIN1, and it goes on like this.
   * See also: http://pi.gadgetoid.com/pinout
   */

  public static final int PIN1 = -1;    /* 3v3 Power, can source up to 50 mA */
  public static final int PIN2 = -1;    /* 5v Power, connected to input power */
  public static final int PIN3 = 2;     /* GPIO 2, also: I2C data */
  public static final int PIN4 = -1;    /* 5v Power, connected to input power */
  public static final int PIN5 = 3;     /* GPIO 3, also: I2C clock */
  public static final int PIN6 = -1;    /* Ground */
  public static final int PIN7 = 4;     /* GPIO 4 */
  public static final int PIN8 = 14;    /* GPIO 14, also: Serial TX */
  public static final int PIN9 = -1;    /* Ground */
  public static final int PIN10 = 15;   /* GPIO 15, also: Serial RX */
  public static final int PIN11 = 17;   /* GPIO 17 */
  public static final int PIN12 = 18;   /* GPIO 18 */
  public static final int PIN13 = 27;   /* GPIO 27 */
  public static final int PIN14 = -1;   /* Ground */
  public static final int PIN15 = 22;   /* GPIO 22 */
  public static final int PIN16 = 23;   /* GPIO 23 */
  public static final int PIN17 = -1;   /* 3v3 Power, can source up to 50 mA */
  public static final int PIN18 = 24;   /* GPIO 24 */
  public static final int PIN19 = 10;   /* GPIO 10, also: SPI MOSI */
  public static final int PIN20 = -1;   /* Ground */
  public static final int PIN21 = 9;    /* GPIO 9, also: SPI MISO */
  public static final int PIN22 = 25;   /* GPIO 25 */
  public static final int PIN23 = 11;   /* GPIO 11, also: SPI SCLK */
  public static final int PIN24 = 8;    /* GPIO 8, also: SPI Chip Select 0 */
  public static final int PIN25 = -1;   /* Ground */
  public static final int PIN26 = 7;    /* GPIO 7, also: SPI Chip Select 1 */
  public static final int PIN27 = 0;    /* GPIO 0, also: HAT I2C data, reserved [currenly not accessible] */
  public static final int PIN28 = 1;    /* GPIO 1, also HAT I2C data, reserved [currenly not accessible] */
  public static final int PIN29 = 5;    /* GPIO 5 */
  public static final int PIN30 = -1;   /* Ground */
  public static final int PIN31 = 6;    /* GPIO 6 */
  public static final int PIN32 = 12;   /* GPIO 12 */
  public static final int PIN33 = 13;   /* GPIO 13 */
  public static final int PIN34 = -1;   /* Ground */
  public static final int PIN35 = 19;   /* GPIO 19, also: SPI MISO [currenly not accessible] */
  public static final int PIN36 = 16;   /* GPIO 16 */
  public static final int PIN37 = 26;   /* GPIO 26 */
  public static final int PIN38 = 20;   /* GPIO 20, also: SPI MISO [currenly not accessible] */
  public static final int PIN39 = -1;   /* Ground */
  public static final int PIN40 = 21;   /* GPIO 21, also: SPI CLK [currenly not accessible] */
}
