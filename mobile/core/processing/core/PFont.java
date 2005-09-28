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
    
    public PFont(InputStream is) {
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
            for (int i = 0; i < charCount; i++) {
                pngSize = dis.readInt();
                pngData = new byte[pngSize];
                dis.readFully(pngData);
                images[i] = new PImage(pngData);
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
