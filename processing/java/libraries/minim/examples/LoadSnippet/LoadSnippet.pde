/**
 * Load Snippet
 * by Damien Di Fede.
 *  
 * This sketch demonstrates how to use the <code>loadSnippet</code> 
 * method of <code>Minim</code>. The <code>loadSnippet</code> method 
 * allows you to specify the file you want to load with a 
 * <code>String</code>. Unlike with <code>loadFile</code> and <code>loadSample</code>, 
 * you are not able to specify a buffer size because an <code>AudioSnippet</code> 
 * doesn't give you access to the samples as they are played. 
 * 
 * Minim is able to load wav files, au files, aif files, snd files, and mp3 
 * files. When you call <code>loadSnippet</code>, if you just specify the 
 * filename it will try to load the file from the data folder of your sketch. 
 * However, you can also specify an absolute path (such as "C:\foo\bar\thing.wav") 
 * and the file will be loaded from that location (keep in mind that won't 
 * work from an applet). You can also specify a URL (such as 
 * "http://www.mysite.com/mp3/song.mp3") but keep in mind that if you run the 
 * sketch as an applet you may run in to security restrictions if the applet 
 * is not on the same domain as the file you want to load. You can get around the 
 * restriction by signing the applet. 
 * 
 * <code>AudioSnippet</code> is a simple wrapper around a JavaSound <code>Clip</code> 
 * (It isn't called AudioClip because that's an interface defined in the package 
 * java.applet). It provides almost the exact same functionality, the main 
 * difference being that length, position, and cue are expressed in milliseconds 
 * instead of microseconds. One of the limitations of <code>AudioSnippet</code> is 
 * that you do not have access to the audio samples as they are played. However, 
 * you are spared all of the overhead associated with making samples available. 
 * An <code>AudioSnippet</code> is a good choice if all you need to do is play 
 * a short sound at some point. If your aim is to repeatedly trigger a sound, you
 * should use an <code>AudioSample</code> instead. 
 * 
 * Before you exit your sketch make sure you call the <code>close</code> 
 * method of any <code>AudioSnippet</code>'s you have received from 
 * <code>loadSnippet</code>.
 */

import ddf.minim.*;

Minim minim;
AudioSnippet snip;

void setup()
{
  size(512, 200);
  
  minim = new Minim(this);
  snip = minim.loadSnippet("groove.mp3");
  // play the file
  snip.play();
}

void draw()
{
  background(0);
  // there are no waveforms to draw
}

void stop()
{
  // always close Minim audio classes when you are done with them
  snip.close();
  // always stop Minim before exiting
  minim.stop();
  
  super.stop();
}
