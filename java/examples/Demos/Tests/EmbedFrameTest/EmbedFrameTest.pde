// Based on code by GeneKao (https://github.com/GeneKao)

import peasy.*;

import javax.swing.JFrame;
import java.awt.BorderLayout;
import java.awt.Insets;
EmbeddedSketch eSketch;
ChildApplet child = new ChildApplet();
boolean mousePressedOnParent = false;
PeasyCam cam, cam2;

void setup() {
  size(320, 240, P3D);
  cam = new PeasyCam(this, 300);
  eSketch = new EmbeddedSketch(child);
  smooth();
}

void draw() {
  background(250);
  if (mousePressed) {
    fill(0);
    text("Mouse pressed on parent.", 10, 10);
    fill(0, 240, 0);
    ellipse(mouseX, mouseY, 60, 60);
    mousePressedOnParent = true;
  } else {
    fill(20);
    ellipse(width/2, height/2, 60, 60);
    mousePressedOnParent = false;
  }
  box(100);
  if (eSketch.sketch.mousePressed) {
    text("Mouse pressed on child.", 10, 30);
  }
}

//The JFrame which will contain the child applet
class EmbeddedSketch extends JFrame {
  PApplet sketch;
  EmbeddedSketch(PApplet p) {
    int w = 400;
    int h = 400;
    sketch = p;
    setVisible(true);
      
    setLayout(new BorderLayout());
    add(p, BorderLayout.CENTER);
    p.init();
      
    Insets insets = getInsets();
    setSize(insets.left + w, insets.top + h);
    p.setBounds(insets.left, insets.top, w, h);
            
    setLocation(500, 200);
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
  }
}

class ChildApplet extends PApplet {
  void setup() {
    size(400, 400, P3D);
    smooth();
    cam2 = new PeasyCam(this, 300);
    cam2.reset();
  }
  
  void draw() {
    background(0);
    if (mousePressed) {
      fill(240, 0, 0);
      ellipse(mouseX, mouseY, 20, 20);
      fill(255);
      text("Mouse pressed on child.", 10, 30);
    } else {
      fill(255);
      ellipse(width/2, height/2, 20, 20);
    }

    box(100, 200, 100);
    if (mousePressedOnParent) {
      fill(255);
      text("Mouse pressed on parent", 20, 20);
    }
  }
}

