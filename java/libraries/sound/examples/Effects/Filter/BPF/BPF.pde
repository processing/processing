/*
This is a simple WhiteNoise generator. It can be started with .play(float amp).
In this example it is started and stopped by clicking into the renderer window.
*/

import processing.sound.*;

WhiteNoise noise;
BandPass bandPass;

float amp=0.0;

void setup() {
    size(640,360);
    background(255);
    
    // Create the noise generator + Filter
    noise = new WhiteNoise(this);
    bandPass = new BandPass(this);
    
    noise.play(0.5);
    bandPass.process(noise, 100);
}      

void draw() {

    bandPass.freq(map(mouseX, 0, width, 20, 10000));
    
    bandPass.bw(map(mouseY, 0, height, 100, 1000));
}
