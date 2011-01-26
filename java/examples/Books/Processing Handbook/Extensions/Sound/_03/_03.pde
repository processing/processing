/**
Loads a sound file off disk and plays it in multiple voices at multiple sampling
increments (demonstrating voice allocation), panning it back and forth between
the speakers. Based on Ping Pong by Krister Olsson <http://tree-axis.com>
*/
import krister.Ess.*;

AudioChannel[] mySound = new AudioChannel[6]; // Six channels of audio playback
Envelope myEnvelope; // Create Envelope
boolean left = true;
boolean middle = false;
boolean right = false;
// Sampling rates to choose from
int[] rates = { 44100, 22050, 2943, 49500, 11025, 37083 };

void setup() {
  size(256, 200);
  stroke(255);
  Ess.start(this); // Start Ess
// Load sounds and set initial panning
// Sounds must be located in the sketch's "data" folder
  for (int i = 0; i < 6; i++) {
    mySound[i] = new AudioChannel("cela3.aif");
    mySound[i].smoothPan = true;
    mySound[i].pan(Ess.LEFT);
    mySound[i].panTo(1, 4000);
  }
  EPoint[] myEnv = new EPoint[3]; // Three-step breakpoint function
  myEnv[0] = new EPoint(0, 0); // Start at 0
  myEnv[1] = new EPoint(0.25, 1); // Attack
  myEnv[2] = new EPoint(2, 0); // Release
  myEnvelope = new Envelope(myEnv); // Bind an Envelope to the breakpoint function
}

void draw() {
  int playSound = 0; // How many sounds do we play on this frame?
  int which = -1; // If so, on which voice?
  noStroke();
  fill(0, 15);
  rect(0, 0, width, height); // Fade background
  stroke(102);
  line(width / 2, 0, width / 2, height); // Center line
  float interp = lerp(0, width, (mySound[0].pan + 1) / 2.0);
  stroke(255);
  line(interp, 0, interp, height); // Moving line
// Trigger 1-3 samples when the line passes the center line or hits an edge
  if ((mySound[0].pan < 0) && (middle == true)) {
    playSound = int(random(1, 3));
    middle = false;
  } else if ((mySound[0].pan > 0) && (middle == false)) {
    playSound = int(random(1, 3));
    middle = true;
  } else if ((mySound[0].pan < -0.9) && (left == true)) {
    playSound = int(random(1, 3));
    left = false;
  } else if ((mySound[0].pan > -0.9) && (left == false)) {
    left = true;
  } else if ((mySound[0].pan > 0.9) && (right == true)) {
    playSound = int(random(1, 3));
    right = false;
  } else if ((mySound[0].pan < 0.9) && (right == false)) {
    right = true;
  }
// Voice allocation block, figure out which AudioChannels are free
  while (playSound > 0) {
    for (int i = 0; i < mySound.length; i++) {
      if (mySound[i].state == Ess.STOPPED) {
        which = i; // Find a free voice
      }
    }
// If a voice is available and selected, play it
    if (which != -1) {
      mySound[which].sampleRate(rates[int(random(0,6))], false);
      mySound[which].play();
      myEnvelope.filter(mySound[which]); // Apply envelope
    }
    playSound--;
  }
}

public void stop() {
  Ess.stop(); // When program stops, stop Ess too
  super.stop();
}

void audioOutputPan(AudioOutput c) {
  c.panTo(-c.pan, 4000); // Reverse pan direction
}
