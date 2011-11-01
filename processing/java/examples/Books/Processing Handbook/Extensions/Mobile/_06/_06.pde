import processing.sound.*;
Sound s;

void setup() {
// The file, soundtrack.mid, must be copied into the data folder of this sketch
  s = new Sound("soundtrack.mid");
  softkey("Play");
  noLoop();
}
void softkeyPressed(String label) {
  if (label.equals("Play")) {
    s.play();
    softkey("Pause"); // Change the label of the softkey to Pause
  } else if (label.equals("Pause")) {
    s.pause();
    softkey("Play"); // Change the label of the softkey back to Play
  }
}
