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

public class PenroseSnowflake extends PApplet {

/** 
 * Penrose Snowflake L-System 
 * by Geraldine Sarmiento (NYU ITP). 
 * 
 * This code was based on Patrick Dwyer's L-System class. 
 */

PenroseSnowflakeLSystem ps;

public void setup() {
  size(640, 360);
  stroke(255);
  noFill();
  smooth();
  ps = new PenroseSnowflakeLSystem();
  ps.simulate(4);
}

public void draw() {
  background(0);
  ps.render();
}


class LSystem 
{
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


class PenroseSnowflakeLSystem extends LSystem {

  String ruleF;

  PenroseSnowflakeLSystem() {
    axiom = "F3-F3-F3-F3-F";
    ruleF = "F3-F3-F45-F++F3-F";
    startLength = 450.0f;
    theta = radians(18); 
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
    translate(width, height);
    int repeats = 1;

    steps += 3;          
    if (steps > production.length()) {
      steps = production.length();
    }

    for (int i = 0; i < steps; i++) {
      char step = production.charAt(i);
      if (step == 'F') {
        for (int j = 0; j < repeats; j++) {
          line(0,0,0, -drawLength);
          translate(0, -drawLength);
        }
        repeats = 1;
      } 
      else if (step == '+') {
        for (int j = 0; j < repeats; j++) {
          rotate(theta);
        }
        repeats = 1;
      } 
      else if (step == '-') {
        for (int j =0; j < repeats; j++) {
          rotate(-theta);
        }
        repeats = 1;
      } 
      else if (step == '[') {
        pushMatrix();
      } 
      else if (step == ']') {
        popMatrix();
      } 
      else if ( (step >= 48) && (step <= 57) ) {
        repeats += step - 48;
      }
    }
  }


  public String iterate(String prod_, String rule_) {
    String newProduction = "";
    for (int i = 0; i < prod_.length(); i++) {
      char step = production.charAt(i);
      if (step == 'F') {
        newProduction = newProduction + ruleF;
      } 
      else {
        if (step != 'F') {
          newProduction = newProduction + step;
        }
      }
    }
    drawLength = drawLength * 0.4f;
    generations++;
    return newProduction;
  }

}

  static public void main(String args[]) {
    PApplet.main(new String[] { "PenroseSnowflake" });
  }
}
