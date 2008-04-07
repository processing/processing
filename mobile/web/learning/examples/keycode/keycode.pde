// keycode
// by Francis Li <http://www.francisli.com/>
//
// A simple key profiling example (and handy utility) that
// shows the value of key presses as returned by the different
// system variables in Mobile Processing- key, keyCode, 
// and rawKeyCode.  This particular example runs on MIDP 2.0
// phones only since it uses the Phone library to run
// fullscreen.  When running fullscreen, softkey buttons 
// return key events, but the values of the rawKeyCode for
// softkey buttons are not defined in the MIDP standard- they
// are not only different between manufacturers, but can even be
// different between phones from the same manufacturer.
//
// Created 06 March 2008
//
import processing.phone.*;

Phone p;

PFont font;

void setup() {
  p = new Phone(this);
  p.fullscreen();
  
  font = loadFont(FACE_PROPORTIONAL, STYLE_PLAIN, SIZE_LARGE);
  textFont(font);
  textAlign(CENTER);
  fill(0);
  
  noLoop();
}

void draw() {
  background(255);
  text("Key:\n" + key + "\n\nkeyCode:\n" + keyCode + "\n\nrawKeyCode:\n" + rawKeyCode, 0, 0, width, height);
}

void keyPressed() {
  redraw();
}
