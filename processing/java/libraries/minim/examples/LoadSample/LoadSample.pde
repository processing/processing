/**
 * Load Sample
 * by Damien Di Fede.
 *  
 * This sketch demonstrates how to use the <code>loadSample</code> 
 * method of <code>Minim</code>. The <code>loadSample</code> 
 * method allows you to specify the sample you want to load with 
 * a <code>String</code> and optionally specify what you 
 * want the buffer size of the returned <code>AudioSample</code> 
 * to be. If you don't specify a buffer size, the returned sample 
 * will have a buffer size of 1024. Minim is able to load wav files, 
 * au files, aif files, snd files, and mp3 files. When you call 
 * <code>loadSample</code>, if you just specify the filename it will 
 * try to load the sample from the data folder of your sketch. However, 
 * you can also specify an absolute path (such as "C:\foo\bar\thing.wav") 
 * and the file will be loaded from that location (keep in mind that 
 * won't work from an applet). You can also specify a URL (such as 
 * "http://www.mysite.com/mp3/song.mp3") but keep in mind that if you 
 * run the sketch as an applet you may run in to security restrictions 
 * if the applet is not on the same domain as the file you want to load. 
 * You can get around the restriction by signing the applet. Before you 
 * exit your sketch make sure you call the <code>close</code> method 
 * of any <code>AudioSamples</code>'s you have received from 
 * <code>loadSample</code>.
 *
 * An <code>AudioSample</code> is a special kind of file playback that 
 * allows you to repeatedly <i>trigger</i> an audio file. It does this 
 * by keeping the entire file in an internal buffer and then keeping a 
 * list of trigger points. <code>AudioSample</code> supports up to 20 
 * overlapping triggers, which should be plenty for short sounds. It is 
 * not advised that you use this class for long sounds (like entire songs, 
 * for example) because the entire file is kept in memory.
 * 
 * Press 'k' to trigger the sample.
 */

import ddf.minim.*;

Minim minim;
AudioSample kick;

void setup()
{
  size(512, 200, P2D);
  // always start Minim before you do anything with it
  minim = new Minim(this);
  // load BD.mp3 from the data folder with a 1024 sample buffer
  // kick = Minim.loadSample("BD.mp3");
  // load BD.mp3 from the data folder, with a 512 sample buffer
  kick = minim.loadSample("BD.mp3", 2048);
}

void draw()
{
  background(0);
  stroke(255);
  // use the mix buffer to draw the waveforms.
  // because these are MONO files, we could have used the left or right buffers and got the same data
  for (int i = 0; i < kick.bufferSize() - 1; i++)
  {
    line(i, 100 - kick.left.get(i)*50, i+1, 100 - kick.left.get(i+1)*50);
  }
}

void keyPressed()
{
  if ( key == 'k' ) kick.trigger();
}

void stop()
{
  // always close Minim audio classes when you are done with them
  kick.close();
  minim.stop();
  
  super.stop();
}
