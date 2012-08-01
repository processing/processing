/**
 * Sound is generated in real time by summing together harmonically related
 * sine tones. Overall pitch and harmonic detuning is controlled by the mouse.
 * Based on the Spooky Stream Save Ess example
 */

import krister.Ess.*;

int numSines = 5; // Number of oscillators to use
AudioStream myStream; // Audio stream to write into
SineWave[] myWave; // Array of sines
FadeOut myFadeOut; // Amplitude ramp function
FadeIn myFadeIn; // Amplitude ramp function

void setup() {
  size(256, 200);
  Ess.start(this); // Start Ess
  myStream = new AudioStream(); // Create a new AudioStream
  myStream.smoothPan = true;
  myWave = new SineWave[numSines]; // Initialize the oscillators
  for (int i = 0; i < myWave.length; i++) {
    float sinVolume = (1.0 / myWave.length) / (i + 1);
    myWave[i] = new SineWave(0, sinVolume);
  }
  myFadeOut = new FadeOut(); // Create amplitude ramp
  myFadeIn = new FadeIn(); // Create amplitude ramp
  myStream.start(); // Start audio
}

void draw() {
  noStroke();
  fill(0, 20);
  rect(0, 0, width, height); // Draw the background
  float offset = millis() - myStream.bufferStartTime;
  int interp = int((offset / myStream.duration) * myStream.size);
  stroke(255);
  for (int i = 0; i < width; i++) {
    float y1 = mouseY;
    float y2 = y1;
    if (i + interp + 1 < myStream.buffer2.length) {
      y1 -= myStream.buffer2[i+interp] * height / 2;
      y2 -= myStream.buffer2[i+interp+1] * height / 2;
    }
    line(i, y1, i + 1, y2); // Draw the waves
  }
}
void audioStreamWrite(AudioStream s) {
  // Figure out frequencies and detune amounts from the mouse
  // using exponential scaling to approximate pitch perception
  float yoffset = (height - mouseY) / float(height);
  float frequency = pow(1000, yoffset) + 150;
  float detune = float(mouseX) / width - 0.5;
  myWave[0].generate(myStream); // Generate first sine, replace Stream
  myWave[0].phase += myStream.size; // Increment the phase
  myWave[0].phase %= myStream.sampleRate;
  for (int i = 1; i < myWave.length; i++) { // Add remaining sines into the Stream
    myWave[i].generate(myStream, Ess.ADD);
    myWave[i].phase = myWave[0].phase;
  }
  myFadeOut.filter(myStream); // Fade down the audio
  for (int i = 0; i < myWave.length; i++) { // Set the frequencies
    myWave[i].frequency = round(frequency * (i + 1 + i * detune));
    myWave[i].phase = 0;
  }
  myFadeIn.filter(myStream); // Fade up the audio
}
