/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2011-12 Ben Fry and Casey Reas

  This library is free software; you can redistribute it and/or
  modify it under the terms of the GNU Lesser General Public
  License as published by the Free Software Foundation; either
  version 2.1 of the License, or (at your option) any later version.

  This library is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
  Lesser General Public License for more details.

  You should have received a copy of the GNU Lesser General
  Public License along with this library; if not, write to the
  Free Software Foundation, Inc., 59 Temple Place, Suite 330,
  Boston, MA  02111-1307  USA
*/

package processing.opengl;

import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PFont;
import processing.core.PImage;

import java.util.HashMap;

/**
 * All the infrastructure needed for optimized font rendering
 * in OpenGL. Basically, this special class is needed because
 * fonts in Processing are handled by a separate PImage for each
 * glyph. For performance reasons, all these glyphs should be
 * stored in a single OpenGL texture (otherwise, rendering a
 * string of text would involve binding and un-binding several
 * textures.
 * PFontTexture manages the correspondence between individual
 * glyphs and the large OpenGL texture containing them. Also,
 * in the case that the font size is very large, one single
 * OpenGL texture might not be enough to store all the glyphs,
 * so PFontTexture also takes care of spreading a single font
 * over several textures.
 * @author Andres Colubri
 */
class FontTexture implements PConstants {
  protected PApplet parent;
  protected PGraphicsOpenGL pg;
  protected PGL pgl;
  protected PFont font;
  protected boolean is3D;

  protected int maxTexWidth;
  protected int maxTexHeight;
  protected int offsetX;
  protected int offsetY;
  protected int lineHeight;
  protected Texture[] textures = null;
  protected PImage[] images = null;
  protected int currentTex;
  protected int lastTex;
  protected TextureInfo[] glyphTexinfos;
  protected HashMap<PFont.Glyph, TextureInfo> texinfoMap;

  public FontTexture(PApplet parent, PFont font, int maxw, int maxh,
                      boolean is3D) {
    this.parent = parent;
    this.font = font;
    pg = (PGraphicsOpenGL)parent.g;
    pgl = pg.pgl;
    this.is3D = is3D;

    initTexture(maxw, maxh);
  }


  protected void allocate() {
    // Nothing to do here: the font textures will allocate
    // themselves.
  }


  protected void initTexture(int w, int h) {
    maxTexWidth = w;
    maxTexHeight = h;

    currentTex = -1;
    lastTex = -1;

    addTexture();

    offsetX = 0;
    offsetY = 0;
    lineHeight = 0;

    texinfoMap = new HashMap<PFont.Glyph, TextureInfo>();
    glyphTexinfos = new TextureInfo[font.getGlyphCount()];
    addAllGlyphsToTexture();
  }


  public boolean addTexture() {
    int w, h;
    boolean resize;

    w = maxTexWidth;
    if (-1 < currentTex && textures[currentTex].glHeight < maxTexHeight) {
      // The height of the current texture is less than the maximum, this
      // means we can replace it with a larger texture.
      h = PApplet.min(2 * textures[currentTex].glHeight, maxTexHeight);
      resize = true;
    } else {
      h = PApplet.min(PGraphicsOpenGL.maxTextureSize, PGL.MAX_FONT_TEX_SIZE / 2,
                                                      maxTexHeight / 4);
      resize = false;
    }

    Texture tex;
    if (is3D) {
      // Bilinear sampling ensures that the texture doesn't look pixelated
      // either when it is magnified or minified...
      tex = new Texture(parent, w, h,
                        new Texture.Parameters(ARGB, Texture.BILINEAR, false));
    } else {
      // ...however, the effect of bilinear sampling is to add some blurriness
      // to the text in its original size. In 2D, we assume that text will be
      // shown at its original size, so linear sampling is chosen instead (which
      // only affects minimized text).
      tex = new Texture(parent, w, h,
                        new Texture.Parameters(ARGB, Texture.LINEAR, false));
    }

    if (textures == null) {
      textures = new Texture[1];
      textures[0] = tex;
      images = new PImage[1];
      images[0] = pg.wrapTexture(tex);
      currentTex = 0;
    } else if (resize) {
      // Replacing old smaller texture with larger one.
      // But first we must copy the contents of the older
      // texture into the new one.
      Texture tex0 = textures[currentTex];
      tex.put(tex0);
      textures[currentTex] = tex;

      pg.setCache(images[currentTex], tex);
      images[currentTex].width = tex.width;
      images[currentTex].height = tex.height;
    } else {
      // Adding new texture to the list.
      Texture[] tempTex = textures;
      textures = new Texture[textures.length + 1];
      PApplet.arrayCopy(tempTex, textures, tempTex.length);
      textures[tempTex.length] = tex;
      currentTex = textures.length - 1;

      PImage[] tempImg = images;
      images = new PImage[textures.length];
      PApplet.arrayCopy(tempImg, images, tempImg.length);
      images[tempImg.length] = pg.wrapTexture(tex);
    }
    lastTex = currentTex;

    // Make sure that the current texture is bound.
    tex.bind();

    return resize;
  }


