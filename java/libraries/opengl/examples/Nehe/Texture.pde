import com.sun.opengl.util.BufferUtil;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.PixelGrabber;
import java.io.IOException;
import java.nio.ByteBuffer;

Texture readTexture(String filename) throws IOException {
  return readTexture(filename, false);
}

Texture readTexture(String filename, boolean storeAlphaChannel) throws IOException {
  BufferedImage bufferedImage;
  if (filename.endsWith(".bmp")) {
    bufferedImage = loadBitmap(filename);
  } 
  else {
    bufferedImage = readImage(filename);
  }
  return readPixels(bufferedImage, storeAlphaChannel);
}

BufferedImage readImage(String resourceName) throws IOException {
  return ImageIO.read(createInput(resourceName));
}

Texture readPixels(BufferedImage img, boolean storeAlphaChannel) {
  int[] packedPixels = new int[img.getWidth() * img.getHeight()];

  PixelGrabber pixelgrabber = new PixelGrabber(img, 0, 0, img.getWidth(), img.getHeight(), packedPixels, 0, img.getWidth());
  try {
    pixelgrabber.grabPixels();
  } 
  catch (InterruptedException e) {
    throw new RuntimeException();
  }

  int bytesPerPixel = storeAlphaChannel ? 4 : 3;
  ByteBuffer unpackedPixels = BufferUtil.newByteBuffer(packedPixels.length * bytesPerPixel);

  for (int row = img.getHeight() - 1; row >= 0; row--) {
    for (int col = 0; col < img.getWidth(); col++) {
      int packedPixel = packedPixels[row * img.getWidth() + col];
      unpackedPixels.put((byte) ((packedPixel >> 16) & 0xFF));
      unpackedPixels.put((byte) ((packedPixel >> 8) & 0xFF));
      unpackedPixels.put((byte) ((packedPixel >> 0) & 0xFF));
      if (storeAlphaChannel) {
        unpackedPixels.put((byte) ((packedPixel >> 24) & 0xFF));
      }
    }
  }

  unpackedPixels.flip();


  return new Texture(unpackedPixels, img.getWidth(), img.getHeight());
}

class Texture {
  ByteBuffer pixels;
  int width;
  int height;

  public Texture(ByteBuffer pixels, int width, int height) {
    this.height = height;
    this.pixels = pixels;
    this.width = width;
  }

  public int getHeight() {
    return height;
  }

  public ByteBuffer getPixels() {
    return pixels;
  }

  public int getWidth() {
    return width;
  }
}

