package processing.core.util.image.load;

import processing.core.PApplet;
import processing.core.PImage;

import javax.swing.*;
import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;


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
