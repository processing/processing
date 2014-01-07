/**
  * This is a relatively simple file player that lets you scrub forward and backward in an audio file.<br />
  * It should be noted that it's not *exactly* scrubbing because the playback speed is not changed,
  * it's simply that the position in the song is changed by very small increments when fast-forwarding or rewinding.
  * But the end result is convincing enough.
  * <p>
  * The positioning code is inside of the Play, Rewind, and Forward classes, which are in button.pde.
  * <p>
  * For more information about Minim and additional features, 
  * visit http://code.compartmental.net/minim/
  */

import ddf.minim.*;

Minim minim;
AudioPlayer song;
Play play;
Rewind rewind;
Forward ffwd;

void setup()
{
  size(512, 200, P3D);
  minim = new Minim(this);
  // load a file from the data folder, use a sample buffer of 1024 samples
  song = minim.loadFile("fair1939.wav", 512);
  // buttons for control
  play = new Play(width/2 - 50, 130, 20, 10);
  rewind = new Rewind(width/2, 130, 20, 10);
  ffwd = new Forward(width/2 + 50, 130, 20, 10);
}

void draw()
{
  background(0);
  // draw the wave form
  // this wav is MONO, so we only need the left channel, 
  // though we could have used the right channel and gotten the same values
  stroke(255);
  for (int i = 0; i < song.bufferSize() - 1;  i++)
  {
    line(i, 50 - song.left.get(i)*50, i+1, 50 - song.left.get(i+1)*10);
  }
  // draw the position in the song
  // the position is in milliseconds,
  // to get a meaningful graphic, we need to map the value to the range [0, width]
  float x = map(song.position(), 0, song.length(), 0, width);
  stroke(255, 0, 0);
  line(x, 50 - 20, x, 50 + 20);
  // do the controls
  play.update();
  play.draw();
  rewind.update();
  rewind.draw();
  ffwd.update(); 
  ffwd.draw();
}

void mousePressed()
{
  play.mousePressed();
  rewind.mousePressed();
  ffwd.mousePressed();
}

void mouseReleased()
{
  play.mouseReleased();
  rewind.mouseReleased();
  ffwd.mouseReleased();
}
