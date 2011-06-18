/**
 * Audio. 
 * Audio playback using the GSPlayer object.
 * By Ryan Kelln
 * 
 * Move the cursor across the screen to change volume. 
 */
 
import codeanticode.gsvideo.*;

GSPlayer sample;

void setup() {
    size(100, 100);
    // The last parameter is used to indicate the stream type:
    // VIDEO (default), AUDIO or DATA.
    sample = new GSPlayer(this, "groove.mp3", GSVideo.AUDIO);
    sample.loop();
}

void draw()
{
    //sample.jump(float(mouseY) / height * sample.duration());
        
    sample.volume(float(mouseX) / width);
}
