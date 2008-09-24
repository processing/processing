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

public class Icosahedra extends PApplet {

/**
 * I Like Icosahedra
 * by Ira Greenberg.
 * 
 * This example plots icosahedra. The Icosahdron is a regular
 * polyhedron composed of 20 equalateral triangles.
 * 
 */
 
 
Icosahedron ico1;
Icosahedron ico2;
Icosahedron ico3;

public void setup(){
  size(640, 360, P3D);
  ico1 = new Icosahedron(75);
  ico2 = new Icosahedron(75);
  ico3 = new Icosahedron(75);
}

public void draw(){
  background(0);
  lights();
  translate(width/2, height/2);

  pushMatrix();
  translate(-width/3.5f, 0);
  rotateX(frameCount * PI/185);
  rotateY(frameCount * PI/-200);
  stroke(170, 0, 0);
  noFill();
  ico1.create();
  popMatrix();

  pushMatrix();
  rotateX(frameCount * PI/200);
  rotateY(frameCount * PI/300);
  stroke(150, 0, 180);
  fill(170, 170, 0);
  ico2.create();
  popMatrix();

  pushMatrix();
  translate(width/3.5f, 0);
  rotateX(frameCount * PI/-200);
  rotateY(frameCount * PI/200);
  noStroke();
  fill(0, 0, 185);
  ico3.create();
  popMatrix();
}

class Dimension3D {
  
   float w, h, d;
   
   Dimension3D(float w, float h, float d){
     this.w=w;
     this.h=h;
     this.d=d;
  }
}
class Icosahedron extends Shape3D {

  // icosahedron
  Vector3D topPoint;
  Vector3D[] topPent = new Vector3D[5];
  Vector3D bottomPoint;
  Vector3D[] bottomPent = new Vector3D[5];
  float angle = 0, radius = 150;
  float triDist;
  float triHt;
  float a, b, c;

  // constructor
  Icosahedron(float radius) {
    this.radius = radius;
    init();
  }

  Icosahedron(Vector3D v, float radius) {
    super(v);
    this.radius = radius;
    init();
  }

  // calculate geometry
  public void init() {
    c = dist(cos(0)*radius, sin(0)*radius, 
             cos(radians(72))*radius,  sin(radians(72))*radius);
    b = radius;
    a = (float)(Math.sqrt(((c*c)-(b*b))));

    triHt = (float)(Math.sqrt((c*c)-((c/2)*(c/2))));

    for (int i = 0; i < topPent.length; i++){
      topPent[i] = new Vector3D(cos(angle)*radius, 
                                sin(angle)*radius, triHt/2.0f);
      angle+=radians(72);
    }
    topPoint = new Vector3D(0, 0, triHt/2.0f+a);
    angle = 72.0f/2.0f;
    for (int i = 0; i < topPent.length; i++){
      bottomPent[i] = new Vector3D(cos(angle)*radius, 
                                   sin(angle)*radius, -triHt/2.0f);
      angle+=radians(72);
    }
    bottomPoint = new Vector3D(0, 0, -(triHt/2.0f+a));
  }

