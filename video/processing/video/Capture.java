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
    
    public Capture(PMIDlet midlet, int width, int height) {
        this.midlet = midlet;
        try {
            player = Manager.createPlayer("capture://video?width=" + width + "&height=" + height);
            player.realize();
            
            control = (VideoControl) player.getControl("VideoControl");
            control.initDisplayMode(VideoControl.USE_DIRECT_VIDEO, midlet.getCanvas());
            
            player.start();
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }
    
    public void show(int x, int y, int width, int height) {
        try {
            control.setDisplaySize(width, height);
            control.setDisplayLocation(x, y);
            control.setVisible(true);
        } catch (MediaException me) {
            throw new RuntimeException(me.getMessage());
        }
    }
    
    public void hide() {
        control.setVisible(false);
    }
    
    public void close() {
        control.setVisible(false);
        player.close();
    }
    
    public byte[] read() {
        try {
            return control.getSnapshot(null);
        } catch (MediaException me) {
            throw new RuntimeException(me.getMessage());
        }
    }
}
