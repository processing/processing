import processing.core.*; 

import java.applet.*; 
import java.awt.*; 
import java.awt.image.*; 
import java.awt.event.*; 
import java.io.*; 
import java.net.*; 
import java.text.*; 
import java.util.*; 
import java.util.zip.*; 
import java.util.regex.*; 

public class KineticType extends PApplet {

/**
 * Kinetic Type 
 * by Zach Lieberman. 
 * 
 * Using push() and pop() to define the curves of the lines of type. 
 */
 
Line ln;
Line lns[];

String words[] = {
  "sometimes it's like", "the lines of text", "are so happy", "that they want to dance",
  "or leave the page or jump", "can you blame them?", "living on the page like that",
  "waiting to be read..."
};

public void setup()
{
  size(640, 360, P3D);
  
  // Array of line objects
  lns = new Line[8];

  // Load the font from the sketch's data directory
  textFont(loadFont("Univers66.vlw.gz"), 1.0f);

  // White type
  fill(255);

  // Creating the line objects
  for(int i = 0; i < 8; i++) {
    // For every line in the array, create a Line object to animate
    // i * 70 is the spacing
    ln = new Line(words[i], 0, i * 70);
    lns[i] = ln;
  }
}

public void draw()
{
  background(0);
  
  translate(-200, -50, -450);
  rotateY(0.3f);

  // Now animate every line object & draw it...
  for(int i = 0; i < 8; i++) {
    float f1 = sin((i + 1.0f) * (millis() / 10000.0f) * TWO_PI);
    float f2 = sin((8.0f - i) * (millis() / 10000.0f) * TWO_PI);
    Line line = lns[i];
    pushMatrix();
    translate(0.0f, line.yPosition, 0.0f);
    for(int j = 0; j < line.myLetters.length; j++) {
      if(j != 0) {
        translate(textWidth(line.myLetters[j - 1].myChar) * 75, 0.0f, 0.0f);
      }
      rotateY(f1 * 0.005f * f2);
      pushMatrix();
      scale(75.0f);
      text(line.myLetters[j].myChar, 0.0f, 0.0f);
      popMatrix();
    }
    popMatrix();
  }
}

class Letter
{
  char myChar;
  float x;
  float y;
  
  Letter(char c, float f, float f1)
  {
    myChar = c;
    x = f;
    y = f1;
  }
}
class Line
{
  String myString;
  int xPosition;
  int yPosition;
  int highlightNum;
  float speed;
  float curlInX;
  Letter myLetters[];
  
  Line(String s, int i, int j) 
  {
    myString = s;
    xPosition = i;
    yPosition = j;
    myLetters = new Letter[s.length()];
    float f1 = 0.0f;
    for(int k = 0; k < s.length(); k++)
    {
      char c = s.charAt(k);
      f1 += textWidth(c);
      Letter letter = new Letter(c, f1, 0.0f);
      myLetters[k] = letter;
    }

    curlInX = 0.1f;
  }
}
class Word
{
  String myName;
  int x;
  
  Word(String s)
  {
    myName = s;
  }
}

  static public void main(String args[]) {
    PApplet.main(new String[] { "KineticType" });
  }
}
