/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2004-10 Ben Fry and Casey Reas
  Copyright (c) 2001-04 Massachusetts Institute of Technology

  This library is free software; you can redistribute it and/or
  modify it under the terms of the GNU Lesser General Public
  License version 2.1 as published by the Free Software Foundation.

  This library is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
  Lesser General Public License for more details.

  You should have received a copy of the GNU Lesser General
  Public License along with this library; if not, write to the
  Free Software Foundation, Inc., 59 Temple Place, Suite 330,
  Boston, MA  02111-1307  USA
*/

package processing.core;

import java.io.*;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.Bitmap.Config;

/**
 * Grayscale bitmap font class used by Processing.
 * <P>
 * Awful (and by that, I mean awesome) ASCII (non-)art for how this works:
 * <PRE>
 *   |
 *   |                   height is the full used height of the image
 *   |
 *   |   ..XX..       }
 *   |   ..XX..       }
 *   |   ......       }
 *   |   XXXX..       }  topExtent (top y is baseline - topExtent)
 *   |   ..XX..       }
 *   |   ..XX..       }  dotted areas are where the image data
 *   |   ..XX..       }  is actually located for the character
 *   +---XXXXXX----   }  (it extends to the right and down
 *   |                   for power of two texture sizes)
 *   ^^^^ leftExtent (amount to move over before drawing the image
 *
 *   ^^^^^^^^^^^^^^ setWidth (width displaced by char)
 * </PRE>
 */
public class PFont implements PConstants {

  /** Number of character glyphs in this font. */
  protected int glyphCount;

  /**
   * Actual glyph data. The length of this array won't necessarily be the
   * same size as glyphCount, in cases where lazy font loading is in use.
   */
  protected Glyph[] glyphs;

  /**
   * Name of the font as seen by Java when it was created.
   * If the font is available, the native version will be used.
   */
  protected String name;

  /**
   * Postscript name of the font that this bitmap was created from.
   */
  protected String psname;

  /**
   * The original size of the font when it was first created
   */
  protected int size;

  /** true if smoothing was enabled for this font, used for native impl */
  protected boolean smooth;

  /**
   * The ascent of the font. If the 'd' character is present in this PFont,
   * this value is replaced with its pixel height, because the values returned
   * by FontMetrics.getAscent() seem to be terrible.
   */
  protected int ascent;

  /**
   * The descent of the font. If the 'p' character is present in this PFont,
   * this value is replaced with its lowest pixel height, because the values
   * returned by FontMetrics.getDescent() are gross.
   */
  protected int descent;

  /**
   * A more efficient array lookup for straight ASCII characters. For Unicode
   * characters, a QuickSort-style search is used.
   */
  protected int[] ascii;

  /**
   * True if this font is set to load dynamically. This is the default when
   * createFont() method is called without a character set. Bitmap versions of
   * characters are only created when prompted by an index() call.
   */
  protected boolean lazy;

  /**
   * Native Android version of the font. If possible, this allows the
   * PGraphics subclass to just use Android's font rendering stuff
   * in situations where that's faster.
   */
  protected Typeface typeface;

  /**
   * True if this font should return 'null' for getFont(), so that the native
   * font will be used to create a subset, but the native version of the font
   * will not be used. 
   */
  protected boolean subsetting; 
  
  /**
   * True if we've already tried to find the native version of this font.
   */
  protected boolean typefaceSearched;

  /**
   * Array of the native system fonts. Used to lookup native fonts by their
   * PostScript name. This is a workaround for a several year old Apple Java
   * bug that they can't be bothered to fix.
   */
  static protected Typeface[] typefaces;
  
  // objects to handle creation of font characters only as they're needed
  Bitmap lazyBitmap;
  Canvas lazyCanvas;
  Paint lazyPaint;
//  FontMetrics lazyMetrics;
  int[] lazySamples;
  
  /** for subclasses that need to store metadata about the font */
  protected HashMap<PGraphics, Object> cacheMap;    
  
  
  public PFont() { }  // for subclasses


