/*
This is a simple pink noise generator. It can be started with .play(float amp).
In this example it is started and stopped by clicking into the renderer window.
*/

import processing.sound.*;

PinkNoise noise;

float amp=0.0;

void setup() {
    size(640, 360);
    background(255);
    
    // Create and start noise generator
    noise = new PinkNoise(this);
    noise.play();
}      

void draw() {
    // Map mouseX from 0.0 to 1.0 for amplitude
    noise.amp(map(mouseX, 0, width, 0.0, 1.0));
    
    // Map mouseY from -1.0 to 1.0 for left to right
    noise.pan(map(mouseY, 0, width, -1.0, 1.0));
}
