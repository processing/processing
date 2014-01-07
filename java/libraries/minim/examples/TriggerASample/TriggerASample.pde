/**
  * This sketch demonstrates how to use the <code>loadSample</code> method of <code>Minim</code>. 
  * The <code>loadSample</code> method allows you to specify the sample you want to load 
  * with a <code>String</code> and optionally specify what you want the buffer size of the 
  * returned <code>AudioSample</code> to be. Minim is able to load wav files, au files, aif
  * files, snd files, and mp3 files. When you call <code>loadSample</code>, if you just 
  * specify the filename it will try to load the sample from the data folder of your sketch. 
  * However, you can also specify an absolute path (such as "C:\foo\bar\thing.wav") and the 
  * file will be loaded from that location (keep in mind that won't work from an applet). 
  * You can also specify a URL (such as "http://www.mysite.com/mp3/song.mp3") but keep in mind 
  * that if you run the sketch as an applet you may run in to security restrictions 
  * if the applet is not on the same domain as the file you want to load. You can get around 
  * the restriction by signing all of the jars in the applet.
  * <p>
  * An <code>AudioSample</code> is a special kind of file playback that allows
  * you to repeatedly <i>trigger</i> an audio file. It does this by keeping the
  * entire file in an internal buffer and then keeping a list of trigger points.
  * <code>AudioSample</code> supports up to 20 overlapping triggers, which
  * should be plenty for short sounds. It is not advised that you use this class
  * for long sounds (like entire songs, for example) because the entire file is
  * kept in memory.
  * <p>
  * Use 'k' and 's' to trigger a kick drum sample and a snare sample, respectively. 
  * You will see their waveforms drawn when they are played back.
  * <p>
  * For more information about Minim and additional features, 
  * visit http://code.compartmental.net/minim/
  */

import ddf.minim.*;

Minim minim;
AudioSample kick;
AudioSample snare;

void setup()
{
  size(512, 200, P3D);
  minim = new Minim(this);

  // load BD.wav from the data folder
  kick = minim.loadSample( "BD.mp3", // filename
                            512      // buffer size
                         );
                         
  // An AudioSample will spawn its own audio processing Thread, 
  // and since audio processing works by generating one buffer 
  // of samples at a time, we can specify how big we want that
  // buffer to be in the call to loadSample. 
  // above, we requested a buffer size of 512 because 
  // this will make the triggering of the samples sound more responsive.
  // on some systems, this might be too small and the audio 
  // will sound corrupted, in that case, you can just increase
  // the buffer size.
  
  // if a file doesn't exist, loadSample will return null
  if ( kick == null ) println("Didn't get kick!");
  
  // load SD.wav from the data folder
  snare = minim.loadSample("SD.wav", 512);
  if ( snare == null ) println("Didn't get snare!");
}

void draw()
{
  background(0);
  stroke(255);
  
  // use the mix buffer to draw the waveforms.
  for (int i = 0; i < kick.bufferSize() - 1; i++)
  {
    float x1 = map(i, 0, kick.bufferSize(), 0, width);
    float x2 = map(i+1, 0, kick.bufferSize(), 0, width);
    line(x1, 50 - kick.mix.get(i)*50, x2, 50 - kick.mix.get(i+1)*50);
    line(x1, 150 - snare.mix.get(i)*50, x2, 150 - snare.mix.get(i+1)*50);
  }
}

void keyPressed() 
{
  if ( key == 's' ) snare.trigger();
  if ( key == 'k' ) kick.trigger();
}

