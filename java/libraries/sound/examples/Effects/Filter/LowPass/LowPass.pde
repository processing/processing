/*
This is a simple WhiteNoise generator. It can be started with .play(float amp).
In this example it is started and stopped by clicking into the renderer window.
*/

import processing.sound.*;

Sound stream;
WhiteNoise noise;
LPF lowPass;


float amp=0.0;

void setup() {
    size(350,350);
    background(255);
    
    // Create and start the sound renderer and the noise generator
    stream = new Sound(this);
    noise = new WhiteNoise(this);
    lowPass = new LPF(this);
    noise.play(0.5);
    lowPass.play(noise, 100);
}      

void draw() {
  
    lowPass.freq(map(mouseX, 0, 350, 20, 10000));
}
