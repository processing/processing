package processing.core.util.image.loadstrategy;

import processing.core.PApplet;
import processing.core.PImage;

import javax.swing.*;
import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.util.Base64;

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
