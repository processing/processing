/*
This is a simple WhiteNoise generator. It can be started with .play(float amp).
In this example it is started and stopped by clicking into the renderer window.
*/

import processing.sound.*;

WhiteNoise noise;
HighPass highPass;

float amp=0.0;

void setup() {
    size(640,360);
    background(255);
    
    // Create the noise generator + filter
    noise = new WhiteNoise(this);
    highPass = new HighPass(this);

    noise.play(0.5);
    highPass.process(noise, 100);
}      

void draw() {
    highPass.freq(map(mouseX, 0, width, 80, 10000));
}
