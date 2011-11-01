PFont font;
String[] words = { "Three", "strikes", "and", "you're", "out...", " " };
int whichWord = 0;

void setup() {
  size(100, 100);
  font = loadFont("Eureka-32.vlw");
  textFont(font);
  textAlign(CENTER);
  frameRate(4);
}

void draw() {
  background(204);
  whichWord++;
  if (whichWord == words.length) {
    whichWord = 0;
  }
  text(words[whichWord], width / 2, 55);
}
