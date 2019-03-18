/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2012-18 The Processing Foundation
  Copyright (c) 2004-12 Ben Fry and Casey Reas
  Copyright (c) 2001-04 Massachusetts Institute of Technology

  This library is free software; you can redistribute it and/or
  modify it under the terms of the GNU Lesser General Public
  License as published by the Free Software Foundation, version 2.1.

  This library is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
  Lesser General Public License for more details.

  You should have received a copy of the GNU Lesser General
  Public License along with this library; if not, write to the
  Free Software Foundation, Inc., 59 Temple Place, Suite 330,
  Boston, MA  02111-1307  USA
*/

package processing.core.util.image.load;

import processing.core.PApplet;
import processing.core.PImage;

import javax.swing.*;
import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;


/**
 * Strategy to load an image through ImageIcon / abstract window toolkit.
 */
public class AwtImageLoadStrategy implements ImageLoadStrategy {

  @Override
  public PImage load(PApplet pApplet, String path, String extension) {
    byte bytes[] = pApplet.loadBytes(path);
    if (bytes == null) {
      return null;
    } else {
      //Image awtImage = Toolkit.getDefaultToolkit().createImage(bytes);
      Image awtImage = new ImageIcon(bytes).getImage();

      if (awtImage instanceof BufferedImage) {
        BufferedImage buffImage = (BufferedImage) awtImage;
        int space = buffImage.getColorModel().getColorSpace().getType();
        if (space == ColorSpace.TYPE_CMYK) {
          System.err.println(path + " is a CMYK image, " +
              "only RGB images are supported.");
          return null;
              /*
              // wishful thinking, appears to not be supported
              // https://community.oracle.com/thread/1272045?start=0&tstart=0
              BufferedImage destImage =
                new BufferedImage(buffImage.getWidth(),
                                  buffImage.getHeight(),
                                  BufferedImage.TYPE_3BYTE_BGR);
              ColorConvertOp op = new ColorConvertOp(null);
              op.filter(buffImage, destImage);
              image = new PImage(destImage);
              */
        }
      }

      boolean checkAlpha = extension.equalsIgnoreCase("gif")
          || extension.equalsIgnoreCase("png")
          || extension.equalsIgnoreCase("unknown");

      PImage image = new PImage(awtImage, checkAlpha, pApplet);
      return image;
    }
  }

}
