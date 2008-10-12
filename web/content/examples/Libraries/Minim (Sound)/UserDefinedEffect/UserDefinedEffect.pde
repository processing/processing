/**
 * User Defined Effect 
 * by Damien Di Fede.
 *  
 * This sketch demonstrates how to write your own AudioEffect. 
 * See NoiseEffect.pde for the implementation.
 */

import ddf.minim.*;
import ddf.minim.effects.*;

Minim minim;
AudioPlayer groove;
ReverseEffect reffect;

void setup()
{
  size(512, 200, P3D);
  
  minim = new Minim(this);
  // try changing the buffer size to see how it changes the effect
  groove = minim.loadFile("groove.mp3", 2048);
  groove.loop();
  reffect = new ReverseEffect();
  groove.addEffect(reffect);
}

void draw()
{
  background(0);
  stroke(255);
  // we multiply the values returned by get by 50 so we can see the waveform
  for ( int i = 0; i < groove.bufferSize() - 1; i++ )
  {
    float x1 = map(i, 0, groove.bufferSize(), 0, width);
    float x2 = map(i+1, 0, groove.bufferSize(), 0, width);
    line(x1, height/4 - groove.left.get(i)*50, x2, height/4 - groove.left.get(i+1)*50);
    line(x1, 3*height/4 - groove.right.get(i)*50, x2, 3*height/4 - groove.right.get(i+1)*50);
  }
}

void stop()
{
  // always close Minim audio classes when you finish with them
  groove.close();
  // always stop Minim before exiting
  minim.stop();

  super.stop();
}
