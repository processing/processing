/**
 * Load File
 * by Damien Di Fede.
 *  
 * This sketch demonstrates how to use the <code>loadFile</code> method 
 * of <code>Minim</code>. The <code>loadFile</code> method allows you to 
 * specify the file you want to load with a <code>String</code> and optionally 
 * specify what you want the buffer size of the returned <code>AudioPlayer</code> 
 * to be. If you don't specify a buffer size, the returned player will have a 
 * buffer size of 1024. Minim is able to play wav files, au files, aif files, 
 * snd files, and mp3 files. When you call <code>loadFile</code>, if you just 
 * specify the filename it will try to load the file from the data folder of 
 * your sketch. However, you can also specify an absolute path 
 * (such as "C:\foo\bar\thing.wav") and the file will be loaded from that 
 * location (keep in mind that won't work from an applet). You can also specify 
 * a URL (such as "http://www.mysite.com/mp3/song.mp3") but keep in mind that 
 * if you run the sketch as an applet you may run in to security restrictions 
 * if the applet is not on the same domain as the file you want to load. You can 
 * get around the restriction by signing the applet. Before you exit your sketch 
 * make sure you call the <code>close</code> method of any <code>AudioPlayer</code>'s 
 * you have received from <code>loadFile</code>, followed by the <code>stop</code>
 * method of <code>Minim</code>.
 */

import ddf.minim.*;

AudioPlayer player;
Minim minim;

void setup()
{
  size(512, 200, P3D);

  minim = new Minim(this);
  
  // load a file, give the AudioPlayer buffers that are 1024 samples long
  // player = minim.loadFile("groove.mp3");
  
  // load a file, give the AudioPlayer buffers that are 2048 samples long
  player = minim.loadFile("groove.mp3", 2048);
  // play the file
  player.play();
}

void draw()
{
  background(0);
  stroke(255);
  // draw the waveforms
  // the values returned by left.get() and right.get() will be between -1 and 1,
  // so we need to scale them up to see the waveform
  // note that if the file is MONO, left.get() and right.get() will return the same value
  for(int i = 0; i < player.left.size()-1; i++)
  {
    line(i, 50 + player.left.get(i)*50, i+1, 50 + player.left.get(i+1)*50);
    line(i, 150 + player.right.get(i)*50, i+1, 150 + player.right.get(i+1)*50);
  }
}

void stop()
{
  // always close Minim audio classes when you are done with them
  player.close();
  minim.stop();
  
  super.stop();
}
