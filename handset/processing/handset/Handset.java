package processing.handset;

import javax.microedition.midlet.MIDlet;
import javax.microedition.lcdui.Display;

/**
 *
 * @author Francis Li
 */
public class Handset {
    private MIDlet      midlet;
    private Display     display;
    
    public Handset(MIDlet midlet) {
        this.midlet = midlet;
        this.display = Display.getDisplay(midlet);
    }
    
    public boolean vibrate(int duration) {
        return display.vibrate(duration);
    }
    
    public boolean flash(int duration) {
        return display.flashBacklight(duration);
    }
}
