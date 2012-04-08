/**
Analyzes a sound file using a Fast Fourier Transform, and plots both the current
spectral frame and a "peak-hold" plot of the maximum over time using logarithmic
scaling. Based on examples by Krister Olsson <http://tree-axis.com>
*/
import krister.Ess.*;

AudioChannel myChannel;
FFT myFFT;
int bands = 256; // Number of FFT frequency bands to calculate

void setup() {
  size(1024, 200);
  Ess.start(this); // Start Ess
// Load "test.aif" into a new AudioChannel, file must be in the "data" folder
  myChannel = new AudioChannel("test.aif");
  myChannel.play(Ess.FOREVER);
  myFFT = new FFT(bands * 2); // We want 256 frequency bands, so we pass in 512
}

void draw() {
  background(176);
// Get spectrum
  myFFT.getSpectrum(myChannel);
// Draw FFT data
  stroke(255);
  for (int i = 0; i < bands; i++) {
    float x = width - pow(1024, (255.0 - i) / bands);
    float maxY = max(0, myFFT.maxSpectrum[i] * height * 2);
    float freY = max(0, myFFT.spectrum[i] * height * 2);
// Draw maximum lines
    stroke(255);
    line(x, height, x, height - maxY);
// Draw frequency lines
    stroke(0);
    line(x, height, x, height - freY);
  }
}

public void stop() {
  Ess.stop(); // When program stops, stop Ess too
  super.stop();
}
