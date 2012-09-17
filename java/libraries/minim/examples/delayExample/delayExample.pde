/* delayExample
   is an example of using the Delay UGen in a continuous sound example.
   Use the mouse to control the delay time and the amount of feedback
   in the delay unit.
   author: Anderson Mills
   Anderson Mills's work was supported by numediart (www.numediart.org)
*/

// import everything necessary to make sound.
import ddf.minim.*;
import ddf.minim.ugens.*;

// create all of the variables that will need to be accessed in
// more than one methods (setup(), draw(), stop()).
Minim minim;
AudioOutput out;
Delay myDelay1;

// setup is run once at the beginning
void setup()
{
  // initialize the drawing window
  size( 512, 200, P2D );

  // initialize the minim and out objects
  minim = new Minim(this);
  out = minim.getLineOut( Minim.MONO, 2048 );
  
  // initialize myDelay1 with continual feedback and no audio passthrough
  myDelay1 = new Delay( 0.6, 0.9, true, false );
  // create the Blip that will be used
  Oscil myBlip = new Oscil( 245.0, 0.3, Waves.saw( 15 ) );
  
  // create an LFO to be used for an amplitude envelope
  Oscil myLFO = new Oscil( 0.5, 0.3, Waves.square( 0.005 ) );
  // our LFO will operate on a base amplitude
  Constant baseAmp = new Constant(0.3);
  // we get the final amplitude by summing the two
  Summer ampSum = new Summer();
  
  Summer sum = new Summer();
  
  // patch everything together
  // the LFO is patched into a summer along with a constant value
  // and that sum is used to drive the amplitude of myBlip
  baseAmp.patch( ampSum );
  myLFO.patch( ampSum );
  ampSum.patch( myBlip.amplitude );

  // the Blip is patched directly into the sum 
  myBlip.patch( sum );
  
 // and the Blip is patched through the delay into the sum.
  myBlip.patch( myDelay1 ).patch( sum );

  // patch the sum into the output
  sum.patch( out );
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
}

// when the mouse is moved, change the delay parameters
void mouseMoved()
{
  // set the delay time by the horizontal location
  float delayTime = map( mouseX, 0, width, 0.0001, 0.5 );
  myDelay1.setDelTime( delayTime );
  // set the feedback factor by the vertical location
  float feedbackFactor = map( mouseY, 0, height, 0.0, 0.99 );
  myDelay1.setDelAmp( feedbackFactor );
}