  /**
   * Create a new Processing font from a native font, but don't create all the
   * characters at once, instead wait until they're used to include them.
   * @param font
   * @param smooth
   */
  public PFont(Typeface font, int size, boolean smooth) {
    this(font, size, smooth, null);
  }


  /**
   * Create a new image-based font on the fly. If charset is set to null,
   * the characters will only be created as bitmaps when they're drawn.
   *
   * @param font the font object to create from
   * @param charset array of all unicode chars that should be included
   * @param smooth true to enable smoothing/anti-aliasing
   */
  public PFont(Typeface font, int size, boolean smooth, char charset[]) {
    // save this so that we can use the native version
    this.typeface = font;
    this.smooth = smooth;

    name = ""; //font.getName();
    psname = ""; //font.getPSName();
    this.size = size;  //font.getSize();

    int initialCount = 10;
    glyphs = new Glyph[initialCount];

    ascii = new int[128];
    Arrays.fill(ascii, -1);

    int mbox3 = size * 3;

//    lazyImage = new BufferedImage(mbox3, mbox3, BufferedImage.TYPE_INT_RGB);
    lazyBitmap = Bitmap.createBitmap(mbox3, mbox3, Config.ARGB_8888);
//    lazyGraphics = (Graphics2D) lazyImage.getGraphics();
    lazyCanvas = new Canvas(lazyBitmap);

//    lazyGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
//                                  smooth ?
//                                  RenderingHints.VALUE_ANTIALIAS_ON :
//                                  RenderingHints.VALUE_ANTIALIAS_OFF);
//    // adding this for post-1.0.9
//    lazyGraphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
//                                  smooth ?
//                                  RenderingHints.VALUE_TEXT_ANTIALIAS_ON :
//                                  RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
    lazyPaint = new Paint();
    lazyPaint.setAntiAlias(smooth);

//    lazyGraphics.setFont(font);
    lazyPaint.setTypeface(font);
//    lazyMetrics = lazyGraphics.getFontMetrics();
    lazyPaint.setTextSize(size);
    lazySamples = new int[mbox3 * mbox3];

//    ascent = lazyMetrics.getAscent();
//    descent = lazyMetrics.getDescent();

    if (charset == null) {
      lazy = true;
      
    } else {
      // charset needs to be sorted to make index lookup run more quickly
      // http://dev.processing.org/bugs/show_bug.cgi?id=494
      Arrays.sort(charset);

      glyphs = new Glyph[charset.length];

      glyphCount = 0;
      for (char c : charset) {
        Glyph glyf = new Glyph(c);
        if (glyf.value < 128) {
          ascii[glyf.value] = glyphCount;
        }
        glyf.index = glyphCount;
        glyphs[glyphCount++] = glyf;
      }

      // shorten the array if necessary
//      if (glyphCount != charset.length) {
//        glyphs = (Glyph[]) PApplet.subset(glyphs, 0, glyphCount);
//      }
    }

    // If not already created, just create these two characters to calculate
    // the ascent and descent values for the font. This was tested to only
    // require 5-10 ms on a 2.4 GHz MacBook Pro.
    // In versions 1.0.9 and earlier, fonts that could not display d or p
    // used the max up/down values as calculated by looking through the font.
    // That's no longer valid with the auto-generating fonts, so we'll just
    // use getAscent() and getDescent() in such (minor) cases.
    if (ascent == 0) {
      new Glyph('d');
      if (ascent == 0) {  // character not valid
        ascent = PApplet.round(lazyPaint.ascent());
      }
    }
    if (descent == 0) {
      new Glyph('p');
      if (descent == 0) {
        descent = PApplet.round(lazyPaint.descent());
      }
    }
  }


