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

public class CharactersStrings extends PApplet {

/**
 * Characters Strings. 
 * 
 * Click on the image to give it focus and then type letters to
 * shift the location of the image. 
 * Characters are typographic symbols such as A, d, and %.  
 * The character datatype, abbreviated as char, stores letters and 
 * symbols in the Unicode format, a coding system developed to support 
 * a variety of world languages. Characters are distinguished from other
 * symbols by putting them between single quotes ('P'). 
 * A string is a sequence of characters. A string is noted by surrounding 
 * a group of letters with double quotes ("Processing"). 
 * Chars and strings are most often used with the keyboard methods, 
 * to display text to the screen, and to load images or files. 
 */
 
PImage frog;
PFont fontA;
int lettersize = 90;
int xoffset;
char letter;

public void setup() 
{
  size(200, 200);
  fontA = loadFont("Eureka90.vlw"); 
  textFont(fontA); 
  textSize(lettersize);
    
  // The String datatype must be capitalized because it is a complex datatype.
  // A String is actually a class with its own methods, some of which are
  // featured below.
  String name= "rathausFrog";
  String extension = ".jpg";
  int nameLength = name.length();
  println("The length of " + name + " is " + nameLength + ".");
  name = name.concat(extension);
  nameLength = name.length();
  println("The length of " + name + " is " + nameLength + ".");

  // The parameter for the loadImage() method must be a string
  // This line could also be written "frog = loadImage("rathausFrog.jpg");
  frog = loadImage(name);
}

public void draw() 
{
  background(51); // Set background to dark gray
  
  image(frog, xoffset, 0);
  
  // Draw an X
  line(0, 0, width, height);  
  line(0, height, width, 0); 
  
  // Get the width of the letter
  int letterWidth = PApplet.parseInt(fontA.width(letter) * lettersize);
      
  // Draw the letter to the center of the screen
  text(letter, width/2-letterWidth/2, height/2);
}

public void keyPressed()
{
  // The variable "key" always contains the value of the most recent key pressed.
  // If the key is an upper or lowercase letter between 'A' and 'z'
  // the image is shifted to the corresponding value of that key
  if(key >= 'A' && key <= 'z') {
    letter = PApplet.parseChar(key);
    // Scale the values to numbers between 0 and 100
    float scale = 100.0f/57.0f;
    int temp = PApplet.parseInt((key - 'A') * scale);
    // Set the offset for the image
    xoffset = temp;
    println(key);
  }
}





  static public void main(String args[]) {
    PApplet.main(new String[] { "CharactersStrings" });
  }
}
