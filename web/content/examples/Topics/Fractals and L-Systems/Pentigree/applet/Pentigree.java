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

public class Pentigree extends PApplet {

/** 
 * Pentigree L-System 
 * by Geraldine Sarmiento (NYU ITP). 
 * 
 * This code was based on Patrick Dwyer's L-System class. 
 */


PentigreeLSystem ps;

public void setup() {
  size(640, 360);
  smooth();
  ps = new PentigreeLSystem();
  ps.simulate(3);
}

public void draw() {
  background(0);
  ps.render();
}


class LSystem {

  int steps = 0;

  String axiom;
  String rule;
  String production;

  float startLength;
  float drawLength;
  float theta;

  int generations;

  LSystem() {

    axiom = "F";
    rule = "F+F-F";
    startLength = 90.0f;
    theta = radians(120.0f);
    reset();
  }

  public void reset() {
    production = axiom;
    drawLength = startLength;
    generations = 0;
  }

  public int getAge() {
    return generations;
  }
  
  public void render() {
    translate(width/2, height/2);
    steps += 5;          
    if (steps > production.length()) {
      steps = production.length();
    }
    for (int i = 0; i < steps; i++) {
      char step = production.charAt(i);
      if (step == 'F') {
        rect(0, 0, -drawLength, -drawLength);
        noFill();
        translate(0, -drawLength);
      } 
      else if (step == '+') {
        rotate(theta);
      } 
      else if (step == '-') {
        rotate(-theta);
      } 
      else if (step == '[') {
        pushMatrix();
      } 
      else if (step == ']') {
        popMatrix();            
      }
    }
  }
  
  public void simulate(int gen) {
    while (getAge() < gen) {
      production = iterate(production, rule);
    }
  }

  public String iterate(String prod_, String rule_) {
    drawLength = drawLength * 0.6f;
    generations++;
    String newProduction = prod_;          
    newProduction = newProduction.replaceAll("F", rule_);
    return newProduction;
  }
}

class PentigreeLSystem extends LSystem {

  int steps = 0;
  float somestep = 0.1f;
  float xoff = 0.01f;

  PentigreeLSystem() {
    axiom = "F-F-F-F-F";
    rule = "F-F++F+F-F-F";
    startLength = 60.0f;
    theta = radians(72);  
    reset();
  }

  public void useRule(String r_) {
    rule = r_;
  }

  public void useAxiom(String a_) {
    axiom = a_;
  }

  public void useLength(float l_) {
    startLength = l_;
  }

  public void useTheta(float t_) {
    theta = radians(t_);
  }

  public void reset() {
    production = axiom;
    drawLength = startLength;
    generations = 0;
  }

  public int getAge() {
    return generations;
  }

  public void render() {
    translate(width/4, height/2);
    steps += 3;          
    if (steps > production.length()) {
      steps = production.length();
    }

    for (int i = 0; i < steps; i++) {
      char step = production.charAt(i);
      if (step == 'F') {
        noFill();
        stroke(255);
        line(0, 0, 0, -drawLength);
        translate(0, -drawLength);
      } 
      else if (step == '+') {
        rotate(theta);
      } 
      else if (step == '-') {
        rotate(-theta);
      } 
      else if (step == '[') {
        pushMatrix();
      } 
      else if (step == ']') {
        popMatrix();
      }
    }
  }

}

  static public void main(String args[]) {
    PApplet.main(new String[] { "Pentigree" });
  }
}
