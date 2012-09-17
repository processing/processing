/**
  * This sketch demonstrates how to use the loadFileIntoBuffer method of the Minim class and is also a good 
  * reference for some of the methods of the MultiChannelBuffer class. When the sketch begins it loads 
  * a file from the data folder into a MultiChannelBuffer and then modifies that sample data before 
  * using it to create a Sampler UGen. You can hear the result of this modification by hitting 
  * the space bar.
  */

import ddf.minim.*;
import ddf.minim.ugens.*;

Minim              minim;
MultiChannelBuffer sampleBuffer;

AudioOutput        output;
Sampler            sampler;

void setup()
{
  size(512, 200, P3D);
  
  // create Minim and an AudioOutput
  minim  = new Minim(this);
  output = minim.getLineOut();
  
  // construct a new MultiChannelBuffer with 2 channels and 1024 sample frames.
  // in our particular case, it doesn't really matter what we choose for these
  // two values because loadFileIntoBuffer will reconfigure the buffer 
  // to match the channel count and length of the file.
  sampleBuffer     = new MultiChannelBuffer( 1, 1024 );
  
  // we pass the buffer to the method and Minim will reconfigure it to match 
  // the file. if the file doesn't exist, or there is some other problen with 
  // loading it, the function will return 0 as the sample rate.
  float sampleRate = minim.loadFileIntoBuffer( "SD.wav", sampleBuffer );
  
  // make sure the file load worked
  if ( sampleRate > 0 )
  {
    // double the size of the buffer to give ourselves some silence to play with
    int originalBufferSize = sampleBuffer.getBufferSize();
    sampleBuffer.setBufferSize( originalBufferSize * 2 );
    
    // go through first half of the buffer, which contains the original sample,
    // and add a delayed version of each sample at some random position.
    // we happen to know that the source file is only one channel
    // but in general you'd want to iterate over all channels when doing something like this
    for( int s = 0; s < originalBufferSize; ++s )
    {
      int   delayIndex  = s + int( random( 0, originalBufferSize ) );
      float sampleValue = sampleBuffer.getSample( 0, s );
      float destValue   = sampleBuffer.getSample( 0, delayIndex ); 
      sampleBuffer.setSample( 0, // channel
                              delayIndex, // sample frame to set
                              sampleValue + destValue // the value to set
                            );
    }
    
    // create a sampler that will use our buffer to generate audio.
    // we must provide the sample rate of the audio and the number of voices. 
    sampler = new Sampler( sampleBuffer, sampleRate, 1 );
    
    // and finally, connect to the output so we can hear it
    sampler.patch( output );
  }
}

void draw()
{
  background(0);
  stroke(255);
  
  // use the mix buffer to draw the waveforms.
  for (int i = 0; i < output.bufferSize() - 1; i++)
  {
    float x1 = map(i, 0, output.bufferSize(), 0, width);
    float x2 = map(i+1, 0, output.bufferSize(), 0, width);
    line(x1, 50 - output.left.get(i)*50, x2, 50 - output.left.get(i+1)*50);
    line(x1, 150 - output.right.get(i)*50, x2, 150 - output.right.get(i+1)*50);
  }
}

void keyPressed() 
{
  if ( key == ' ' && sampler != null )
  {
    sampler.trigger();
  }
}

