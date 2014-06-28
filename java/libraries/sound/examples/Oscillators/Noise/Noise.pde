/*
This is a simple WhiteNoise generator. It can be started with .play(float amp).
In this example it is started and stopped by clicking into the renderer window.
*/

import processing.sound.*;

Sound stream;
WhiteNoise noise;

float amp=0.0;

void setup() {
    size(350,350);
    background(255);
    
    // Create and start the sound renderer and the noise generator
    stream = new Sound(this);
    noise = new WhiteNoise(this);
    noise.play();
}      

void draw() {
    // Map mouseX from 0.0 to 1.0 for amplitude
    noise.amp(map(mouseX, 0, 350, 0.0, 1.0));
    
    // Map mouseY from -1.0 to 1.0 for left to right
    noise.pan(map(mouseY, 0, 350, -1.0, 1.0));
}
