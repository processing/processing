/* delayExample<br/>
 * is an example of using the Delay UGen in a continuous sound example.
 * <p>
 * For more information about Minim and additional features, 
 * visit http://code.compartmental.net/minim/
 * <p>
 * author: Anderson Mills<br/>
 * Anderson Mills's work was supported by numediart (www.numediart.org)
 */

// import everything necessary to make sound.
import ddf.minim.*;
import ddf.minim.ugens.*;

// create all of the variables that will need to be accessed in
// more than one methods (setup(), draw(), stop()).
Minim minim;
AudioOutput out;
Delay myDelay;

// setup is run once at the beginning
void setup()
{
  // initialize the drawing window
  size( 512, 200 );

  // initialize the minim and out objects
  minim = new Minim(this);
  out = minim.getLineOut();
  
  // initialize myDelay with continual feedback and audio passthrough
  myDelay = new Delay( 0.4, 0.5, true, true );
  
  // sawh will create a Sawtooth wave with the requested number of harmonics.
  // like with Waves.randomNHarms for sine waves, 
  // you can create a richer sounding sawtooth this way.
  Waveform saw = Waves.sawh( 15 );
  // create the Blip that will be used
  Oscil myBlip = new Oscil( 245.0, 0.3, saw );
  
  // Waves.square will create a square wave with an uneven duty-cycle,
  // also known as a pulse wave. a square wave has only two values, 
  // either -1 or 1 and the duty cycle indicates how much of the wave 
  // should -1 and how much 1. in this case, we are asking for a square 
  // wave that is -1 90% of the time, and 1 10% of the time. 
  Waveform square = Waves.square( 0.9 );
  // create an LFO to be used for an amplitude envelope
  Oscil myLFO = new Oscil( 1, 0.3, square );
  // offset the center value of the LFO so that it outputs 0 
  // for the long portion of the duty cycle
  myLFO.offset.setLastValue( 0.3 );

  myLFO.patch( myBlip.amplitude );
  
  // and the Blip is patched through the delay into the output
  myBlip.patch( myDelay ).patch( out );
}

// draw is run many times
void draw()
{
  // erase the window to dark grey
  background( 64 );
  // draw using a light gray stroke
  stroke( 192 );
  // draw the waveforms
  for( int i = 0; i < out.bufferSize() - 1; i++ )
  {
    // find the x position of each buffer value
    float x1  =  map( i, 0, out.bufferSize(), 0, width );
    float x2  =  map( i+1, 0, out.bufferSize(), 0, width );
    // draw a line from one buffer position to the next for both channels
    line( x1, 50 + out.left.get(i)*50, x2, 50 + out.left.get(i+1)*50);
    line( x1, 150 + out.right.get(i)*50, x2, 150 + out.right.get(i+1)*50);
  }
  
  text( "Delay time is " + myDelay.delTime.getLastValue(), 5, 15 );
  text( "Delay amplitude (feedback) is " + myDelay.delAmp.getLastValue(), 5, 30 );
}

// when the mouse is moved, change the delay parameters
void mouseMoved()
{
  // set the delay time by the horizontal location
  float delayTime = map( mouseX, 0, width, 0.0001, 0.5 );
  myDelay.setDelTime( delayTime );
  // set the feedback factor by the vertical location
  float feedbackFactor = map( mouseY, 0, height, 0.99, 0.0 );
  myDelay.setDelAmp( feedbackFactor );
}
