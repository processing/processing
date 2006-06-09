package processing.video;

import processing.core.*;

import javax.microedition.lcdui.*;
import javax.microedition.media.*;
import javax.microedition.media.control.VideoControl;

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
public class Capture {
    private PMIDlet         midlet;
    private Player          player;
    private VideoControl    control;
    
    public int sourceWidth;
    public int sourceHeight;
    
    public int viewX;
    public int viewY;
    public int viewWidth;
    public int viewHeight; 
    
    public Capture(PMIDlet midlet) {
        this.midlet = midlet;
        init("capture://video");
    }
    
    public Capture(PMIDlet midlet, String locator) {
        this.midlet = midlet;
        init(locator);
    }
    
    protected void init(String locator) {
        try {
            player = Manager.createPlayer(locator);
            player.realize();
            
            control = (VideoControl) player.getControl("VideoControl");
            control.initDisplayMode(VideoControl.USE_DIRECT_VIDEO, midlet.getCanvas());
            
            sourceWidth = control.getSourceWidth();
            sourceHeight = control.getSourceHeight();
            
            viewX = control.getDisplayX();
            viewY = control.getDisplayY();
            viewWidth = control.getDisplayWidth();
            viewHeight = control.getDisplayHeight();
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }
    
    public boolean show() {
        boolean result = false;
        try {
            player.start();
            result = true;
        } catch (MediaException me) { }
        if (result) {
            control.setVisible(true);
        }
        return result;
    }
    
    public boolean show(int x, int y) {
        //// try to show
        boolean result = show();
        //// try to set coordinates
        control.setDisplayLocation(x, y);
        //// get actual display coordinates, in case they were ignored
        viewX = control.getDisplayX();
        viewY = control.getDisplayY();
        //// return result
        return result;
    }
    
    public boolean show(int x, int y, int width, int height) {
        //// try to show
        boolean result = show(x, y);
        //// try to set width/height
        try {
            control.setDisplaySize(width, height);
        } catch (MediaException me) {
            //// ignore, allow to continue without resizing
        }
        //// get actual width height, in case implementation changed them
        viewWidth = control.getDisplayWidth();
        viewHeight = control.getDisplayHeight();
        //// return result
        return result;
    }
    
    public void hide() {
        try {
            player.stop();
        } catch (MediaException me) { }
        control.setVisible(false);        
    }
    
    public void close() {
        try {
            player.stop();
            player.close();
        } catch (MediaException me) { }
        control.setVisible(false);
    }
    
    public byte[] read(String encoding) {
        try {
            return control.getSnapshot(encoding);
        } catch (MediaException me) {
            throw new RuntimeException(me.getMessage());
        }
    }
    
    public byte[] read() {
        return read(null);
    }
    
    public byte[] read(int width, int height) {
        return read("width=" + width + "&height=" + height);
    }
        
    public byte[] read(String encoding, int width, int height) {
        return read("encoding=" + encoding + "&width=" + width + "&height=" + height);
    }    
}
