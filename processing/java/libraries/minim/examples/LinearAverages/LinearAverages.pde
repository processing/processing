/**
 * Linear Averages
 * by Damien Di Fede.
 *  
 * This sketch demonstrates how to use the averaging abilities of the FFT.
 * 128 linearly spaced averages are requested and then those are drawn as rectangles.
 */

import ddf.minim.analysis.*;
import ddf.minim.*;

Minim minim;
AudioPlayer jingle;
FFT fft;

void setup()
{
  size(512, 200, P2D);
  minim = new Minim(this);
  
  jingle = minim.loadFile("jingle.mp3", 2048);
  // loop the file
  jingle.loop();
  // create an FFT object that has a time-domain buffer the same size as jingle's sample buffer
  // and a sample rate that is the same as jingle's
  // note that this needs to be a power of two 
  // and that it means the size of the spectrum will be 1024. 
  // see the online tutorial for more info.
  fft = new FFT(jingle.bufferSize(), jingle.sampleRate());
  // use 128 averages.
  // the maximum number of averages we could ask for is half the spectrum size. 
  fft.linAverages(128);
  rectMode(CORNERS);
}

void draw()
{
  background(0);
  fill(255);
  // perform a forward FFT on the samples in jingle's mix buffer
  // note that if jingle were a MONO file, this would be the same as using jingle.left or jingle.right
  fft.forward(jingle.mix);
  int w = int(fft.specSize()/128);
  for(int i = 0; i < fft.avgSize(); i++)
  {
    // draw a rectangle for each average, multiply the value by 5 so we can see it better
    rect(i*w, height, i*w + w, height - fft.getAvg(i)*5);
  }
}

void stop()
{
  // always close Minim audio classes when you finish with them
  jingle.close();
  minim.stop();
  
  super.stop();
}
