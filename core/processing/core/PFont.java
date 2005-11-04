package processing.core;

import java.io.*;
import javax.microedition.lcdui.*;

/**
 * Part of the Mobile Processing project - http://mobile.processing.org
 *
 * Copyright (c) 2004-05 Francis Li
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General
 * Public License along with this library; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA  02111-1307  USA
 *
 * @author  Francis Li
 */
public class PFont {
    public int      charCount;
    
    public PImage   images[];
    public char     value[];        // char code
    public byte     heights[];       // height of the bitmap data
    public byte     width[];        // width of bitmap data
    public byte     setWidth[];     // width displaced by the char
    public byte     topExtent[];    // offset for the top
    public byte     leftExtent[];   // offset for the left

    protected int   ascii[];        // quick lookup for the ascii chars
    
    public final int    baseline;
    public final int    height;
    
    public Font     font;
    
    public PFont(Font font) {
        this.font = font;
        height = font.getHeight();
        baseline = font.getBaselinePosition();
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
            heights     = new byte[charCount];
            width       = new byte[charCount];
            setWidth    = new byte[charCount];
            topExtent   = new byte[charCount];
            leftExtent  = new byte[charCount];

            ascii = new int[128];
            for (int i = 0; i < 128; i++) ascii[i] = -1;

            int baseline = 0;
            int height = 0;            
            // read the information about the individual characters
            for (int i = 0; i < charCount; i++) {
              value[i]      = dis.readChar();
              heights[i]    = dis.readByte();
              width[i]      = dis.readByte();
              setWidth[i]   = dis.readByte();
              topExtent[i]  = dis.readByte();
              leftExtent[i] = dis.readByte();

              // cache locations of the ascii charset
              if (value[i] < 128) ascii[value[i]] = i;
              
              //// get height and baseline
              baseline = Math.max(baseline, topExtent[i]);
              height = Math.max(height, baseline + heights[i] - topExtent[i]);
            }
            this.baseline = baseline;
            this.height = height;

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
