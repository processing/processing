package processing.phone;

import processing.core.*;

import javax.microedition.midlet.MIDlet;
import javax.microedition.lcdui.Display;

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
public class Phone {
    private PMIDlet     midlet;
    private PCanvas     canvas;
    private Display     display;
    
    public Phone(PMIDlet midlet) {
        this.midlet = midlet;
        this.canvas = midlet.canvas;
        this.display = midlet.display;
    }
    
    public void fullscreen() {
        canvas.removeCommand(midlet.cmdExit);
        canvas.removeCommand(midlet.cmdCustom);
        canvas.setCommandListener(null);
        
        canvas.setFullScreenMode(true);
        midlet.width = canvas.getWidth();
        midlet.height = canvas.getHeight();
    }      
    
    public void noFullscreen() {
        canvas.setFullScreenMode(false);
        midlet.width = canvas.getWidth();
        midlet.height = canvas.getHeight();
        
        canvas.addCommand(midlet.cmdExit);
        canvas.addCommand(midlet.cmdCustom);
        canvas.setCommandListener(midlet);
    }
    
    public boolean vibrate(int duration) {
        return display.vibrate(duration);
    }
    
    public boolean flash(int duration) {
        return display.flashBacklight(duration);
    }
    
    public boolean call(String number) {
        return launch("tel:" + number);
    }
    
    public boolean launch(String url) {
        try {
            return midlet.platformRequest(url);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }
}