  public PFont(InputStream input) throws IOException {
    DataInputStream is = new DataInputStream(input);

    // number of character images stored in this font
    glyphCount = is.readInt();

    // used to be the bitCount, but now used for version number.
    // version 8 is any font before 69, so 9 is anything from 83+
    // 9 was buggy so gonna increment to 10.
    int version = is.readInt();

    // this was formerly ignored, now it's the actual font size
    //mbox = is.readInt();
    size = is.readInt();

    // this was formerly mboxY, the one that was used
    // this will make new fonts downward compatible
    is.readInt();  // ignore the other mbox attribute

    ascent  = is.readInt();  // formerly baseHt (zero/ignored)
    descent = is.readInt();  // formerly ignored struct padding

    // allocate enough space for the character info
    glyphs = new Glyph[glyphCount];

    ascii = new int[128];
    Arrays.fill(ascii, -1);

    // read the information about the individual characters
    for (int i = 0; i < glyphCount; i++) {
      Glyph glyph = new Glyph(is);
      // cache locations of the ascii charset
      if (glyph.value < 128) {
        ascii[glyph.value] = i;
      }
      glyph.index = i;
      glyphs[i] = glyph;
    }

    // not a roman font, so throw an error and ask to re-build.
    // that way can avoid a bunch of error checking hacks in here.
    if ((ascent == 0) && (descent == 0)) {
      throw new RuntimeException("Please use \"Create Font\" to " +
                                 "re-create this font.");
    }

    for (Glyph glyph : glyphs) {
      glyph.readBitmap(is);
    }

    if (version >= 10) {  // includes the font name at the end of the file
      name = is.readUTF();
      psname = is.readUTF();
    }
    if (version == 11) {
      smooth = is.readBoolean();
    }
  }
  
  
  void delete() {
    if (cacheMap != null) {    
      Set<PGraphics> keySet = cacheMap.keySet();
      if (!keySet.isEmpty()) {
        Object[] keys = keySet.toArray();
        for (int i = 0; i < keys.length; i++) {
          Object data = getCache((PGraphics)keys[i]);
          Method del = null;
          
          try {
            Class<?> c = data.getClass();
            del = c.getMethod("delete", new Class[] {});
          } catch (Exception e) {}
          
          if (del != null) {
            // The metadata have a delete method. We try running it.
            try {
              del.invoke(data, new Object[] {});
            } catch (Exception e) {}
          }   
        }
      }    
    }
  }
  
  
  /**
   * Write this PFont to an OutputStream.
   * <p>
   * This is used by the Create Font tool, or whatever anyone else dreams
   * up for messing with fonts themselves.
   * <p>
   * It is assumed that the calling class will handle closing
   * the stream when finished.
   */
  public void save(OutputStream output) throws IOException {
    DataOutputStream os = new DataOutputStream(output);

    os.writeInt(glyphCount);

    if ((name == null) || (psname == null)) {
      name = "";
      psname = "";
    }

    os.writeInt(11);      // formerly numBits, now used for version number
    os.writeInt(size);    // formerly mboxX (was 64, now 48)
    os.writeInt(0);       // formerly mboxY, now ignored
    os.writeInt(ascent);  // formerly baseHt (was ignored)
    os.writeInt(descent); // formerly struct padding for c version

    for (int i = 0; i < glyphCount; i++) {
      glyphs[i].writeHeader(os);
    }

    for (int i = 0; i < glyphCount; i++) {
      glyphs[i].writeBitmap(os);
    }

    // version 11
    os.writeUTF(name);
    os.writeUTF(psname);
    os.writeBoolean(smooth);

    os.flush();
  }


  /**
   * Create a new glyph, and add the character to the current font.
   * @param c character to create an image for.
   */
  protected void addGlyph(char c) {
    Glyph glyph = new Glyph(c);

    if (glyphCount == glyphs.length) {
      glyphs = (Glyph[]) PApplet.expand(glyphs);
    }
    if (glyphCount == 0) {
      glyph.index = 0;
      glyphs[glyphCount] = glyph;
      if (glyph.value < 128) {
        ascii[glyph.value] = 0;
      }

    } else if (glyphs[glyphCount-1].value < glyph.value) {
      glyphs[glyphCount] = glyph;
      if (glyph.value < 128) {
        ascii[glyph.value] = glyphCount;
      }

    } else {
      for (int i = 0; i < glyphCount; i++) {
        if (glyphs[i].value > c) {
          for (int j = glyphCount; j > i; --j) {
            glyphs[j] = glyphs[j-1];
            if (glyphs[j].value < 128) {
              ascii[glyphs[j].value] = j;
            }
          }
          glyph.index = i;
          glyphs[i] = glyph;
          // cache locations of the ascii charset
          if (c < 128) ascii[c] = i;
          break;
        }
      }
    }
    glyphCount++;
  }


