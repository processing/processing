/*
This is a sound file player. 
*/

import processing.sound.*;

SoundFile soundfile;
Reverb reverb;
 

void setup() {
    size(640,360);
    background(255);
        
    //Load a soundfile
    soundfile = new SoundFile(this, "vibraphon.aiff");
    
    // create a Delay Effect
    reverb = new Reverb(this);
   
    // Play the file in a loop
    soundfile.loop();
  
    // Set soundfile as input to the reverb 
    reverb.process(soundfile);
}      


void draw() {
  
    // change the roomsize of the reverb
    reverb.room(map(mouseX, 0, width, 0, 1.0));
    
    // change the high frequency dampening parameter
    reverb.damp(map(mouseX, 0, width, 0, 1.0));
    
    // change the wet/dry relation of the effect
    reverb.wet(map(mouseY, 0, height, 0, 1.0));
    
}