  // draws icosahedron 
  public void create(){
    for (int i=0; i<topPent.length; i++){
      // icosahedron top
      beginShape();
      if (i<topPent.length-1){
        vertex(x+topPent[i].x, y+topPent[i].y, z+topPent[i].z);
        vertex(x+topPoint.x, y+topPoint.y, z+topPoint.z);
        vertex(x+topPent[i+1].x, y+topPent[i+1].y, z+topPent[i+1].z);
      } 
      else {
        vertex(x+topPent[i].x, y+topPent[i].y, z+topPent[i].z);
        vertex(x+topPoint.x, y+topPoint.y, z+topPoint.z);
        vertex(x+topPent[0].x, y+topPent[0].y, z+topPent[0].z);
      }
      endShape(CLOSE);

      // icosahedron bottom
      beginShape();
      if (i<bottomPent.length-1){
        vertex(x+bottomPent[i].x, y+bottomPent[i].y, z+bottomPent[i].z);
        vertex(x+bottomPoint.x, y+bottomPoint.y, z+bottomPoint.z);
        vertex(x+bottomPent[i+1].x, y+bottomPent[i+1].y, z+bottomPent[i+1].z);
      } 
      else {
        vertex(x+bottomPent[i].x, y+bottomPent[i].y, z+bottomPent[i].z);
        vertex(x+bottomPoint.x, y+bottomPoint.y, z+bottomPoint.z);
        vertex(x+bottomPent[0].x, y+bottomPent[0].y, z+bottomPent[0].z);
      }
      endShape(CLOSE);
    }

    // icosahedron body
    for (int i=0; i<topPent.length; i++){
      if (i<topPent.length-2){
        beginShape();
        vertex(x+topPent[i].x, y+topPent[i].y, z+topPent[i].z);
        vertex(x+bottomPent[i+1].x, y+bottomPent[i+1].y, z+bottomPent[i+1].z);
        vertex(x+bottomPent[i+2].x, y+bottomPent[i+2].y, z+bottomPent[i+2].z);
        endShape(CLOSE);

        beginShape();
        vertex(x+bottomPent[i+2].x, y+bottomPent[i+2].y, z+bottomPent[i+2].z);
        vertex(x+topPent[i].x, y+topPent[i].y, z+topPent[i].z);
        vertex(x+topPent[i+1].x, y+topPent[i+1].y, z+topPent[i+1].z);
        endShape(CLOSE);
      } 
      else if (i==topPent.length-2){
        beginShape();
        vertex(x+topPent[i].x, y+topPent[i].y, z+topPent[i].z);
        vertex(x+bottomPent[i+1].x, y+bottomPent[i+1].y, z+bottomPent[i+1].z);
        vertex(x+bottomPent[0].x, y+bottomPent[0].y, z+bottomPent[0].z);
        endShape(CLOSE);

        beginShape();
        vertex(x+bottomPent[0].x, y+bottomPent[0].y, z+bottomPent[0].z);
        vertex(x+topPent[i].x, y+topPent[i].y, z+topPent[i].z);
        vertex(x+topPent[i+1].x, y+topPent[i+1].y, z+topPent[i+1].z);
        endShape(CLOSE);
      }
      else if (i==topPent.length-1){
        beginShape();
        vertex(x+topPent[i].x, y+topPent[i].y, z+topPent[i].z);
        vertex(x+bottomPent[0].x, y+bottomPent[0].y, z+bottomPent[0].z);
        vertex(x+bottomPent[1].x, y+bottomPent[1].y, z+bottomPent[1].z);
        endShape(CLOSE);

        beginShape();
        vertex(x+bottomPent[1].x, y+bottomPent[1].y, z+bottomPent[1].z);
        vertex(x+topPent[i].x, y+topPent[i].y, z+topPent[i].z);
        vertex(x+topPent[0].x, y+topPent[0].y, z+topPent[0].z);
        endShape(CLOSE);
      }
    }
  }

  // overrided methods fom Shape3D
  public void rotZ(float theta){
    float tx=0, ty=0, tz=0;
    // top point
    tx = cos(theta)*topPoint.x+sin(theta)*topPoint.y;
    ty = sin(theta)*topPoint.x-cos(theta)*topPoint.y;
    topPoint.x = tx;
    topPoint.y = ty;

    // bottom point
    tx = cos(theta)*bottomPoint.x+sin(theta)*bottomPoint.y;
    ty = sin(theta)*bottomPoint.x-cos(theta)*bottomPoint.y;
    bottomPoint.x = tx;
    bottomPoint.y = ty;

    // top and bottom pentagons
    for (int i=0; i<topPent.length; i++){
      tx = cos(theta)*topPent[i].x+sin(theta)*topPent[i].y;
      ty = sin(theta)*topPent[i].x-cos(theta)*topPent[i].y;
      topPent[i].x = tx;
      topPent[i].y = ty;

      tx = cos(theta)*bottomPent[i].x+sin(theta)*bottomPent[i].y;
      ty = sin(theta)*bottomPent[i].x-cos(theta)*bottomPent[i].y;
      bottomPent[i].x = tx;
      bottomPent[i].y = ty;
    }
  }

