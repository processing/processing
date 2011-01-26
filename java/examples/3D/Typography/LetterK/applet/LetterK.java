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

public class LetterK extends PApplet {

/**
 * Letter K 
 * by Peter Cho. 
 * 
 * Move the mouse across the screen to fold the "K". 
 */
 
int backgroundColor;
int foregroundColor;
int foregroundColor2;

float px, py;
float pfx, pfy;
float pv2, pvx, pvy;
float pa2, pax, pay;
float pMass, pDrag;

public void setup() {
  size(640, 360, P3D);
  noStroke();
  backgroundColor = color(134, 144, 154);
  foregroundColor = color(235, 235, 30);
  foregroundColor2 = color(240, 130, 20);
  initParticle(0.6f, 0.9f,  width/2, height/2);
}

public void draw() {
  background(backgroundColor);
  pushMatrix();

  iterateParticle(0.15f*(-px+mouseX), 0.15f*(-py+(height-mouseY)));

  translate(width/2, height/2, 0);
  fill(foregroundColor);
  drawK();
 
  pushMatrix();
  translate(0, 0, 1);
  translate(0.75f * (px-width/2), -0.75f * (py-height/2), 0);
  translate(0.75f * (px-width/2), -0.75f * (py-height/2), 0);
  rotateZ(atan2(-(py-height/2), (px-width/2)) + PI/2);
  rotateX(PI);
  rotateZ(-(atan2(-(py-height/2), (px-width/2)) + PI/2));
  
  fill(foregroundColor2);
  drawK();
  popMatrix();

  translate(0.75f * (px-width/2), -0.75f * (py-height/2), 2);
  rotateZ(atan2(-(py-height/2), (px-width/2)) + PI/2);
  
  fill(backgroundColor);
  beginShape(QUADS);
  vertex(-640, 0);
  vertex( 640, 0);
  vertex( 640, -360);
  vertex(-640, -360);
  endShape();
  
  popMatrix();
 
}

public void initParticle(float _mass, float _drag, float ox, float oy) {
  px = ox;
  py = oy;
  pv2 = 0.0f;
  pvx = 0.0f;
  pvy = 0.0f;
  pa2 = 0.0f;
  pax = 0.0f;
  pay = 0.0f;
  pMass = _mass;
  pDrag = _drag;
}

public void iterateParticle(float fkx, float fky) {
  // iterate for a single force acting on the particle
  pfx = fkx;
  pfy = fky;
  pa2 = pfx*pfx + pfy*pfy;
  if (pa2 < 0.0000001f) {
    return;
  }
  pax = pfx/pMass;
  pay = pfy/pMass;
  pvx += pax;
  pvy += pay;
  pv2 = pvx*pvx + pvy*pvy;
  if (pv2 < 0.0000001f) {
    return;
  }
  pvx *= (1.0f - pDrag);
  pvy *= (1.0f - pDrag);
  px += pvx;
  py += pvy;
}

public void drawK() {
  pushMatrix();
  
  scale(1.5f);
  translate(-63, 71);
  beginShape(QUADS);
  vertex(0, 0, 0);
  vertex(0, -142.7979f, 0);
  vertex(37.1992f, -142.7979f, 0);
  vertex(37.1992f, 0, 0);
  
  vertex(37.1992f, -87.9990f, 0);
  vertex(84.1987f, -142.7979f, 0);
  vertex(130.3979f, -142.7979f, 0);
  vertex(37.1992f, -43.999f, 0);

  vertex(77.5986f-.2f, -86.5986f-.3f, 0);
  vertex(136.998f, 0, 0);
  vertex(90.7988f, 0, 0);
  vertex(52.3994f-.2f, -59.999f-.3f, 0);
  endShape();
  //translate(63, -71);
  popMatrix();
}




  static public void main(String args[]) {
    PApplet.main(new String[] { "LetterK" });
  }
}
