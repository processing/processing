/**
Sound is generated at setup with a triangle waveform and a simple envelope
generator. Insert your own array of notes as 'rawSequence' and let it roll.
*/
import krister.Ess.*;
AudioChannel myChannel; // Create channel
TriangleWave myWave; // Create triangle waveform
Envelope myEnvelope; // Create envelope
int numNotes = 200; // Number of notes
int noteDuration = 300; // Duration of each note in milliseconds
float[] rawSequence = { 293.6648, 293.6648, 329.62756, 329.62756, 391.995, 369.99445 , 293.6648, 293.6648,
                        329.62756, 293.6648, 439.997, 391.995, 293.6648, 293.6648, 587.3294, 493.8834,
                        391.995, 369.9945, 329.62756, 523.2516, 523.2516, 493.8834, 391.995,
                        439.997, 391.995 }; // Happy birthday

void setup() {
  size(100, 100);
  Ess.start(this); // Start Ess
  myChannel = new AudioChannel(); // Create a new AudioChannel
  myChannel.initChannel(myChannel.frames(rawSequence.length * noteDuration));
  int current = 0;
  myWave = new TriangleWave(480, 0.3); // Create triangle wave
  EPoint[] myEnv = new EPoint[3]; // Three-step breakpoint function
  myEnv[0] = new EPoint(0, 0); // Start at 0
  myEnv[1] = new EPoint(0.25, 1); // Attack
  myEnv[2] = new EPoint(2, 0); // Release
  myEnvelope = new Envelope(myEnv); // Bind Envelope to the breakpoint function
  int time = 0;
  for (int i = 0; i < rawSequence.length; i++) {
    myWave.frequency = rawSequence[current]; // Update waveform frequency
    int begin = myChannel.frames(time); // Starting position within Channel
    int e = int(noteDuration * 0.8);
    int end = myChannel.frames(e); // Ending position with Channel
    myWave.generate(myChannel, begin, end); // Render triangle wave
    myEnvelope.filter(myChannel, begin, end); // Apply envelope
    current++; // Move to next note
    time += noteDuration; // Increment the Channel output point
  }
  myChannel.play(); // Play the sound!
}

void draw() { } // Empty draw() keeps the program running

public void stop() {
  Ess.stop(); // When program stops, stop Ess too
  super.stop();
}
