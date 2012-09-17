/**
  * This sketch demonstrates how to create a simple synthesis chain that 
  * involves controlling the value of a UGenInput with the output of 
  * a UGen. In this case, we patch an Oscil generating a sine wave into 
  * the amplitude input of an Oscil generating a square wave. The result 
  * is known as amplitude modulation.
  */

import ddf.minim.*;
import ddf.minim.ugens.*;

Minim minim;
AudioOutput out;
Oscil       wave;
Oscil       mod;

void setup()
{
  size(512, 200, P3D);
  
  minim = new Minim(this);
  
  // use the getLineOut method of the Minim object to get an AudioOutput object
  out = minim.getLineOut();
  
  // create a triangle wave Oscil, set to 440 Hz, at 1.0 amplitude
  // in this case, the amplitude we construct the Oscil with 
  // doesn't matter because we will be patching something to
  // its amplitude input.
  wave = new Oscil( 440, 1.0f, Waves.TRIANGLE );
 
  // create a sine wave Oscil for modulating the amplitude of wave
  mod  = new Oscil( 2, 0.4f, Waves.SINE );
 
  // connect up the modulator
  mod.patch( wave.amplitude );
  
  // patch wave to the output
  wave.patch( out );
}

void draw()
{
  background(0);
  stroke(255);
  
  // draw the waveforms
  for(int i = 0; i < out.bufferSize() - 1; i++)
  {
    line( i, 50 + out.left.get(i)*50, i+1, 50 + out.left.get(i+1)*50 );
    line( i, 150 + out.right.get(i)*50, i+1, 150 + out.right.get(i+1)*50 );
  }
}