  public void begin() {
    setTexture(0);
  }


  public void end() {
    for (int i = 0; i < textures.length; i++) {
      pgl.disableTexturing(textures[i].glTarget);
    }
  }


  public void setTexture(int idx) {
    if (0 <= idx && idx < textures.length) {
      currentTex = idx;
    }
  }


  public PImage getTexture(int idx) {
    if (0 <= idx && idx < images.length) {
      return images[idx];
    }
    return null;
  }


  public PImage getCurrentTexture() {
    return getTexture(currentTex);
  }


  // Add all the current glyphs to opengl texture.
  public void addAllGlyphsToTexture() {
    // loop over current glyphs.
    for (int i = 0; i < font.getGlyphCount(); i++) {
      addToTexture(i, font.getGlyph(i));
    }
  }


  public void updateGlyphsTexCoords() {
    // loop over current glyphs.
    for (int i = 0; i < glyphTexinfos.length; i++) {
      TextureInfo tinfo = glyphTexinfos[i];
      if (tinfo != null && tinfo.texIndex == currentTex) {
        tinfo.updateUV();
      }
    }
  }


  public TextureInfo getTexInfo(PFont.Glyph glyph) {
    TextureInfo info = texinfoMap.get(glyph);
    return info;
  }


  public TextureInfo addToTexture(PFont.Glyph glyph) {
    int n = glyphTexinfos.length;
    if (n == 0) {
      glyphTexinfos = new TextureInfo[1];
    }
    addToTexture(n, glyph);
    return glyphTexinfos[n];
  }


  public boolean contextIsOutdated() {
    boolean outdated = false;
    for (int i = 0; i < textures.length; i++) {
      if (textures[i].contextIsOutdated())  {
        outdated = true;
      }
    }
    if (outdated) {
      for (int i = 0; i < textures.length; i++) {
        pg.removeTextureObject(textures[i].glName, textures[i].context);
        textures[i].glName = 0;
      }
    }
    return outdated;
  }

