/*
This is a simple WhiteNoise generator. It can be started with .play(float amp).
In this example it is started and stopped by clicking into the renderer window.
*/

import processing.sound.*;

WhiteNoise noise;
BandPass bPass;

float amp=0.0;

void setup() {
    size(640,360);
    background(255);
    
    // Create the noise generator + Filter
    noise = new WhiteNoise(this);
    bPass = new BandPass(this);
    noise.play(0.5);
    bPass.process(noise, 100);
}      

void draw() {

    bPass.freq(map(mouseX, 0, 350, 20, 10000));
    
    bPass.res(map(mouseY, 0, 350, 0.05, 1.0));
}
