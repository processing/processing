/*
Be Careful with your speaker volume, you might produce a painful 
feedback. We recommend to wear headphones for this example.

*/

import processing.sound.*;

AudioIn input;
Amplitude rms;

int scale=1;

void setup() {
    size(640,360);
    background(255);
        
    //Create an Audio input and grab the 1st channel
    input = new AudioIn(this, 0);
    
    // start the Audio Input
    input.play();
    
    // create a new Amplitude analyzer
    rms = new Amplitude(this);
    
    // Patch the input to an volume analyzer
    rms.input(input);
}      


void draw() {
    background(125,255,125);
    
    // adjust the volume of the audio input
    input.amp(map(mouseY, 0, height, 0.0, 1.0));
    
    // rms.analyze() return a value between 0 and 1. To adjust
    // the scaling and mapping of an ellipse we scale from 0 to 0.5
    scale=int(map(rms.analyze(), 0, 0.5, 1, 350));
    noStroke();
    
    fill(255,0,150);
    // We draw an ellispe coupled to the audio analysis
    ellipse(width/2, height/2, 1*scale, 1*scale);
}
