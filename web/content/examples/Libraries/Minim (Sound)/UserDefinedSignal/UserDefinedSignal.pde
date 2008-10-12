/**
 * User Defined Signal
 * by Damien Di Fede.
 *  
 * This sketch demonstrates how to implement your own AudioSignal 
 * for Minim. See MouseSaw.pde for the implementation.
 */

import ddf.minim.*;
import ddf.minim.signals.*;

Minim minim;
AudioOutput out;
MouseSaw msaw;

void setup()
{
  size(512, 200, P3D);
  
  minim = new Minim(this);
  
  out = minim.getLineOut(Minim.STEREO, 2048);
  msaw = new MouseSaw();
  // adds the signal to the output
  out.addSignal(msaw);
}

void draw()
{
  background(0);
  stroke(255);
  // draw the waveforms
  for(int i = 0; i < out.bufferSize()-1; i++)
  {
    float x1 = map(i, 0, out.bufferSize(), 0, width);
    float x2 = map(i+1, 0, out.bufferSize(), 0, width);
    line(x1, 50 + out.left.get(i)*50, x2, 50 + out.left.get(i+1)*50);
    line(x1, 150 + out.right.get(i)*50, x2, 150 + out.right.get(i+1)*50);
  }
}

void stop()
{
  out.close();
  minim.stop();
  
  super.stop();
}
