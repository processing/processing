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

public class ArrayListClass extends PApplet {

/**
 * ArrayList of objects
 * by Daniel Shiffman.  
 * 
 * This example demonstrates how to use a Java ArrayList to store 
 * a variable number of objects.  Items can be added and removed
 * from the ArrayList.
 *
 * Click the mouse to add bouncing balls.
 */

ArrayList balls;
int ballWidth = 48;

public void setup() {
  size(200, 200);
  smooth();
  noStroke();

  // Create an empty ArrayList
  balls = new ArrayList();
  
  // Start by adding one element
  balls.add(new Ball(width/2, 0, ballWidth));
}

public void draw() {
  background(255);

  // With an array, we say balls.length, with an ArrayList, we say balls.size()
  // The length of an ArrayList is dynamic
  // Notice how we are looping through the ArrayList backwards
  // This is because we are deleting elements from the list  
  for (int i = balls.size()-1; i >= 0; i--) { 
    // An ArrayList doesn't know what it is storing so we have to cast the object coming out
    Ball ball = (Ball) balls.get(i);
    ball.move();
    ball.display();
    if (ball.finished()) {
      // Items can be deleted with remove()
      balls.remove(i);
    }
    
  }  
  
}

public void mousePressed() {
  // A new ball object is added to the ArrayList (by default to the end)
  balls.add(new Ball(mouseX, mouseY, ballWidth));
}

// Simple bouncing ball class

class Ball {
  
  float x;
  float y;
  float speed;
  float gravity;
  float w;
  float life = 255;
  
  Ball(float tempX, float tempY, float tempW) {
    x = tempX;
    y = tempY;
    w = tempW;
    speed = 0;
    gravity = 0.1f;
  }
  
    public void move() {
    // Add gravity to speed
    speed = speed + gravity;
    // Add speed to y location
    y = y + speed;
    // If square reaches the bottom
    // Reverse speed
    if (y > height) {
      // Dampening
      speed = speed * -0.8f;
      y = height;
    }
  }
  
  public boolean finished() {
    // Balls fade out
    life--;
    if (life < 0) {
      return true;
    } else {
      return false;
    }
  }
  
  public void display() {
    // Display the circle
    fill(0,life);
    //stroke(0,life);
    ellipse(x,y,w,w);
  }
}  

  static public void main(String args[]) {
    PApplet.main(new String[] { "ArrayListClass" });
  }
}