  // Adds this glyph to the opengl texture in PFont.
  protected void addToTexture(int idx, PFont.Glyph glyph) {
    // We add one pixel to avoid issues when sampling the font texture at
    // fractional screen positions. I.e.: the pixel on the screen only contains
    // half of the font rectangle, so it would sample half of the color from the
    // glyph area in the texture, and the other half from the contiguous pixel.
    // If the later contains a portion of the neighbor glyph and the former
    // doesn't, this would result in a shaded pixel when the correct output is
    // blank. This is a consequence of putting all the glyphs in a common
    // texture with bilinear sampling.
    int w = 1 + glyph.width + 1;
    int h = 1 + glyph.height + 1;

    // Converting the pixels array from the PImage into a valid RGBA array for
    // OpenGL.
    int[] rgba = new int[w * h];
    int t = 0;
    int p = 0;
    if (PGL.BIG_ENDIAN)  {
      java.util.Arrays.fill(rgba, 0, w, 0xFFFFFF00); // Set the first row to blank pixels.
      t = w;
      for (int y = 0; y < glyph.height; y++) {
        rgba[t++] = 0xFFFFFF00; // Set the leftmost pixel in this row as blank
        for (int x = 0; x < glyph.width; x++) {
          rgba[t++] = 0xFFFFFF00 | glyph.image.pixels[p++];
        }
        rgba[t++] = 0xFFFFFF00; // Set the rightmost pixel in this row as blank
      }
      java.util.Arrays.fill(rgba, (h - 1) * w, h * w, 0xFFFFFF00); // Set the last row to blank pixels.
    } else {
      java.util.Arrays.fill(rgba, 0, w, 0x00FFFFFF); // Set the first row to blank pixels.
      t = w;
      for (int y = 0; y < glyph.height; y++) {
        rgba[t++] = 0x00FFFFFF; // Set the leftmost pixel in this row as blank
        for (int x = 0; x < glyph.width; x++) {
          rgba[t++] = (glyph.image.pixels[p++] << 24) | 0x00FFFFFF;
        }
        rgba[t++] = 0x00FFFFFF; // Set the rightmost pixel in this row as blank
      }
      java.util.Arrays.fill(rgba, (h - 1) * w, h * w, 0x00FFFFFF); // Set the last row to blank pixels.
    }

    // Is there room for this glyph in the current line?
    if (offsetX + w > textures[currentTex].glWidth) {
      // No room, go to the next line:
      offsetX = 0;
      offsetY += lineHeight;
      lineHeight = 0;
    }
    lineHeight = Math.max(lineHeight, h);

    boolean resized = false;
    if (offsetY + lineHeight > textures[currentTex].glHeight) {
      // We run out of space in the current texture, so we add a new texture:
      resized = addTexture();
      if (resized) {
        // Because the current texture has been resized, we need to
        // update the UV coordinates of all the glyphs associated to it:
        updateGlyphsTexCoords();
      } else {
        // A new texture has been created. Reseting texture coordinates
        // and line.
        offsetX = 0;
        offsetY = 0;
        lineHeight = 0;
      }
    }

    TextureInfo tinfo = new TextureInfo(currentTex, offsetX, offsetY,
                                        w, h, rgba);
    offsetX += w;

    if (idx == glyphTexinfos.length) {
      TextureInfo[] temp = new TextureInfo[glyphTexinfos.length + 1];
      System.arraycopy(glyphTexinfos, 0, temp, 0, glyphTexinfos.length);
      glyphTexinfos = temp;
    }

    glyphTexinfos[idx] = tinfo;
    texinfoMap.put(glyph, tinfo);
  }


  class TextureInfo {
    int texIndex;
    int width;
    int height;
    int[] crop;
    float u0, u1;
    float v0, v1;
    int[] pixels;

    TextureInfo(int tidx, int cropX, int cropY, int cropW, int cropH,
                int[] pix) {
      texIndex = tidx;
      crop = new int[4];
      // The region of the texture corresponding to the glyph is surrounded by a
      // 1-pixel wide border to avoid artifacts due to bilinear sampling. This
      // is why the additions and subtractions to the crop values.
      crop[0] = cropX + 1;
      crop[1] = cropY + 1 + cropH - 2;
      crop[2] = cropW - 2;
      crop[3] = -cropH + 2;
      pixels = pix;
      updateUV();
      updateTex();
    }


    void updateUV() {
      width = textures[texIndex].glWidth;
      height = textures[texIndex].glHeight;
      u0 = (float)crop[0] / (float)width;
      u1 = u0 + (float)crop[2] / (float)width;
      v0 = (float)(crop[1] + crop[3]) / (float)height;
      v1 = v0 - (float)crop[3] / (float)height;
    }


    void updateTex() {
      textures[texIndex].setNative(pixels, crop[0] - 1, crop[1] + crop[3] - 1,
                                           crop[2] + 2, -crop[3] + 2);
    }
  }
}