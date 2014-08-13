/*
This example shows how to create a cluster of sine oscillators, change the frequency and detune them
depending on the position of the mouse in the renderer window. The Y position determines the basic
frequency of the oscillator and X the detuning of the oscillator. The basic frequncy ranges between
150 and 1150 Hz.
*/

import processing.sound.*;

SinOsc[] sineWaves; 

// The number of oscillators
int numSines = 5; 

// A float for calculating the amplitudes
float[] sineVolume;

void setup() {
  size(500, 500);
  background(255);

  // Create the oscillators and amplitudes
  sineWaves = new SinOsc[numSines];
  sineVolume = new float[numSines]; 

  for (int i = 0; i < numSines; i++) {
    
    // The overall amplitude shouldn't exceed 1.0 which is prevented by 1.0/numSines.
    // The ascending waves will get lower in volume the higher the frequency
    sineVolume[i] = (1.0 / numSines) / (i + 1);
    
    // Create the Sine Oscillators and start them
    sineWaves[i] = new SinOsc(this);
    sineWaves[i].play();
  }
}

void draw() {
  noStroke();

  // Map mouseY to get values from 0.0 to 1.0
  float yoffset = (height - mouseY) / float(height);
  
  // Map that value logarithmically to 150 - 1150 Hz
  float frequency = pow(1000, yoffset) + 150;
  
  // Map mouseX from -0.5 to 0.5 to get a multiplier for detuning the oscillators
  float detune = float(mouseX) / width - 0.5;
  
  // Set the frequencies, detuning and volume
  for (int i = 0; i < numSines; i++) { 
    sineWaves[i].freq(frequency * (i + 1 + i * detune));
    sineWaves[i].amp(sineVolume[i]);

  }
}
