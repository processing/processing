/*
This is a pulse-wave oscillator. The method .play() starts the oscillator. 
There are several setters like .amp(), .freq(), .width(), .pan() and .add(). 
If you want to set all of them at the same time use 
.set(float freq, float width, float amp, float add, float pan)
*/

import processing.sound.*;

Pulse pulse;

void setup() {
    size(640,360);
    background(255);
    
    // Create and start the pulse wave oscillator
    pulse = new Pulse(this);
    pulse.play();
}

void draw() {
    // Map mouseX from 20Hz to 500Hz for frequency  
    pulse.freq(map(mouseX, 0, width, 20.0, 500.0));
    // Map mouseX from 0.0 to 0.5 for amplitude
    pulse.pan(map(mouseX, 0, width, -1.0, 1.0));
    // Map mouseY from 0.0 to 0.5 for amplitude
    pulse.amp(map(mouseY, 0, height, 0.0, 0.5));
    // Map mouseY from 0.0 to 0.5 for amplitude
    pulse.width(map(mouseY, 0, height, 0.0, 1.0));
}
