/*
This is a simple WhiteNoise generator. It can be started with .play(float amp).
In this example it is started and stopped by clicking into the renderer window.
*/

import processing.sound.*;

WhiteNoise noise;
LowPass lowPass;

float amp=0.0;

void setup() {
    size(640,360);
    background(255);
    
    // Create the noise generator + filter
    noise = new WhiteNoise(this);
    lowPass = new LowPass(this);
    noise.play(0.2);
    lowPass.process(noise, 800);
}      

void draw() {
    lowPass.freq(map(mouseX, 0, width, 80, 10000));
}