  public String getName() {
    return name;
  }


  public String getPostScriptName() {
    return psname;
  }


  /**
   * Set the native complement of this font.
   */
  public void setTypeface(Typeface typeface) {
    this.typeface = typeface;
  }


  /**
   * Return the native Typeface object associated with this PFont (if any).
   */
  public Typeface getTypeface() {
    if (subsetting) {
      return null;
    }
    return typeface;
  }


  /**
   * Attempt to find the native version of this font.
   * (Public so that it can be used by OpenGL or other renderers.)
   */
  static public Typeface findTypeface(String name) {
    loadTypefaces();
    return typefaceMap.get(name);
  }


  public Glyph getGlyph(char c) {
    int index = index(c);
    return (index == -1) ? null : glyphs[index];
  }


  /**
   * Get index for the character.
   * @return index into arrays or -1 if not found
   */
  protected int index(char c) {
    if (lazy) {
      int index = indexActual(c);
      if (index != -1) {
        return index;
      }
//      if (font.canDisplay(c)) {
        // create the glyph
        addGlyph(c);
        // now where did i put that?
        return indexActual(c);

//      } else {
//        return -1;
//      }

    } else {
      return indexActual(c);
    }
  }


  protected int indexActual(char c) {
    // degenerate case, but the find function will have trouble
    // if there are somehow zero chars in the lookup
    //if (value.length == 0) return -1;
    if (glyphCount == 0) return -1;

    // quicker lookup for the ascii fellers
    if (c < 128) return ascii[c];

    // some other unicode char, hunt it out
    //return index_hunt(c, 0, value.length-1);
    return indexHunt(c, 0, glyphCount-1);
  }


  protected int indexHunt(int c, int start, int stop) {
    int pivot = (start + stop) / 2;

    // if this is the char, then return it
    if (c == glyphs[pivot].value) return pivot;

    // char doesn't exist, otherwise would have been the pivot
    //if (start == stop) return -1;
    if (start >= stop) return -1;

    // if it's in the lower half, continue searching that
    if (c < glyphs[pivot].value) return indexHunt(c, start, pivot-1);

    // if it's in the upper half, continue there
    return indexHunt(c, pivot+1, stop);
  }


  /**
   * Currently un-implemented for .vlw fonts,
   * but honored for layout in case subclasses use it.
   */
  public float kern(char a, char b) {
    return 0;
  }


  /**
   * Returns the ascent of this font from the baseline.
   * The value is based on a font of size 1.
   */
  public float ascent() {
    return ((float) ascent / (float) size);
  }


  /**
   * Returns how far this font descends from the baseline.
   * The value is based on a font size of 1.
   */
  public float descent() {
    return ((float) descent / (float) size);
  }


  /**
   * Width of this character for a font of size 1.
   */
  public float width(char c) {
    if (c == 32) return width('i');

    int cc = index(c);
    if (cc == -1) return 0;

    return ((float) glyphs[cc].setWidth / (float) size);
  }


  //////////////////////////////////////////////////////////////

  // METADATA REQUIRED BY THE RENDERERS
  

  /**
   * Store data of some kind for a renderer that requires extra metadata of
   * some kind. Usually this is a renderer-specific representation of the
   * font data, for instance a custom OpenGL texture for PGraphicsOpenGL2.
   * @param renderer The PGraphics renderer associated to the font
   * @param storage The metadata required by the renderer    
   */
  public void setCache(PGraphics renderer, Object storage) {
    if (cacheMap == null) cacheMap = new HashMap<PGraphics, Object>();
    cacheMap.put(renderer, storage);
  }


