// Sporepedia Mobile
// by Francis Li
// http://www.francisli.com/
//
// Posted April 6, 2009
//
// This mobile sketch allows you to randomly browse the user generated
// creations in the Spore universe.  Use the UP/DOWN/LEFT/RIGHT
// keys to get a new random Creature/Vehicle/UFO/Building and press
// the FIRE key to see the name and author.
//
// Download your own copy of the Spore Creature Creator now!
// http://www.spore.com/
//
import processing.phone.*;
import processing.xml.*;
import java.util.*;

Phone phone;
PClient client;
PFont font;
PImage highlight;
PImage[] icons;
StateMachine machine;
String username;
XMLParser parser;
String[] TYPES = { 
  "CREATURE", "UFO", "VEHICLE", "BUILDING" };

void setup() {
  phone = new Phone(this);
  phone.fullscreen();

  client = new PClient(this, "www.spore.com");
  parser = new XMLParser(this);

  username = null;
  font = loadFont(FACE_PROPORTIONAL, STYLE_BOLD, SIZE_LARGE);
  highlight = loadImage("highlight.png");

  icons = new PImage[4];
  for (int i = 0; i < 4; i++) {
    icons[i] = loadImage("icon" + i + ".png");
  }

  machine = new StateMachine();
  machine.current = new Search(0);

  framerate(20);
}

void draw() {
  if (machine.draw() == null) {
    exit();
  }
}

void sleep() {
  try {
    //// release some cpu cycles for the background network thread
    Thread.sleep(100);
  } 
  catch (Exception e) { 
  }
}

void keyPressed() {
  if (machine.keyPressed() == null) {
    exit();
  }
}

void libraryEvent(Object library, int event, Object data) {
  if (machine.libraryEvent(library, event, data) == null) {
    exit();
  }
}

interface State {
  State draw();
  State keyPressed();
  State libraryEvent(Object library, int event, Object data);
}

class StateMachine implements State {
  State current;

  StateMachine() {
  }

  State handleTransition(State next) {
    if (next == null) {
      return null;
    }
    current = next;
    return this;
  }

  State draw() {
    return handleTransition(current.draw());
  }

  State keyPressed() {
    return handleTransition(current.keyPressed());
  }

  State libraryEvent(Object library, int event, Object data) {
    return handleTransition(current.libraryEvent(library, event, data));
  }
}

class Asset extends Hashtable {
  PImage image;
}

int spinner;
void drawSpinner(int x, int y, int radius) {
  fill(0xff0000ff);
  noStroke();
  int diameter;
  for (int i = 7; i >= 0; i--) {
    if (i == spinner) {
      diameter = 14;
    } 
    else {
      diameter = 10;
    }
    ellipse(x + fptoi(mul(itofp(radius), cos(i * PI / 4))),
    y + fptoi(mul(itofp(radius), sin(i * PI / 4))),
    diameter,
    diameter);
  }
  spinner = (spinner + 1) % 8;
}
