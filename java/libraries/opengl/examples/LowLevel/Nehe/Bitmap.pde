import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.io.IOException;
import java.io.InputStream;

BufferedImage loadBitmap(String file) throws IOException {
  BufferedImage image;
  InputStream input = null;
  try {
    input = createInput(file);

    int bitmapFileHeaderLength = 14;
    int bitmapInfoHeaderLength = 40;

    byte bitmapFileHeader[] = new byte[bitmapFileHeaderLength];
    byte bitmapInfoHeader[] = new byte[bitmapInfoHeaderLength];

    input.read(bitmapFileHeader, 0, bitmapFileHeaderLength);
    input.read(bitmapInfoHeader, 0, bitmapInfoHeaderLength);

    int nSize = bytesToInt(bitmapFileHeader, 2);
    int nWidth = bytesToInt(bitmapInfoHeader, 4);
    int nHeight = bytesToInt(bitmapInfoHeader, 8);
    int nBiSize = bytesToInt(bitmapInfoHeader, 0);
    int nPlanes = bytesToShort(bitmapInfoHeader, 12);
    int nBitCount = bytesToShort(bitmapInfoHeader, 14);
    int nSizeImage = bytesToInt(bitmapInfoHeader, 20);
    int nCompression = bytesToInt(bitmapInfoHeader, 16);
    int nColoursUsed = bytesToInt(bitmapInfoHeader, 32);
    int nXPixelsMeter = bytesToInt(bitmapInfoHeader, 24);
    int nYPixelsMeter = bytesToInt(bitmapInfoHeader, 28);
    int nImportantColours = bytesToInt(bitmapInfoHeader, 36);

    if (nBitCount == 24) {
      image = read24BitBitmap(nSizeImage, nHeight, nWidth, input);
    } 
    else if (nBitCount == 8) {
      image = read8BitBitmap(nColoursUsed, nBitCount, nSizeImage, nWidth, nHeight, input);
    } 
    else {
      System.out.println("Not a 24-bit or 8-bit Windows Bitmap, aborting...");
      image = null;
    }
  } 
  finally {
    try {
      if (input != null)
        input.close();
    } 
    catch (IOException e) {
    }
  }
  return image;
}

BufferedImage read8BitBitmap(int nColoursUsed, int nBitCount, int nSizeImage, int nWidth, int nHeight, InputStream input) throws IOException {
  int nNumColors = (nColoursUsed > 0) ? nColoursUsed : (1 & 0xff) << nBitCount;

  if (nSizeImage == 0) {
    nSizeImage = ((((nWidth * nBitCount) + 31) & ~31) >> 3);
    nSizeImage *= nHeight;
  }

  int npalette[] = new int[nNumColors];
  byte bpalette[] = new byte[nNumColors * 4];
  readBuffer(input, bpalette);
  int nindex8 = 0;

  for (int n = 0; n < nNumColors; n++) {
    npalette[n] = (255 & 0xff) << 24 |
      (bpalette[nindex8 + 2] & 0xff) << 16 |
      (bpalette[nindex8 + 1] & 0xff) << 8 |
      (bpalette[nindex8 + 0] & 0xff);

    nindex8 += 4;
  }

  int npad8 = (nSizeImage / nHeight) - nWidth;
  BufferedImage bufferedImage = new BufferedImage(nWidth, nHeight, BufferedImage.TYPE_INT_ARGB);
  DataBufferInt dataBufferByte = ((DataBufferInt) bufferedImage.getRaster().getDataBuffer());
  int[][] bankData = dataBufferByte.getBankData();
  byte bdata[] = new byte[(nWidth + npad8) * nHeight];

  readBuffer(input, bdata);
  nindex8 = 0;

  for (int j8 = nHeight - 1; j8 >= 0; j8--) {
    for (int i8 = 0; i8 < nWidth; i8++) {
      bankData[0][j8 * nWidth + i8] = npalette[((int) bdata[nindex8] & 0xff)];
      nindex8++;
    }
    nindex8 += npad8;
  }

  return bufferedImage;
}

BufferedImage read24BitBitmap(int nSizeImage, int nHeight, int nWidth, InputStream input) throws IOException {
  int npad = (nSizeImage / nHeight) - nWidth * 3;
  if (npad == 4 || npad < 0)
    npad = 0;
  int nindex = 0;
  BufferedImage bufferedImage = new BufferedImage(nWidth, nHeight, BufferedImage.TYPE_4BYTE_ABGR);
  DataBufferByte dataBufferByte = ((DataBufferByte) bufferedImage.getRaster().getDataBuffer());
  byte[][] bankData = dataBufferByte.getBankData();
  byte brgb[] = new byte[(nWidth + npad) * 3 * nHeight];

  readBuffer(input, brgb);

  for (int j = nHeight - 1; j >= 0; j--) {
    for (int i = 0; i < nWidth; i++) {
      int base = (j * nWidth + i) * 4;
      bankData[0][base] = (byte) 255;
      bankData[0][base + 1] = brgb[nindex];
      bankData[0][base + 2] = brgb[nindex + 1];
      bankData[0][base + 3] = brgb[nindex + 2];
      nindex += 3;
    }
    nindex += npad;
  }

  return bufferedImage;
}

int bytesToInt(byte[] bytes, int index) {
  return (bytes[index + 3] & 0xff) << 24 |
    (bytes[index + 2] & 0xff) << 16 |
    (bytes[index + 1] & 0xff) << 8 |
    bytes[index + 0] & 0xff;
}

short bytesToShort(byte[] bytes, int index) {
  return (short) (((bytes[index + 1] & 0xff) << 8) |
    (bytes[index + 0] & 0xff));
}

void readBuffer(InputStream in, byte[] buffer) throws IOException {
  int bytesRead = 0;
  int bytesToRead = buffer.length;
  while (bytesToRead > 0) {
    int read = in.read(buffer, bytesRead, bytesToRead);
    bytesRead += read;
    bytesToRead -= read;
  }
}

