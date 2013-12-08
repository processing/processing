/**
  * This sketch demonstrates how to use an <code>AudioRecorder</code> to record audio to disk. 
  * Press 'r' to toggle recording on and off and the press 's' to save to disk. 
  * The recorded file will be placed in the sketch folder of the sketch.
  * <p>
  * For more information about Minim and additional features, 
  * visit http://code.compartmental.net/minim/
  */

import ddf.minim.*;
import ddf.minim.ugens.*;

Minim         minim;
AudioOutput   out;
AudioRecorder recorder;

void setup()
{
  size(512, 200, P3D);
  
  minim = new Minim(this);

  out = minim.getLineOut();
  
  // create a recorder that will record from the output to the filename specified
  // the file will be located in the sketch's root folder.
  recorder = minim.createRecorder(out, "myrecording.wav");
  
  // patch some sound into the output so we have something to record
  Oscil wave = new Oscil( 440.f, 1.0f );
  Oscil mod  = new Oscil( 4.0f,  0.25f, Waves.SAW );
  mod.offset.setLastValue( 0.5f );
  mod.patch( wave.amplitude );
  wave.patch( out );
  
  textFont(createFont("Arial", 12));
}

void draw()
{
  background(0); 
  stroke(255);
  // draw the waveforms
  // the values returned by left.get() and right.get() will be between -1 and 1,
  // so we need to scale them up to see the waveform
  for(int i = 0; i < out.bufferSize() - 1; i++)
  {
    line(i, 50  + out.left.get(i)*50,  i+1, 50  + out.left.get(i+1)*50);
    line(i, 150 + out.right.get(i)*50, i+1, 150 + out.right.get(i+1)*50);
  }
  
  if ( recorder.isRecording() )
  {
    text("Currently recording...", 5, 15);
  }
  else
  {
    text("Not recording.", 5, 15);
  }
}

void keyReleased()
{
  if ( key == 'r' ) 
  {
    // to indicate that you want to start or stop capturing audio data, you must call
    // beginRecord() and endRecord() on the AudioRecorder object. You can start and stop
    // as many times as you like, the audio data will be appended to the end of the buffer 
    // (in the case of buffered recording) or to the end of the file (in the case of streamed recording). 
    if ( recorder.isRecording() ) 
    {
      recorder.endRecord();
    }
    else 
    {
      recorder.beginRecord();
    }
  }
  if ( key == 's' )
  {
    // we've filled the file out buffer, 
    // now write it to the file we specified in createRecorder
    // in the case of buffered recording, if the buffer is large, 
    // this will appear to freeze the sketch for sometime
    // in the case of streamed recording, 
    // it will not freeze as the data is already in the file and all that is being done
    // is closing the file.
    // the method returns the recorded audio as an AudioRecording, 
    // see the example  AudioRecorder >> RecordAndPlayback for more about that
    recorder.save();
    println("Done saving.");
  }
}