  public void rotX(float theta){
  }

  public void rotY(float theta){
  }

}
abstract class Shape3D {
  
  float x, y, z;
  float w, h, d;

  Shape3D(){ }

  Shape3D(float x, float y, float z) {
    this.x = x;
    this.y = y;
    this.z = z;
  }

  Shape3D(Vector3D p) {
    x = p.x;
    y = p.y;
    z = p.z;
  }


  Shape3D(Dimension3D dim) {
    w = dim.w;
    h = dim.h;
    d = dim.d;
  }

  Shape3D(float x, float y, float z, float w, float h, float d) {
    this.x = x;
    this.y = y;
    this.z = z;
    this.w = w;
    this.h = h;
    this.d = d;
  }

  Shape3D(float x, float y, float z, Dimension3D dim) {
    this.x = x;
    this.y = y;
    this.z = z;
    w = dim.w;
    h = dim.h;
    d = dim.d;
  }

  Shape3D(Vector3D p, Dimension3D dim) {
    x = p.x;
    y = p.y;
    z = p.z;
    w = dim.w;
    h = dim.h;
    d = dim.d;
  }

  public void setLoc(Vector3D p) {
    x=p.x;
    y=p.y;
    z=p.z;
  }

  public void setLoc(float x, float y, float z) {
    this.x=x;
    this.y=y;
    this.z=z;
  }


  // override if you need these
  public void rotX(float theta) {
  }

  public void rotY(float theta) {
  }

  public void rotZ(float theta) {
  }


  // must be implemented in subclasses
  public abstract void init();
  public abstract void create();
}
class Vector3D {
  
  float x, y, z;
  float[]origVals;

  Vector3D(){ }

  Vector3D(float x, float y, float z) {
    this.x = x;
    this.y = y;
    this.z = z;

    // capture original values
    origVals  = new float[]{ 
      x, y, z     };
  }

  //methods
  public void add(Vector3D v) {
    x+=v.x;
    y+=v.y;
    z+=v.z;
  }

  public void subtract(Vector3D v) {
    x-=v.x;
    y-=v.y;
    z-=v.z;
  }

  public void multiply(float s) {
    x*=s;
    y*=s;
    z*=s;
  }

  public void divide(float s) {
    x/=s;
    y/=s;
    z/=s;
  }

  public Vector3D getAverage(Vector3D v) {
    Vector3D u = new Vector3D();
    u.x = (x+v.x)/2;
    u.y = (y+v.y)/2;
    u.z = (z+v.z)/2;
    return u;
  }

  public void setTo(Vector3D v) {
    x = v.x;
    y = v.y;
    z = v.z;
  }

  public void reset() {
    x = origVals[0];
    y = origVals[1];
    z = origVals[2];
  }

  public float getDotProduct(Vector3D v) {
    return x*v.x + y*v.y + z*v.z;
  }

  public Vector3D getCrossProduct(Vector3D v, Vector3D u) {
    Vector3D v1 = new Vector3D(v.x-x, v.y-y, v.z-z);
    Vector3D v2 = new Vector3D(u.x-x, u.y-y, u.z-z);
    float xx = v1.y*v2.z-v1.z*v2.y;
    float yy = v1.z*v2.x-v1.x*v2.z;
    float zz = v1.x*v2.y-v1.y*v2.x;
    return new Vector3D(xx, yy, zz);
  }

  public Vector3D getNormal(Vector3D v, Vector3D u) {
    Vector3D n = getCrossProduct(v, u);
    n.normalize();
    return(n);
  }

  public void normalize() {
    float m = getMagnitude();
    x/=m;
    y/=m;
    z/=m;
  }

  public float getMagnitude() {
    return sqrt(x*x+y*y+z*z);
  }
}

  static public void main(String args[]) {
    PApplet.main(new String[] { "Icosahedra" });
  }
}
