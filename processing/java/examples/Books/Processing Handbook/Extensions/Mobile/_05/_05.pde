import processing.sound.*;
// Notes range from 0 to 127 as in the MIDI specification
int[] notes = { 60, 62, 64, 65, 67, 69, 71, 72, 74 };

void setup() {
  noLoop(); // No drawing in this sketch, so we don't need to run the draw() loop
}

void keyPressed() {
  if ((key >= '1') && (key <= '9')) {
// Use the key as an index into the array of notes
    Sound.playTone(notes[key - '1'], 500, 80);
  }
}
