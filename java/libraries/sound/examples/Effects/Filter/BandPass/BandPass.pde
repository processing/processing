/*
This is a simple WhiteNoise generator. It can be started with .play(float amp).
In this example it is started and stopped by clicking into the renderer window.
*/

import processing.sound.*;

Sound stream;
WhiteNoise noise;
BPF bandPass;

float amp=0.0;

void setup() {
    size(350,350);
    background(255);
    
    // Create and start the sound renderer and the noise generator
    stream = new Sound(this);
    noise = new WhiteNoise(this);
    bandPass = new BPF(this);
    noise.play(0.5);
    bandPass.play(noise, 100);
}      

void draw() {

    bandPass.freq(map(mouseX, 0, 350, 20, 10000));
    
    bandPass.res(map(mouseY, 0, 350, 0.05, 1.0));
}
