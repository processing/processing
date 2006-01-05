import processing.core.*; import java.applet.*; import java.awt.*; import java.awt.image.*; import java.awt.event.*; import java.io.*; import java.net.*; import java.text.*; import java.util.*; import java.util.zip.*; public class LetterK extends PApplet {// Letter K
// by Peter Cho <www.pcho.net>

// Move the mouse across the screen to fold the "K"

// Created: 16 December 2002

int bgc, fgc, fgc2;

float p_x, p_y;
float p_fx, p_fy;
float p_v2, p_vx, p_vy;
float p_a2, p_ax, p_ay;
float p_mass, p_drag;

public void setup()
{
  size(200, 200, P3D);
  noStroke();
  colorMode(RGB, 255);
  bgc = color(134, 144, 154);
  fgc = color(235, 235, 30);
  fgc2 = color(240, 130, 20);
  init_particle(.6f, .9f,  width/2, height/2);
}

public void draw()
{
  background(bgc);
  pushMatrix();

  iterate_particle(.15f*(-p_x+mouseX), .15f*(-p_y+(height-mouseY)));

  translate(width/2, height/2, 0);
  fill(fgc);
  drawK();
 
  pushMatrix();
  translate(0, 0, 1);
  translate(.75f*(p_x-width/2), -.75f*(p_y-height/2), 0);
  translate(.75f*(p_x-width/2), -.75f*(p_y-height/2), 0);
  rotateZ(atan2(-(p_y-height/2),(p_x-width/2)) + PI/2);
  rotateX(PI);
  rotateZ(-(atan2(-(p_y-height/2),(p_x-width/2)) + PI/2));
  
  fill(fgc2);
  drawK();
  popMatrix();

  translate(0, 0, 2);
  translate(.75f*(p_x-width/2), -.75f*(p_y-height/2), 0);
  rotateZ(atan2(-(p_y-height/2),(p_x-width/2)) + PI/2);
  fill(bgc);
  beginShape(QUADS);
  vertex(-200, 0);
  vertex(+200, 0);
  vertex(+200, -200);
  vertex(-200, -200);
  endShape();
  
  popMatrix();
}

public void init_particle(float _mass, float _drag, float ox, float oy) 
{
  p_x = ox;
  p_y = oy;
  p_v2 = 0.0f;
  p_vx = 0.0f;
  p_vy = 0.0f;
  p_a2 = 0.0f;
  p_ax = 0.0f;
  p_ay = 0.0f;
  p_mass = _mass;
  p_drag = _drag;
}

public void iterate_particle(float fkx, float fky) 
{
  // iterate for a single force acting on the particle
  p_fx = fkx;
  p_fy = fky;
  p_a2 = p_fx*p_fx + p_fy*p_fy;
  if (p_a2 < 0.0000001f) return;
  p_ax = p_fx/p_mass;
  p_ay = p_fy/p_mass;
  p_vx += p_ax;
  p_vy += p_ay;
  p_v2 = p_vx*p_vx + p_vy*p_vy;
  if (p_v2 < 0.0000001f) return;
  p_vx *= (1.0f - p_drag);
  p_vy *= (1.0f - p_drag);
  p_x += p_vx;
  p_y += p_vy;
}

public void drawK() 
{
  scale(1);
  translate(-63, +71);
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
  translate(+63, -71);
}



}