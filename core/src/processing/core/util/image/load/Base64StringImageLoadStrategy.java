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
import java.util.Base64;


/**
 * Strategy for loading images from Base64 strings.
 */
public class Base64StringImageLoadStrategy implements ImageLoadStrategy {

  @Override
  public PImage load(PApplet pApplet, String content, String extension) {
    byte[] decodedBytes = Base64.getDecoder().decode(content);

    if(decodedBytes == null){
      System.err.println("Decode Error on image: " + content.substring(0, 20));
      return null;
    }

    Image awtImage = new ImageIcon(decodedBytes).getImage();

    if (awtImage instanceof BufferedImage) {
      BufferedImage buffImage = (BufferedImage) awtImage;
      int space = buffImage.getColorModel().getColorSpace().getType();
      if (space == ColorSpace.TYPE_CMYK) {
        return null;
      }
    }

    PImage loadedImage = new PImage(
        awtImage,
        ImageLoadUtil.checkExtensionRequiresAlpha(extension)
    );

    if (loadedImage.width == -1) {
      // error...
    }

    return loadedImage;
  }

}
