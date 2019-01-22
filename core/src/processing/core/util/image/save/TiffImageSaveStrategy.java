package processing.core.util.image.save;

import processing.core.util.image.constants.TifConstants;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;

public class TiffImageSaveStrategy implements ImageSaveStrategy {

  @Override
  public boolean save(int[] pixels, int pixelWidth, int pixelHeight, int format,
      String filename) throws FileNotFoundException {

    OutputStream output = ImageSaveUtil.createForFile(filename);

    // shutting off the warning, people can figure this out themselves
    /*
    if (format != RGB) {
      System.err.println("Warning: only RGB information is saved with " +
                         ".tif files. Use .tga or .png for ARGB images and others.");
    }
    */
    try {
      byte tiff[] = new byte[768];
      System.arraycopy(
          TifConstants.TIFF_HEADER,
          0,
          tiff,
          0,
          TifConstants.TIFF_HEADER.length
      );

      tiff[30] = (byte) ((pixelWidth >> 8) & 0xff);
      tiff[31] = (byte) ((pixelWidth) & 0xff);
      tiff[42] = tiff[102] = (byte) ((pixelHeight >> 8) & 0xff);
      tiff[43] = tiff[103] = (byte) ((pixelHeight) & 0xff);

      int count = pixelWidth*pixelHeight*3;
      tiff[114] = (byte) ((count >> 24) & 0xff);
      tiff[115] = (byte) ((count >> 16) & 0xff);
      tiff[116] = (byte) ((count >> 8) & 0xff);
      tiff[117] = (byte) ((count) & 0xff);

      // spew the header to the disk
      output.write(tiff);

      for (int i = 0; i < pixels.length; i++) {
        output.write((pixels[i] >> 16) & 0xff);
        output.write((pixels[i] >> 8) & 0xff);
        output.write(pixels[i] & 0xff);
      }
      output.flush();
      output.close();
      return true;

    } catch (IOException e) {
      e.printStackTrace();
    }
    return false;
  }

}
