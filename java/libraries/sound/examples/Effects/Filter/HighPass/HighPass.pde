/*
This is a simple WhiteNoise generator. It can be started with .play(float amp).
In this example it is started and stopped by clicking into the renderer window.
*/

import processing.sound.*;

Sound stream;
WhiteNoise noise;
HPF highPass;

float amp=0.0;

void setup() {
    size(640,360);
    background(255);
    
    // Create and start the sound renderer and the noise generator
    stream = new Sound(this);
    noise = new WhiteNoise(this);
    highPass = new HPF(this);
    noise.play(0.5);
    highPass.process(noise, 100);
}      

void draw() {
  
    highPass.freq(map(mouseX, 0, 350, 20, 10000));
}
