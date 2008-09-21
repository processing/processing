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

public class Birds extends PApplet {

/**
 * Crazy Flocking 3D Birds 
 * by Ira Greenberg. 
 * 
 * Simulates a flock of birds using a Bird class and nested
 * pushMatrix() / popMatrix() functions. 
 * Trigonometry functions handle the flapping and sinuous movement.
 */

// Flock array
int birdCount = 200;
Bird[]birds = new Bird[birdCount];
float[]x = new float[birdCount];
float[]y = new float[birdCount];
float[]z = new float[birdCount];
float[]rx = new float[birdCount];
float[]ry = new float[birdCount];
float[]rz = new float[birdCount];
float[]spd = new float[birdCount];
float[]rot = new float[birdCount];

public void setup() {
  size(640, 360, P3D);
  noStroke();

  // Initialize arrays with random values
  for (int i = 0; i < birdCount; i++){
    birds[i] = new Bird(random(-300, 300), random(-300, 300), 
               random(-500, -2500), random(5, 30), random(5, 30)); 

    x[i] = random(20, 340);
    y[i] = random(30, 350);
    z[i] = random(1000, 4800);
    rx[i] = random(-160, 160);
    ry[i] = random(-55, 55);
    rz[i] = random(-20, 20);
    spd[i] = random(.1f, 3.75f);
    rot[i] = random(.025f, .15f);
  }
}

public void draw() {
  background(0);
  lights();
  for (int i = 0; i < birdCount; i++){
    birds[i].setFlight(x[i], y[i], z[i], rx[i], ry[i], rz[i]);
    birds[i].setWingSpeed(spd[i]);
    birds[i].setRotSpeed(rot[i]);
    birds[i].fly();
  }
}


class Bird {
  
  // Properties
  float offsetX, offsetY, offsetZ;
  float w, h;
  int bodyFill;
  int wingFill;
  float ang = 0, ang2 = 0, ang3 = 0, ang4 = 0;
  float radiusX = 120, radiusY = 200, radiusZ = 700;
  float rotX = 15, rotY = 10, rotZ = 5;
  float flapSpeed = 0.4f;
  float rotSpeed = 0.1f;

  // Constructors
  Bird(){
    this(0, 0, 0, 60, 80);
  }

  Bird(float offsetX, float offsetY, float offsetZ, 
  float w, float h){
    this.offsetX = offsetX;
    this.offsetY = offsetY;
    this.offsetZ = offsetZ;
    this.h = h;
    this.w = w;
    bodyFill = color(153);
    wingFill = color(204);
  }

  public void setFlight(float radiusX, float radiusY, float radiusZ, 
    float rotX, float rotY, float rotZ){
    this.radiusX = radiusX;
    this.radiusY = radiusY;
    this.radiusZ = radiusZ;

    this.rotX = rotX;
    this.rotY = rotY;
    this.rotZ = rotZ;
  }

  public void setWingSpeed(float flapSpeed){
    this.flapSpeed = flapSpeed;
  }

  public void setRotSpeed(float rotSpeed){
    this.rotSpeed = rotSpeed;
  }

  public void fly() {
    pushMatrix();
    float px, py, pz;
    
    // Flight
    px = sin(radians(ang3)) * radiusX;
    py = cos(radians(ang3)) * radiusY;
    pz = sin(radians(ang4)) * radiusZ;
    
    translate(width/2 + offsetX + px, height/2 + offsetY+py, -700 + offsetZ+pz);

    rotateX(sin(radians(ang2)) * rotX);
    rotateY(sin(radians(ang2)) * rotY);
    rotateZ(sin(radians(ang2)) * rotZ);

    // Body
    fill(bodyFill);
    box(w/5, h, w/5);

    // Left wing
    fill(wingFill);
    pushMatrix();
    rotateY(sin(radians(ang)) * 20);
    rect(0, -h/2, w, h);
    popMatrix();

    // Right wing
    pushMatrix();
    rotateY(sin(radians(ang)) * -20);
    rect(-w, -h/2, w, h);
    popMatrix();

    // Wing flap
    ang += flapSpeed;
    if (ang > 3) {
      flapSpeed*=-1;
    } 
    if (ang < -3) {
      flapSpeed*=-1;
    }

    // Ang's run trig functions
    ang2 += rotSpeed;
    ang3 += 1.25f;
    ang4 += 0.55f;
    popMatrix();
  }
}




  static public void main(String args[]) {
    PApplet.main(new String[] { "Birds" });
  }
}
