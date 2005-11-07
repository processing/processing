package processing.sound;

import javax.microedition.media.*;
import javax.microedition.media.control.*;

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
public class Sound implements PlayerListener {
    private final Player    player;
    
    public Sound(String file, String type) {
        try {
            String[] protocols = Manager.getSupportedProtocols(null);
            boolean specified = false;
            for (int i = protocols.length - 1; i >= 0; i--) {
                if (file.startsWith(protocols[i] + "://")) {
                    specified = true;
                    break;
                }
            }
            if (!specified) {
                if (type == null) {
                    String lower = file.toLowerCase();
                    if (lower.endsWith(".mid")) {
                        type = "audio/midi";
                    } else if (lower.endsWith(".wav")) {
                        type = "audio/x-wav";
                    }
                }
                if (!file.startsWith("/")) {
                    file = "/" + file;
                }
                player = Manager.createPlayer(getClass().getResourceAsStream(file), type);
            } else {
                player = Manager.createPlayer(file);
            }
            player.addPlayerListener(this);
            player.realize();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }
    
    public Sound(String file) {
        this(file, null);
    }
    
    public void play() {
        try {
            player.setLoopCount(1);
            player.start();
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }
    
    public void loop() {
        try {
            player.setLoopCount(-1);
            player.start();
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }
    
    public void pause() {
        try {
            player.stop();
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }
    
    public void stop() {
        try {
            player.stop();
            if (player.getState() == Player.PREFETCHED) {
                player.deallocate();
            }
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }        
    }
    
    public void volume(int level) {
        try {
            Control[] controls = player.getControls();
            for (int i = controls.length - 1; i >= 0; i--) {
                if (controls[i] instanceof VolumeControl) {
                    VolumeControl vc = (VolumeControl) controls[i];
                    vc.setLevel(level);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }        
    }
    
    public int duration() {
        return (int) player.getDuration();
    }
    
    public int time() {
        return (int) player.getMediaTime();
    }
    
    public void playerUpdate(Player player, String event, Object eventData) {
        if (this.player == player) {
            
        }
    }
    
    public static void playTone(int note, int duration, int volume) {
        try {
            Manager.playTone(note, duration, volume);
        } catch (MediaException me) {
            throw new RuntimeException(me.getMessage());
        }
    }
    
    public static String[] supportedTypes() {
        return Manager.getSupportedContentTypes(null);
    }
}
