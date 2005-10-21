package processing.sound;

import javax.microedition.media.*;
import javax.microedition.media.control.*;

/** An MMAPI/MIDP2 sound implementation.
 *
 * @author Francis Li
 */
public class PSound implements PlayerListener {
    private final Player    player;
    
    public PSound(String file, String type) {
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
    
    public PSound(String file) {
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
}