  /**
   * Get cache storage data for the specified renderer. Because each renderer
   * will cache data in different formats, it's necessary to store cache data
   * keyed by the renderer object. Otherwise, attempting to draw the same
   * image to both a PGraphicsJava2D and a PGraphicsOpenGL2 will cause errors.
   * @param renderer The PGraphics renderer associated to the font
   * @return metadata stored for the specified renderer
   */
  public Object getCache(PGraphics renderer) {
    if (cacheMap == null) return null;
    return cacheMap.get(renderer);
  }


  /**
   * Remove information associated with this renderer from the cache, if any.
   * @param parent The PGraphics renderer whose cache data should be removed
   */
  public void removeCache(PGraphics renderer) {
    if (cacheMap != null) {
      cacheMap.remove(renderer);
    }
  }  
  
  
  //////////////////////////////////////////////////////////////  
  
  public int getGlyphCount()  {
    return glyphCount;
  }
  
  public Glyph getGlyph(int i)  {
    return glyphs[i];  
  }  
  
  //////////////////////////////////////////////////////////////

  
  static final char[] EXTRA_CHARS = {
    0x0080, 0x0081, 0x0082, 0x0083, 0x0084, 0x0085, 0x0086, 0x0087,
    0x0088, 0x0089, 0x008A, 0x008B, 0x008C, 0x008D, 0x008E, 0x008F,
    0x0090, 0x0091, 0x0092, 0x0093, 0x0094, 0x0095, 0x0096, 0x0097,
    0x0098, 0x0099, 0x009A, 0x009B, 0x009C, 0x009D, 0x009E, 0x009F,
    0x00A0, 0x00A1, 0x00A2, 0x00A3, 0x00A4, 0x00A5, 0x00A6, 0x00A7,
    0x00A8, 0x00A9, 0x00AA, 0x00AB, 0x00AC, 0x00AD, 0x00AE, 0x00AF,
    0x00B0, 0x00B1, 0x00B4, 0x00B5, 0x00B6, 0x00B7, 0x00B8, 0x00BA,
    0x00BB, 0x00BF, 0x00C0, 0x00C1, 0x00C2, 0x00C3, 0x00C4, 0x00C5,
    0x00C6, 0x00C7, 0x00C8, 0x00C9, 0x00CA, 0x00CB, 0x00CC, 0x00CD,
    0x00CE, 0x00CF, 0x00D1, 0x00D2, 0x00D3, 0x00D4, 0x00D5, 0x00D6,
    0x00D7, 0x00D8, 0x00D9, 0x00DA, 0x00DB, 0x00DC, 0x00DD, 0x00DF,
    0x00E0, 0x00E1, 0x00E2, 0x00E3, 0x00E4, 0x00E5, 0x00E6, 0x00E7,
    0x00E8, 0x00E9, 0x00EA, 0x00EB, 0x00EC, 0x00ED, 0x00EE, 0x00EF,
    0x00F1, 0x00F2, 0x00F3, 0x00F4, 0x00F5, 0x00F6, 0x00F7, 0x00F8,
    0x00F9, 0x00FA, 0x00FB, 0x00FC, 0x00FD, 0x00FF, 0x0102, 0x0103,
    0x0104, 0x0105, 0x0106, 0x0107, 0x010C, 0x010D, 0x010E, 0x010F,
    0x0110, 0x0111, 0x0118, 0x0119, 0x011A, 0x011B, 0x0131, 0x0139,
    0x013A, 0x013D, 0x013E, 0x0141, 0x0142, 0x0143, 0x0144, 0x0147,
    0x0148, 0x0150, 0x0151, 0x0152, 0x0153, 0x0154, 0x0155, 0x0158,
    0x0159, 0x015A, 0x015B, 0x015E, 0x015F, 0x0160, 0x0161, 0x0162,
    0x0163, 0x0164, 0x0165, 0x016E, 0x016F, 0x0170, 0x0171, 0x0178,
    0x0179, 0x017A, 0x017B, 0x017C, 0x017D, 0x017E, 0x0192, 0x02C6,
    0x02C7, 0x02D8, 0x02D9, 0x02DA, 0x02DB, 0x02DC, 0x02DD, 0x03A9,
    0x03C0, 0x2013, 0x2014, 0x2018, 0x2019, 0x201A, 0x201C, 0x201D,
    0x201E, 0x2020, 0x2021, 0x2022, 0x2026, 0x2030, 0x2039, 0x203A,
    0x2044, 0x20AC, 0x2122, 0x2202, 0x2206, 0x220F, 0x2211, 0x221A,
    0x221E, 0x222B, 0x2248, 0x2260, 0x2264, 0x2265, 0x25CA, 0xF8FF,
    0xFB01, 0xFB02
  };


