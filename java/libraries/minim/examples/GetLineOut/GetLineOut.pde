/**
 * Get Line Out
 * by Damien Di Fede.
 *  
 * This sketch demonstrates how to use the <code>getLineOut</code> method 
 * of <code>Minim</code>. This method returns an <code>AudioOutput</code> 
 * object. An <code>AudioOutput</code> represents a connection to the 
 * computer's speakers and is used to generate audio with <code>AudioSignal</code>s. 
 * There are five versions of <code>getLineOut</code>:
 * <pre>
 * getLineOut()
 * getLineOut(int type) 
 * getLineOut(int type, int bufferSize) 
 * getLineOut(int type, int bufferSize, float sampleRate) 
 * getLineOut(int type, int bufferSize, float sampleRate, int bitDepth)  
 * </pre>
 * The value you can use for <code>type</code> is either <code>Minim.MONO</code> 
 * or <code>Minim.STEREO</code>. <code>bufferSize</code> specifies how large 
 * you want the sample buffer to be, <code>sampleRate</code> specifies what 
 * the sample rate of the audio you will be generating is, and <code>bitDepth</code> 
 * specifies what the bit depth of the audio you will be generating is (8 or 16). 
 * <code>type</code> defaults to <code>Minim.STEREO</code>, <code>bufferSize</code> 
 * defaults to 1024, <code>sampleRate</code> defaults to 44100, and 
 * <code>bitDepth</code> defaults to 16.
 *
 * Before you exit your sketch make sure you call the <code>close</code> 
 * method of any <code>AudioOutput</code>'s you have received from <code>getLineOut</code>.
 */

import ddf.minim.*;
import ddf.minim.signals.*;

Minim minim;
AudioOutput out;
SineWave sine;

void setup()
{
  size(512, 200, P2D);
  
  minim = new Minim(this);
  
  // get a line out from Minim, default sample rate is 44100, default bit depth is 16
  out = minim.getLineOut(Minim.STEREO, 2048);
  
  // create a sine wave Oscillator, set to 440 Hz, at 0.5 amplitude, sample rate 44100 to match the line out
  sine = new SineWave(440, 0.5, out.sampleRate());
  // add the oscillator to the line out
  out.addSignal(sine);
}

void draw()
{
  background(0);
  stroke(255);
  // draw the waveforms
  for(int i = 0; i < out.bufferSize() - 1; i++)
  {
    line(i, 50 + out.left.get(i)*50, i+1, 50 + out.left.get(i+1)*50);
    line(i, 150 + out.right.get(i)*50, i+1, 150 + out.right.get(i+1)*50);
  }
}


void stop()
{
  // always close Minim audio classes when you are done with them
  out.close();
  minim.stop();

  super.stop();
}
