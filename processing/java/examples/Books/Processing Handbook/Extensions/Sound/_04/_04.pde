/**
Applies reverb 10 times to a succession of guitar chords.
Inspired by Alvin Lucier's "I am Sitting in a Room."
Based on Reverb by Krister Olsson <http://www.tree-axis.com>
*/
import krister.Ess.*;

AudioChannel myChannel;
Reverb myReverb;
Normalize myNormalize;
int numRepeats = 9;
int repeats = 0;
float rectWidth;

void setup() {
  size(256, 200);
  noStroke();
  background(0);
  rectWidth = width / (numRepeats + 1.0);
  Ess.start(this); // Start Ess
// Load audio file into a AudioChannel, file must be in the sketch's "data" folder
  myChannel = new AudioChannel("guitar.aif");
  myReverb = new Reverb();
  myNormalize = new Normalize();
  myNormalize.filter(myChannel); // Normalize the audio
  myChannel.play(1);
}

void draw() {
  if (repeats < numRepeats) {
    if (myChannel.state == Ess.STOPPED) { // If the audio isn't playing
      myChannel.adjustChannel(myChannel.size / 16, Ess.END);
      myChannel.out(myChannel.size);
// Apply reverberation "in place" to the audio in the channel
      myReverb.filter(myChannel);
// Normalize the signal
      myNormalize.filter(myChannel);
      myChannel.play(1);
      repeats++;
    }
  } else {
    exit(); // Quit the program
  }
// Draw rectangle to show the current repeat (1 of 9)
  rect(rectWidth * repeats, 0, rectWidth - 1, height);
}

public void stop() {
  Ess.stop(); // When program stops, stop Ess too
  super.stop();
}