  /**
   * The default Processing character set.
   * <P>
   * This is the union of the Mac Roman and Windows ANSI (CP1250)
   * character sets. ISO 8859-1 Latin 1 is Unicode characters 0x80 -> 0xFF,
   * and would seem a good standard, but in practice, most P5 users would
   * rather have characters that they expect from their platform's fonts.
   * <P>
   * This is more of an interim solution until a much better
   * font solution can be determined. (i.e. create fonts on
   * the fly from some sort of vector format).
   * <P>
   * Not that I expect that to happen.
   */
  static public char[] CHARSET;
  static {
    CHARSET = new char[126-33+1 + EXTRA_CHARS.length];
    int index = 0;
    for (int i = 33; i <= 126; i++) {
      CHARSET[index++] = (char)i;
    }
    for (int i = 0; i < EXTRA_CHARS.length; i++) {
      CHARSET[index++] = EXTRA_CHARS[i];
    }
  };


  static HashMap<String, Typeface> typefaceMap;
  static String[] fontList;


  /**
   * Get a list of the built-in fonts.
   */
  static public String[] list() {
    loadTypefaces();
    return fontList;
  }


  static public void loadTypefaces() {
    if (typefaceMap == null) {
      typefaceMap = new HashMap<String, Typeface>();

      typefaceMap.put("Serif",
                  Typeface.create(Typeface.SERIF, Typeface.NORMAL));
      typefaceMap.put("Serif-Bold",
                  Typeface.create(Typeface.SERIF, Typeface.BOLD));
      typefaceMap.put("Serif-Italic",
                  Typeface.create(Typeface.SERIF, Typeface.ITALIC));
      typefaceMap.put("Serif-BoldItalic",
                  Typeface.create(Typeface.SERIF, Typeface.BOLD_ITALIC));

      typefaceMap.put("SansSerif",
                  Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL));
      typefaceMap.put("SansSerif-Bold",
                  Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
      typefaceMap.put("SansSerif-Italic",
                  Typeface.create(Typeface.SANS_SERIF, Typeface.ITALIC));
      typefaceMap.put("SansSerif-BoldItalic",
                  Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD_ITALIC));

      typefaceMap.put("Monospaced",
                  Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL));
      typefaceMap.put("Monospaced-Bold",
                  Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
      typefaceMap.put("Monospaced-Italic",
                  Typeface.create(Typeface.MONOSPACE, Typeface.ITALIC));
      typefaceMap.put("Monospaced-BoldItalic",
                  Typeface.create(Typeface.MONOSPACE, Typeface.BOLD_ITALIC));

      fontList = new String[typefaceMap.size()];
      typefaceMap.keySet().toArray(fontList);
    }
  }

  
  /////////////////////////////////////////////////////////////

  /**
   * A single character, and its visage.
   */
  public class Glyph {
    public PImage image;
    public int value;
    public int height;
    public int width;
    public int index;    
    public int setWidth;
    public int topExtent;
    public int leftExtent;
    public boolean fromStream = false;


    protected Glyph() {
      // used when reading from a stream or for subclasses
    }


    protected Glyph(DataInputStream is) throws IOException {
      readHeader(is);
    }


    protected void readHeader(DataInputStream is) throws IOException {
      value = is.readInt();
      height = is.readInt();
      width = is.readInt();
      setWidth = is.readInt();
      topExtent = is.readInt();
      leftExtent = is.readInt();

      // pointer from a struct in the c version, ignored
      is.readInt();

      // the values for getAscent() and getDescent() from FontMetrics
      // seem to be way too large.. perhaps they're the max?
      // as such, use a more traditional marker for ascent/descent
      if (value == 'd') {
        if (ascent == 0) ascent = topExtent;
      }
      if (value == 'p') {
        if (descent == 0) descent = -topExtent + height;
      }
    }


    protected void writeHeader(DataOutputStream os) throws IOException {
      os.writeInt(value);
      os.writeInt(height);
      os.writeInt(width);
      os.writeInt(setWidth);
      os.writeInt(topExtent);
      os.writeInt(leftExtent);
      os.writeInt(0); // padding
    }


    protected void readBitmap(DataInputStream is) throws IOException {
      image = new PImage(width, height, ALPHA);
      int bitmapSize = width * height;

      byte[] temp = new byte[bitmapSize];
      is.readFully(temp);

      // convert the bitmap to an alpha channel
      int w = width;
      int h = height;
      int[] pixels = image.pixels;
      for (int y = 0; y < h; y++) {
        for (int x = 0; x < w; x++) {
          pixels[y * width + x] = temp[y*w + x] & 0xff;
//          System.out.print((image.pixels[y*64+x] > 128) ? "*" : ".");
        }
//        System.out.println();
      }
      fromStream = true;
//      System.out.println();
    }


    protected void writeBitmap(DataOutputStream os) throws IOException {
      int[] pixels  = image.pixels;
      for (int y = 0; y < height; y++) {
        for (int x = 0; x < width; x++) {
          os.write(pixels[y * width + x] & 0xff);
        }
      }
    }


    protected Glyph(char c) {
      int mbox3 = size * 3;
//      lazyGraphics.setColor(Color.white);
//      lazyGraphics.fillRect(0, 0, mbox3, mbox3);
      lazyCanvas.drawColor(Color.WHITE);  // fill canvas with white
//      lazyGraphics.setColor(Color.black);
      lazyPaint.setColor(Color.BLACK);
//      lazyGraphics.drawString(String.valueOf(c), size, size * 2);
      lazyCanvas.drawText(String.valueOf(c), size, size * 2, lazyPaint);

//      WritableRaster raster = lazyImage.getRaster();
//      raster.getDataElements(0, 0, mbox3, mbox3, lazySamples);
      lazyBitmap.getPixels(lazySamples, 0, mbox3, 0, 0, mbox3, mbox3);

      int minX = 1000, maxX = 0;
      int minY = 1000, maxY = 0;
      boolean pixelFound = false;

      for (int y = 0; y < mbox3; y++) {
        for (int x = 0; x < mbox3; x++) {
          int sample = lazySamples[y * mbox3 + x] & 0xff;
          if (sample != 255) {
            if (x < minX) minX = x;
            if (y < minY) minY = y;
            if (x > maxX) maxX = x;
            if (y > maxY) maxY = y;
            pixelFound = true;
          }
        }
      }

      if (!pixelFound) {
        minX = minY = 0;
        maxX = maxY = 0;
        // this will create a 1 pixel white (clear) character..
        // maybe better to set one to -1 so nothing is added?
      }

      value = c;
      height = (maxY - minY) + 1;
      width = (maxX - minX) + 1;
//      setWidth = lazyMetrics.charWidth(c);
      setWidth = (int) lazyPaint.measureText(String.valueOf(c));

      // offset from vertical location of baseline
      // of where the char was drawn (size*2)
      topExtent = size*2 - minY;

      // offset from left of where coord was drawn
      leftExtent = minX - size;

      image = new PImage(width, height, ALPHA);
      int[] pixels = image.pixels;
      for (int y = minY; y <= maxY; y++) {
        for (int x = minX; x <= maxX; x++) {
          int val = 255 - (lazySamples[y * mbox3 + x] & 0xff);
          int pindex = (y - minY) * width + (x - minX);
          pixels[pindex] = val;
        }
      }

      // replace the ascent/descent values with something.. err, decent.
      if (value == 'd') {
        if (ascent == 0) ascent = topExtent;
      }
      if (value == 'p') {
        if (descent == 0) descent = -topExtent + height;
      }
    }
  }
}
