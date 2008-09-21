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

public class LinearGradient extends PApplet {

/**
 * Simple Linear Gradient 
 * by Ira Greenberg. 
 * 
 * Using the convenient red(), green() 
 * and blue() component functions,
 * generate some linear gradients.
 */

// constants
int Y_AXIS = 1;
int X_AXIS = 2;

public void setup(){
  size(200, 200);

  // create some gradients
  // background
  int b1 = color(190, 190, 190);
  int b2 = color(20, 20, 20);
  setGradient(0, 0, width, height, b1, b2, Y_AXIS);
  //center squares
  int c1 = color(255, 120, 0);
  int c2 = color(10, 45, 255);
  int c3 = color(10, 255, 15);
  int c4 = color(125, 2, 140);
  int c5 = color(255, 255, 0);
  int c6 = color(25, 255, 200);
  setGradient(25, 25, 75, 75, c1, c2, Y_AXIS);
  setGradient(100, 25, 75, 75, c3, c4, X_AXIS);
  setGradient(25, 100, 75, 75, c2, c5, X_AXIS);
  setGradient(100, 100, 75, 75, c4, c6, Y_AXIS);
}

public void setGradient(int x, int y, float w, float h, int c1, int c2, int axis ){
  // calculate differences between color components 
  float deltaR = red(c2)-red(c1);
  float deltaG = green(c2)-green(c1);
  float deltaB = blue(c2)-blue(c1);

  // choose axis
  if(axis == Y_AXIS){
    /*nested for loops set pixels
     in a basic table structure */
    // column
    for (int i=x; i<=(x+w); i++){
      // row
      for (int j = y; j<=(y+h); j++){
        int c = color(
        (red(c1)+(j-y)*(deltaR/h)),
        (green(c1)+(j-y)*(deltaG/h)),
        (blue(c1)+(j-y)*(deltaB/h)) 
          );
        set(i, j, c);
      }
    }  
  }  
  else if(axis == X_AXIS){
    // column 
    for (int i=y; i<=(y+h); i++){
      // row
      for (int j = x; j<=(x+w); j++){
        int c = color(
        (red(c1)+(j-x)*(deltaR/h)),
        (green(c1)+(j-x)*(deltaG/h)),
        (blue(c1)+(j-x)*(deltaB/h)) 
          );
        set(j, i, c);
      }
    }  
  }
}


  static public void main(String args[]) {
    PApplet.main(new String[] { "LinearGradient" });
  }
}
