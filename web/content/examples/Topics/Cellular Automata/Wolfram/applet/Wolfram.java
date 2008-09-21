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

public class Wolfram extends PApplet {

/**
 * Wolfram Cellular Automata
 * by Daniel Shiffman.  
 * 
 * Simple demonstration of a Wolfram 1-dimensional cellular automata
 * When the system reaches bottom of the window, it restarts with a new ruleset
 * Mouse click restarts as well. 
 */
 
CA ca;   // An instance object to describe the Wolfram basic Cellular Automata

public void setup() {
  size(640, 360, P2D);
  frameRate(30);
  background(0);
  int[] ruleset = {0,1,0,1,1,0,1,0};    // An initial rule system
  ca = new CA(ruleset);                 // Initialize CA
}

public void draw() {
  ca.render();    // Draw the CA
  ca.generate();  // Generate the next level
  
  if (ca.finished()) {   // If we're done, clear the screen, pick a new ruleset and restart
    background(0);
    ca.randomize();
    ca.restart();
  }
}

public void mousePressed() {
  background(0);
  ca.randomize();
  ca.restart();
}



class CA {

  int[] cells;     // An array of 0s and 1s 
  int generation;  // How many generations?
  int scl;         // How many pixels wide/high is each cell?

  int[] rules;     // An array to store the ruleset, for example {0,1,1,0,1,1,0,1}

  CA(int[] r) {
    rules = r;
    scl = 1;
    cells = new int[width/scl];
    restart();
  }
  
   CA() {
    scl = 1;
    cells = new int[width/scl];
    randomize();
    restart();
  }
  
  // Set the rules of the CA
  public void setRules(int[] r) {
    rules = r;
  }
  
  // Make a random ruleset
  public void randomize() {
    for (int i = 0; i < 8; i++) {
      rules[i] = PApplet.parseInt(random(2));
    }
  }
  
  // Reset to generation 0
  public void restart() {
    for (int i = 0; i < cells.length; i++) {
      cells[i] = 0;
    }
    cells[cells.length/2] = 1;    // We arbitrarily start with just the middle cell having a state of "1"
    generation = 0;
  }

  // The process of creating the new generation
  public void generate() {
    // First we create an empty array for the new values
    int[] nextgen = new int[cells.length];
    // For every spot, determine new state by examing current state, and neighbor states
    // Ignore edges that only have one neighor
    for (int i = 1; i < cells.length-1; i++) {
      int left = cells[i-1];   // Left neighbor state
      int me = cells[i];       // Current state
      int right = cells[i+1];  // Right neighbor state
      nextgen[i] = rules(left,me,right); // Compute next generation state based on ruleset
    }
    // Copy the array into current value
    cells = (int[]) nextgen.clone();
    generation++;
  }
  
  // This is the easy part, just draw the cells, fill 255 for '1', fill 0 for '0'
  public void render() {
    for (int i = 0; i < cells.length; i++) {
      if (cells[i] == 1) fill(255);
      else               fill(0);
      noStroke();
      rect(i*scl,generation*scl, scl,scl);
    }
  }
  
  // Implementing the Wolfram rules
  // Could be improved and made more concise, but here we can explicitly see what is going on for each case
  public int rules (int a, int b, int c) {
    if (a == 1 && b == 1 && c == 1) return rules[0];
    if (a == 1 && b == 1 && c == 0) return rules[1];
    if (a == 1 && b == 0 && c == 1) return rules[2];
    if (a == 1 && b == 0 && c == 0) return rules[3];
    if (a == 0 && b == 1 && c == 1) return rules[4];
    if (a == 0 && b == 1 && c == 0) return rules[5];
    if (a == 0 && b == 0 && c == 1) return rules[6];
    if (a == 0 && b == 0 && c == 0) return rules[7];
    return 0;
  }
  
  // The CA is done if it reaches the bottom of the screen
  public boolean finished() {
    if (generation > height/scl) {
       return true;
    } else {
       return false;
    }
  }
}


  static public void main(String args[]) {
    PApplet.main(new String[] { "Wolfram" });
  }
}
