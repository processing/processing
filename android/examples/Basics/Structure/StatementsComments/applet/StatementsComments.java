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

public class StatementsComments extends PApplet {
  public void setup() {/**
 * Statements and Comments. 
 * 
 * Statements are the elements that make up programs. 
 * The ";" (semi-colon) symbol is used to end statements.  
 * It is called the "statement terminator." 
 * Comments are used for making notes to help people better understand programs. 
 * A comment begins with two forward slashes ("//"). 
 */

// The size function is a statement that tells the computer 
// how large to make the window.
// Each function statement has zero or more parameters. 
// Parameters are data passed into the function
// and used as values for specifying what the computer will do.
size(200, 200);

// The background function is a statement that tells the computer
// which color to make the background of the window 
background(102);

  noLoop();
} 
  static public void main(String args[]) {
    PApplet.main(new String[] { "StatementsComments" });
  }
}
