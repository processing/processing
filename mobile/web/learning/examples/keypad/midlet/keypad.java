import processing.core.*; public class keypad extends PMIDlet{// Keypad
// modified for Mobile by Francis Li <http://www.francisli.com>
//
// based on
//
// Keyboard
// by REAS <http://reas.com> 
//
// Press the numbers on the keypad to create forms in time and space. Each
// key has a unique identifying number called 
// it's ASCII value. These numbers can be used to position shapes in space.
//
// Created 27 October 2002 
// Modified 19 September 2005

int numChars = 10; 
int[] colors = new int[numChars]; 
int keyIndex; 
int keyScale; 
int rectWidth; 
 
    
public void setup() 
{ 
  noStroke(); 
  background(0); 
  keyScale = width / numChars - 1; 
  rectWidth = width / 4; 
} 
 
public void draw() 
{ 
  if(keyPressed) { 
    if(key >= '0' && key <= '9') { 
      keyIndex = key - '0'; 
      fill(millis() % 255); 
      int beginRect = rectWidth / 2 + keyIndex * keyScale - rectWidth / 2; 
      rect(beginRect, 0, rectWidth, height); 
    } 
  } 
} 
}