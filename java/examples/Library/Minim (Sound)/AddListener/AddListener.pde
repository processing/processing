/**
 * Add Listener
 * by Damien Di Fede.
 *  
 * This sketch demonstrates how to use the <code>addListener</code> method of a <code>Recordable</code> class. 
 * The class used here is <code>AudioPlayer</code>, but you can also add listeners to <code>AudioInput</code>, 
 * <code>AudioOutput</code>, and <code>AudioSample</code> objects. The class defined in waveform.pde implements 
 * the <code>AudioListener</code> interface and can therefore be added as a listener to <code>groove</code>.
 */

import ddf.minim.*;

Minim minim;
AudioPlayer groove;
WaveformRenderer waveform;

void setup()
{
  size(512, 200, P2D);
  
  minim = new Minim(this);
  groove = minim.loadFile("groove.mp3", 512);
  groove.loop();
  waveform = new WaveformRenderer();
  groove.addListener(waveform);
}

void draw()
{
  background(0);
  // see waveform.pde for an explanation of how this works
  waveform.draw();
}

void stop()
{
  // always close Minim audio classes when you are done with them
  groove.close();
  // always stop Minim before exiting.
  minim.stop();
  super.stop();
}
