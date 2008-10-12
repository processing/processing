/**
 * Get Set Pan
 * by Damien Di Fede.
 *  
 * This sketch demonstrates how to use the <code>getPan</code> and 
 * <code>setPan</code> methods of a <code>Controller</code> object. 
 * The class used here is an <code>AudioOutput</code> but you can also 
 * get and set the pan of <code>AudioSample</code>, <code>AudioSnippet</code>, 
 * <code>AudioInput</code>, and <code>AudioPlayer</code> objects. 
 * <code>getPan</code> and <code>setPan</code> will get and set the pan 
 * of the <code>DataLine</code> that is being used for input or output, 
 * but only if that line has a pan control. A <code>DataLine</code> is 
 * a low-level JavaSound class that is used for sending audio to, 
 * or receiving audio from, the audio system. You will notice in this 
 * sketch that you will hear the pan changing (if it's available) but you 
 * will not see any difference in the waveform being drawn. The reason 
 * for this is that what you see in the output's sample buffers is what 
 * it sends to the audio system. The system makes the pan change after 
 * receiving the samples.
 */

import ddf.minim.*;
import ddf.minim.signals.*;

Minim minim;
AudioOutput out;
Oscillator  osc;
WaveformRenderer waveform;

void setup()
{
  size(512, 200);
  
  minim = new Minim(this);
  out = minim.getLineOut();
  
  // see the example AudioOutput >> SawWaveSignal for more about this class
  osc = new SawWave(100, 0.2, out.sampleRate());
  // see the example Polyphonic >> addSignal for more about this
  out.addSignal(osc);
  
  waveform = new WaveformRenderer();
  // see the example Recordable >> addListener for more about this
  out.addListener(waveform); 
  
  textFont(createFont("Arial", 12));
}

void draw()
{
  background(0);
  // see waveform.pde for more about this
  waveform.draw();
  
  if ( out.hasControl(Controller.PAN) )
  {
    // map the mouse position to the range of the pan
    float val = map(mouseX, 0, width, -1, 1);
    // if a pan control is not available, this will do nothing
    out.setPan(val); 
    // if a pan control is not available this will report zero
    text("The current pan is " + out.getPan() + ".", 5, 15);
  }
  else
  {
    text("The output doesn't have a pan control.", 5, 15);
  }
}

void stop()
{
  // always close Minim audio classes when you are finished with them
  out.close();
  minim.stop();
  
  super.stop();
}
