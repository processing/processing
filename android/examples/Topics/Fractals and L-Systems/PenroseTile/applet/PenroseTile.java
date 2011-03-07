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

public class PenroseTile extends PApplet {

/** 
 * Penrose Tile L-System 
 * by Geraldine Sarmiento (NYU ITP).
 *  
 * This code was based on Patrick Dwyer's L-System class. 
 */

PenroseLSystem ds;

public void setup() 
{
  size(640, 360);
  smooth();
  ds = new PenroseLSystem();
  ds.simulate(4);
}

public void draw() 
{
  background(0);
  ds.render();
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
    startLength = 190.0f;
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
class PenroseLSystem extends LSystem {

  int steps = 0;
  float somestep = 0.1f;
  String ruleW;
  String ruleX;
  String ruleY;
  String ruleZ;

  PenroseLSystem() {
    axiom = "[X]++[X]++[X]++[X]++[X]";
    ruleW = "YF++ZF4-XF[-YF4-WF]++";
    ruleX = "+YF--ZF[3-WF--XF]+";
    ruleY = "-WF++XF[+++YF++ZF]-";
    ruleZ = "--YF++++WF[+ZF++++XF]--XF";
    startLength = 460.0f;
    theta = radians(36);  
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
    translate(width/2, height/2);
    int pushes = 0;
    int repeats = 1;
    steps += 12;          
    if (steps > production.length()) {
      steps = production.length();
    }

    for (int i = 0; i < steps; i++) {
      char step = production.charAt(i);
      if (step == 'F') {
        stroke(255, 60);
        for (int j = 0; j < repeats; j++) {
          line(0, 0, 0, -drawLength);
          noFill();
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
        pushes++;            
        pushMatrix();
      } 
      else if (step == ']') {
        popMatrix();
        pushes--;
      } 
      else if ( (step >= 48) && (step <= 57) ) {
        repeats = (int)step - 48;
      }
    }

    // Unpush if we need too
    while (pushes > 0) {
      popMatrix();
      pushes--;
    }
  }

  public String iterate(String prod_, String rule_) {
    String newProduction = "";
    for (int i = 0; i < prod_.length(); i++) {
      char step = production.charAt(i);
      if (step == 'W') {
        newProduction = newProduction + ruleW;
      } 
      else if (step == 'X') {
        newProduction = newProduction + ruleX;
      }
      else if (step == 'Y') {
        newProduction = newProduction + ruleY;
      }  
      else if (step == 'Z') {
        newProduction = newProduction + ruleZ;
      } 
      else {
        if (step != 'F') {
          newProduction = newProduction + step;
        }
      }
    }

    drawLength = drawLength * 0.5f;
    generations++;
    return newProduction;
  }

}


  static public void main(String args[]) {
    PApplet.main(new String[] { "PenroseTile" });
  }
}
