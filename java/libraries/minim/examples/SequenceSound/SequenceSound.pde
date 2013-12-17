/**
  * This sketch demonstrates how to create synthesized sound with Minim using an <code>AudioOutput</code> and the
  * default instrument built into an <code>AudioOutput</code>. By using the <code>playNote</code> method you can 
  * schedule notes to played at some point in the future, essentially allowing to you create musical scores with 
  * code. Because they are constructed with code, they can be either deterministic or different every time. This
  * sketch creates a deterministic score, meaning it is the same every time you run the sketch. It also demonstrates 
  * a couple different versions of the <code>playNote</code> method.
  * <p>
  * For more complex examples of using <code>playNote</code> check out 
  * algorithmicCompExample and compositionExample in the Synthesis folder.
  * <p>
  * For more information about Minim and additional features, 
  * visit http://code.compartmental.net/minim/
  */

import ddf.minim.*;
import ddf.minim.ugens.*;

Minim minim;
AudioOutput out;

void setup()
{
  size(512, 200, P3D);
  
  minim = new Minim(this);
  
  // use the getLineOut method of the Minim object to get an AudioOutput object
  out = minim.getLineOut();
  
  // set the tempo of the sequencer
  // this makes the first argument of playNote 
  // specify the start time in quarter notes
  // and the duration becomes relative to the length of a quarter note
  // by default the tempo is 60 BPM (beats per minute).
  // at 60 BPM both start time and duration can be interpreted as seconds.
  // to retrieve the current tempo, use getTempo().
  out.setTempo( 80 );
  
  // pause the sequencer so our note play back will be rock solid
  // if you don't do this, then tiny bits of error can occur since 
  // the sequencer is running in parallel with you note queueing.
  out.pauseNotes();
  
  // given start time, duration, and frequency
  out.playNote( 0.0, 0.9, 97.99 );
  out.playNote( 1.0, 0.9, 123.47 );
  
  // given start time, duration, and note name  
  out.playNote( 2.0, 2.9, "C3" );
  out.playNote( 3.0, 1.9, "E3" );
  out.playNote( 4.0, 0.9, "G3" );
    
  // given start time and note name or frequency
  // (duration defaults to 1.0)
  out.playNote( 5.0, "" );
  out.playNote( 6.0, 329.63);
  out.playNote( 7.0, "G4" );
  
  // the note offset is simply added into the start time of 
  // every subsequenct call to playNote. It's expressed in beats.
  // to get the current note offset, use getNoteOffset().
  out.setNoteOffset( 8.1 );
  
  // because only given a note name or frequency
  // starttime defaults to 0.0 and duration defaults to 1.0
  out.playNote( "G5" );
  out.playNote( 987.77 );
  
  // now we can start the sequencer again to hear our sequence
  out.resumeNotes();
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
