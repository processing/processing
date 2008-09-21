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

public class Keyboard extends PApplet {

/**
 * Keyboard. 
 * 
 * Click on the image to give it focus and press the letter keys 
 * to create forms in time and space. Each key has a unique identifying 
 * number called it's ASCII value. These numbers can be used to position 
 * shapes in space. 
 */
 
int numChars = 26;
int[] colors = new int[numChars];
int keyIndex;
float keyScale;
int rectWidth;

    
public void setup()
{
  size(200, 200);
  noStroke();
  background(0);
  keyScale = 200/numChars-1.0f;
  rectWidth = width/4;
}

public void draw()
{ 
  if(keyPressed) {
    if(key >= 'A' && key <= 'z') {
      if(key <= 'Z') {
        keyIndex = key-'A';
      } else {
        keyIndex = key-'a';
      }
      fill(millis()%255);
      float beginRect = rectWidth/2 + keyIndex*keyScale-rectWidth/2;
      rect(beginRect, 0.0f, rectWidth, height);
    }
  }
}

  static public void main(String args[]) {
    PApplet.main(new String[] { "Keyboard" });
  }
}
