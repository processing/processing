/*
This is a sine-wave oscillator. The method .play() starts the oscillator. There
are several setters like .amp(), .freq(), .pan() and .add(). If you want to set all of them at
the same time use .set(float freq, float amp, float add, float pan)
*/

import processing.sound.*;

Sound stream;
SinOsc sine;

float freq=400;
float amp=0.5;
float pos;

void setup() {
    size(350,350);
    background(255);
    
    // Create and start the sound renderer and the sine oscillator.
    // Second and Third arguments are optional, default is 44100 / 512 
    // for Sample Rate and Buffer Size
    stream = new Sound(this, 44100, 256);
    sine = new SinOsc(this);
    
    //Start the Sine Oscillator. 
    sine.play();
}

void draw() {

  // Map mouseY from 0.0 to 1.0 for amplitude
  amp=map(mouseY, 0, height, 1.0, 0.0);
  sine.amp(amp);
  
  // Map mouseX from 20Hz to 1000Hz for frequency  
  freq=map(mouseX, 0, width, 80.0, 1000.0);
  sine.freq(freq);
  
  // Map mouseX from -1.0 to 1.0 for left to right 
  pos=map(mouseX, 0, width, -1.0, 1.0);
  sine.pan(pos);
}
