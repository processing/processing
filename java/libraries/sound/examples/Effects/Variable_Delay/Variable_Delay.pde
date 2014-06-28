/*
This is a sound file player. 
*/

import processing.sound.*;

Sound stream;
SoundFile soundfile;
Delay delay;

void setup() {
    size(350,350);
    background(255);
    
    // Create and start the sound renderer
    stream = new Sound(this);
    
    //Load a soundfile
    soundfile = new SoundFile(this, "vibraphon.aiff");
    
    // create a Delay Effect
    delay = new Delay(this);
    
    // These methods return useful infos about the file
    println("SFSampleRate= " + soundfile.sampleRate() + " Hz");
    println("SFSamples= " + soundfile.frames() + " samples");
    println("SFDuration= " + soundfile.duration() + " seconds");

    // Play the file in a loop
    soundfile.loop();
    
    // Patch the delay
    delay.play(soundfile, 5);
}      


void draw() {
  // Map mouseX from 0.25 to 4.0 for playback rate. 1 equals original playback 
  // speed 2 is an octave up 0.5 is an octave down.
  soundfile.rate(map(mouseX, 0, 350, 0.25, 4.0)); 
  
  // Map mouseY from 0.2 to 1.0 for amplitude  
  soundfile.amp(map(mouseY, 0, 350, 0.2, 1.0)); 
 
  // Map mouseY from -1.0 to 1.0 for left to right 
  soundfile.pan(map(mouseY, 0, 350, -1.0, 1.0));  
  
  // Map mouseY from 0.001 to 2.0 seconds for the delaytime 
  delay.time(map(mouseY, 0, 350, 0.001, 2.0));
  
  // Map mouseX from 0 to 0.8 for the delay feedback 
  delay.feedback(map(mouseX, 0, 350, 0.0, 0.8));
}
