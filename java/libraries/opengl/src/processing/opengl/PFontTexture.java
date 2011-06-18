/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2004-08 Ben Fry and Casey Reas

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

import java.util.HashMap;
import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PFont;

/**
 * All the infrastructure needed for optimized font rendering 
 * in OpenGL.
 * By Andres Colubri
 * 
 */
class PFontTexture implements PConstants {
  protected PApplet parent;
  protected PFont font;

  protected int maxTexWidth;
  protected int maxTexHeight;
  protected int offsetX;
  protected int offsetY;
  protected int lineHeight;
  protected PTexture[] textures = null;
  protected int currentTex;
  protected int lastTex; 
  protected TextureInfo[] glyphTexinfos; 
  protected HashMap<PFont.Glyph, TextureInfo> texinfoMap;  
  
  public PFontTexture(PApplet parent, PFont font, int maxw, int maxh) {
    this.parent = parent;
    this.font = font;
    
    initTexture(maxw, maxh);
  }    
  
  
  public void delete() {
    for (int i = 0; i < textures.length; i++) {
      textures[i].delete();
    }
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
      h = PApplet.min(PGraphicsOpenGL2.maxTextureSize, 512, maxTexHeight / 4);
      resize = false;
    }
    
    PTexture.Parameters par = new PTexture.Parameters(ARGB, BILINEAR);
    par.wrapU = CLAMP;
    par.wrapV = CLAMP;
    PTexture tex = new PTexture(parent, w, h, par);

    if (textures == null) {
      textures = new PTexture[1];
      textures[0] = tex;
      currentTex = 0;     
    } else if (resize) {
      // Replacing old smaller texture with larger one.
      // But first we must copy the contents of the older
      // texture into the new one.
      PTexture tex0 = textures[currentTex];
      tex.put(tex0);
      textures[currentTex] = tex;
      tex0.delete();
    } else {
      // Adding new texture to the list.
      PTexture[] temp = textures;
      textures = new PTexture[textures.length + 1];
      PApplet.arrayCopy(temp, textures, temp.length);
      textures[temp.length] = tex;
      currentTex = textures.length - 1;
    }
    lastTex = currentTex;
    
    // Make sure that the current texture is bound.
    tex.bind();
    
    return resize;
  }
  
  
  public void setFirstTexture() {
    setTexture(0);
  }
  
  
  public void setTexture(int idx) {
    if (0 <= idx && idx < textures.length) {      
      currentTex = idx;
      textures[currentTex].bind();
    }
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
    for (int i = 0; i < font.getGlyphCount(); i++) {
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
  

  // Adds this glyph to the opengl texture in PFont.
  protected void addToTexture(int idx, PFont.Glyph glyph) {
    // We add one pixel to avoid issues when sampling the font texture at fractional
    // screen positions. I.e.: the pixel on the screen only contains half of the 
    // font rectangle, so it would sample half of the color from the glyph 
    // area in the texture, and the other half from the contiguous pixel. If the
    // later contains a portion of the neighbor glyph and former doesn't, this
    // would result in a shaded pixel when the correct output is blank.
    // This is a consequence of putting all the glyphs in a common texture with
    // bilinear sampling.
    int w = 1 + glyph.width + 1;
    int h = 1 + glyph.height + 1;
      
    // Converting the pixels array from the PImage into a valid RGBA array for OpenGL.    
    int[] rgba = new int[w * h];
    int t = 0;
    int p = 0;
    if (PGraphicsOpenGL2.BIG_ENDIAN)  {            
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
    
    if (lastTex == -1) { 
      lastTex = 0;
    }
    
    // We assume GL_TEXTURE_2D is enabled at this point.
    // We reset texture when it was resized because even the
    // texture index didn't change, the texture is a new one
    // in fact, so we need to rebind.
    if (currentTex != lastTex || resized) {
      setTexture(lastTex);
    }
    
    textures[currentTex].setTexels(offsetX, offsetY, w, h, rgba);
    
    TextureInfo tinfo = new TextureInfo(currentTex, offsetX, offsetY, w, h);
    offsetX += w;
 
    if (idx == glyphTexinfos.length) {
      TextureInfo[] temp = new TextureInfo[glyphTexinfos.length + 1];
      System.arraycopy(glyphTexinfos, 0, temp, 0, glyphTexinfos.length);
      glyphTexinfos = temp;
    }
    
    glyphTexinfos[idx] = tinfo;    
    texinfoMap.put(glyph, tinfo);
  }

  
  public class TextureInfo {
    public int texIndex;
    public int width;
    public int height;    
    public int[] crop;
    public float u0, u1;
    public float v0, v1;

    public TextureInfo(int tidx, int cropX, int cropY, int cropW, int cropH) {
      texIndex = tidx;      
      crop = new int[4];
      // The region of the texture corresponding to the glyph is surrounded by a 
      // 1-pixel wide border to avoid artifacts due to bilinear sampling. This is
      // why the additions and subtractions to the crop values. 
      crop[0] = cropX + 1;
      crop[1] = cropY + 1 + cropH - 2;
      crop[2] = cropW - 2;
      crop[3] = -cropH + 2;
      updateUV();
    }

    void updateUV() {
      width = textures[texIndex].glWidth;
      height = textures[texIndex].glHeight;      
      u0 = (float)crop[0] / (float)width;
      u1 = u0 + (float)crop[2] / (float)width;
      v0 = (float)(crop[1] + crop[3]) / (float)height;
      v1 = v0 - (float)crop[3] / (float)height;  
    }
  }
}