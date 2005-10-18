package processing.core;

import java.io.*;
import javax.microedition.lcdui.*;

public class PFont {
    public int      charCount;
    
    public PImage   images[];
    public char     value[];        // char code
    public byte     height[];       // height of the bitmap data
    public byte     width[];        // width of bitmap data
    public byte     setWidth[];     // width displaced by the char
    public byte     topExtent[];    // offset for the top
    public byte     leftExtent[];   // offset for the left

    protected int   ascii[];        // quick lookup for the ascii chars
    
    public Font     font;
    
    public PFont(Font font) {
        this.font = font;
    }
    
    public PFont(InputStream is, int color, int bgcolor) {
        try {
            DataInputStream dis = new DataInputStream(is);

            // number of character images stored in this font
            charCount = dis.readInt();
            // version of format
            int version = dis.readInt();

            // allocate enough space for the character info
            value       = new char[charCount];
            height      = new byte[charCount];
            width       = new byte[charCount];
            setWidth    = new byte[charCount];
            topExtent   = new byte[charCount];
            leftExtent  = new byte[charCount];

            ascii = new int[128];
            for (int i = 0; i < 128; i++) ascii[i] = -1;

            // read the information about the individual characters
            for (int i = 0; i < charCount; i++) {
              value[i]      = dis.readChar();
              height[i]     = dis.readByte();
              width[i]      = dis.readByte();
              setWidth[i]   = dis.readByte();
              topExtent[i]  = dis.readByte();
              leftExtent[i] = dis.readByte();

              // cache locations of the ascii charset
              if (value[i] < 128) ascii[value[i]] = i;
            }

            images = new PImage[charCount];
            int pngSize;
            byte[] pngData;
            byte[] palette = null;
            for (int i = 0; i < charCount; i++) {
                pngSize = dis.readInt();
                if (pngSize > 0) {
                    pngData = new byte[pngSize];
                    dis.readFully(pngData);
                    if ((color != 0) || (bgcolor != 0xffffff)) {
                        //// modify palette before decoding
                        final int offset = 8 /* png signature */ + 25 /* IHDR chunk */ + 8 /* PLTE chunk header */;
                        if (palette == null) {
                            //// calculate a gradient from bgcolor to color
                            int r = (bgcolor & 0xff0000) >> 16;
                            int g = (bgcolor & 0xff00) >> 8;
                            int b = bgcolor & 0xff;
                            int dr = ((color & 0xff0000) >> 16) - r;
                            int dg = ((color & 0xff00) >> 8) - g;
                            int db = (color & 0xff) - b;
                            int index = offset;
                            for (int j = 0; j < 256; j++) {
                                pngData[index++] = (byte) (r + j * dr / 255);
                                pngData[index++] = (byte) (g + j * dg / 255);
                                pngData[index++] = (byte) (b + j * db / 255);
                            }
                            //// recalculate crc
                            int crc = PMIDlet.crc(pngData, offset - 4, (768 + 4));
                            pngData[offset + 768] = (byte) ((crc & 0xff000000) >> 24);
                            pngData[offset + 769] = (byte) ((crc & 0xff0000) >> 16);
                            pngData[offset + 770] = (byte) ((crc & 0xff00) >> 8);
                            pngData[offset + 771] = (byte) (crc & 0xff);
                            //// save new palette
                            palette = new byte[768 + 4];
                            System.arraycopy(pngData, offset, palette, 0, 768 + 4);
                        } else {
                            System.arraycopy(palette, 0, pngData, offset, 768 + 4);
                        }
                    }
                    images[i] = new PImage(pngData);
                }
            }            
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }
    
    public int getIndex(char c) {
        if (c < 128) {
            return ascii[c];
        }
        return getIndex(c, 0, charCount - 1);
    }

    private int getIndex(int c, int start, int stop) {
        int pivot = (start + stop) / 2;

        // if this is the char, then return it
        if (c == value[pivot]) return pivot;

        // char doesn't exist, otherwise would have been the pivot
        //if (start == stop) return -1;
        if (start >= stop) return -1;

        // if it's in the lower half, continue searching that
        if (c < value[pivot]) return getIndex(c, start, pivot-1);

        // if it's in the upper half, continue there
        return getIndex(c, pivot+1, stop);
    }
}
