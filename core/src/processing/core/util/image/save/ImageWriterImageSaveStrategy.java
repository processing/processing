package processing.core.util.image.save;

import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.util.io.PathUtil;

import javax.imageio.*;
import javax.imageio.metadata.IIOInvalidTreeException;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;

/**
 * Use ImageIO functions from Java 1.4 and later to handle image save.
 * Various formats are supported, typically jpeg, png, bmp, and wbmp.
 * To get a list of the supported formats for writing, use: <BR>
 * <TT>println(javax.imageio.ImageIO.getReaderFormatNames())</TT>
 */
public class ImageWriterImageSaveStrategy implements ImageSaveStrategy {

  @Override
  public boolean save(int[] pixels, int pixelWidth, int pixelHeight, int format, String path) throws IOException {
    try {
      int outputFormat = (format == PConstants.ARGB) ?
          BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;

      String extension = PathUtil.cleanExtension(
          PathUtil.parseExtension(path)
      );

      // JPEG and BMP images that have an alpha channel set get pretty unhappy.
      // BMP just doesn't write, and JPEG writes it as a CMYK image.
      // http://code.google.com/p/processing/issues/detail?id=415
      if (isRgbImage(extension)) {
        outputFormat = BufferedImage.TYPE_INT_RGB;
      }

      BufferedImage bimage = new BufferedImage(pixelWidth, pixelHeight, outputFormat);
      bimage.setRGB(0, 0, pixelWidth, pixelHeight, pixels, 0, pixelWidth);

      File file = new File(path);

      ImageWriter writer = null;
      ImageWriteParam param = null;
      IIOMetadata metadata = null;

      if (isJpeg(extension)) {
        if ((writer = getImageIoWriter("jpeg")) != null) {
          // Set JPEG quality to 90% with baseline optimization. Setting this
          // to 1 was a huge jump (about triple the size), so this seems good.
          // Oddly, a smaller file size than Photoshop at 90%, but I suppose
          // it's a completely different algorithm.
          param = writer.getDefaultWriteParam();
          param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
          param.setCompressionQuality(0.9f);
        }
      }

      if (isPng(extension)) {
        if ((writer = getImageIoWriter("png")) != null) {
          param = writer.getDefaultWriteParam();
          if (false) {
            metadata = getImageIoDpi(writer, param, 100);
          }
        }
      }

      if (writer != null) {
        BufferedOutputStream output =
            new BufferedOutputStream(PApplet.createOutput(file));
        writer.setOutput(ImageIO.createImageOutputStream(output));
//        writer.write(null, new IIOImage(bimage, null, null), param);
        writer.write(metadata, new IIOImage(bimage, null, metadata), param);
        writer.dispose();

        output.flush();
        output.close();
        return true;
      }
      // If iter.hasNext() somehow fails up top, it falls through to here
      return javax.imageio.ImageIO.write(bimage, extension, file);

    } catch (Exception e) {
      e.printStackTrace();
      throw new IOException("image save failed.");
    }
  }

  private ImageWriter getImageIoWriter(String extension) {
    Iterator<ImageWriter> iter = ImageIO.getImageWritersByFormatName(extension);
    if (iter.hasNext()) {
      return iter.next();
    }
    return null;
  }

  private IIOMetadata getImageIoDpi(ImageWriter writer, ImageWriteParam param, double dpi) {
    // http://stackoverflow.com/questions/321736/how-to-set-dpi-information-in-an-image
    ImageTypeSpecifier typeSpecifier =
        ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_INT_RGB);
    IIOMetadata metadata =
        writer.getDefaultImageMetadata(typeSpecifier, param);

    if (!metadata.isReadOnly() && metadata.isStandardMetadataFormatSupported()) {
      // for PNG, it's dots per millimeter
      double dotsPerMilli = dpi / 25.4;

      IIOMetadataNode horiz = new IIOMetadataNode("HorizontalPixelSize");
      horiz.setAttribute("value", Double.toString(dotsPerMilli));

      IIOMetadataNode vert = new IIOMetadataNode("VerticalPixelSize");
      vert.setAttribute("value", Double.toString(dotsPerMilli));

      IIOMetadataNode dim = new IIOMetadataNode("Dimension");
      dim.appendChild(horiz);
      dim.appendChild(vert);

      IIOMetadataNode root = new IIOMetadataNode("javax_imageio_1.0");
      root.appendChild(dim);

      try {
        metadata.mergeTree("javax_imageio_1.0", root);
        return metadata;

      } catch (IIOInvalidTreeException e) {
        System.err.println("Could not set the DPI of the output image");
        e.printStackTrace();
      }
    }
    return null;
  }

  private boolean isRgbImage(String extension) {
    return extension.equals("bmp") || isJpeg(extension);
  }

  private boolean isJpeg(String extension) {
    return extension.equals("jpg") || extension.equals("jpeg");
  }

  private boolean isPng(String extension) {
    return extension.equals("png");
  }

}
