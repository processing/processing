import processing.core.*; 
import processing.xml.*; 

import processing.opengl2.*; 

import java.applet.*; 
import java.awt.Dimension; 
import java.awt.Frame; 
import java.awt.event.MouseEvent; 
import java.awt.event.KeyEvent; 
import java.awt.event.FocusEvent; 
import java.awt.Image; 
import java.io.*; 
import java.net.*; 
import java.text.*; 
import java.util.*; 
import java.util.zip.*; 
import java.util.regex.*; 

public class Planets extends PApplet {

// Planets, by Andres Colubri
// This example uses the beginRecord/endRecord method to save
// an entire shape geometry into a PShape3D object for faster 
// drawing. It also demonstrates mulitexturing and displacement 
// of texture coordinates
// Sun and mercury textures from http://planetpixelemporium.com
// Star field picture from http://www.galacticimages.com/



PImage starfield;

PShape sun;
PImage suntex;

PShape planet1;
PImage surftex1;
PImage cloudtex;

PShape planet2;
PImage surftex2;

public void setup() {
  size(480,  800,  OPENGL2);
  //orientation(PORTRAIT);
  
  starfield = loadImage("starfield.jpg");
  suntex = loadImage("sun.jpg");  
  surftex1 = loadImage("planet.jpg");  
   
  // We need trilinear sampling for this texture so it looks good
  // even when rendered very small.
  PTexture.Parameters params1 = PTexture.newParameters(ARGB, TRILINEAR);  
  surftex2 = loadImage("mercury.jpg", params1);  
  
  // The clouds texture will "move" having the values of its u
  // texture coordinates displaced by adding a constant increment
  // in each frame. This requires REPEAT wrapping mode so texture 
  // coordinates can be larger than 1.
  PTexture.Parameters params2 = PTexture.newParameters();
  params2.wrapU = REPEAT;
  cloudtex = createImage(512, 256, ARGB, params2);

  // Using 3D Perlin noise to generate a clouds texture that is seamless on
  // its edges so it can be applied on a sphere.
  println("Generating clouds texture. It takes some time, please wait..."); 
  cloudtex.loadPixels();
  Perlin perlin = new Perlin();
  for (int j = 0; j < cloudtex.height; j++) {
    for (int i = 0; i < cloudtex.width; i++) {
      // The angle values corresponding to each u,v pair:
      float u = PApplet.parseFloat(i) / cloudtex.width;
      float v = PApplet.parseFloat(j) / cloudtex.height;
      float phi = map(u, 0, 1, TWO_PI, 0); 
      float theta = map(v, 0, 1, -HALF_PI, HALF_PI);
      // The x, y, z point corresponding to these angles:
      float x = cos(phi) * cos(theta);
      float y = sin(theta);            
      float z = sin(phi) * cos(theta);      
      float n = perlin.noise3D(x, y, z, 1.2f, 2, 8);
      cloudtex.pixels[j * cloudtex.width + i] = color(255, 255,  255, 255 * n * n);
    }
  }  
  cloudtex.updatePixels();
  println("Done.");

  textureMode(NORMAL);
  noStroke();
  fill(255);

  sun = beginShapeRecord();  
  drawSphere(150, 40, suntex, null);
  endShapeRecord();  

  planet1 = beginShapeRecord();
  drawSphere(150, 40, surftex1, cloudtex);
  endShapeRecord();
  
  planet2 = beginShapeRecord();
  drawSphere(50, 20, surftex2, null);
  endShapeRecord();
}

public void draw() {
  // Even we draw a full screen image after this, it is recommended to use
  // background to clear the screen anyways, otherwise A3D will think
  // you want to keep each drawn frame in the framebuffer, which results in 
  // slower rendering.
  background(0);
  
  // Disabling writing to the depth mask so the 
  // background image doesn't occludes any 3D object.
  hint(DISABLE_DEPTH_MASK);
  image(starfield, 0, 0, width, height);
  hint(ENABLE_DEPTH_MASK);
  
  // Displacing the u texture coordinate of layer 1 in planet
  // so it creates the effect of moving clouds.
  PShape3D p = (PShape3D)planet1;
  p.loadTexcoords(1);
  for (int i = 0; i < p.getVertexCount(); i++) {
      float u = p.texcoords[2 * i + 0];
      u += 0.002f;
      p.texcoords[2 * i + 0] = u;
  }
  p.updateTexcoords();

  pushMatrix();
  translate(width/2, height/2, -100);  
  
  pushMatrix();
  rotateY(PI * frameCount / 500);
  shape(sun);
  popMatrix();

  pointLight(255,  255,  255,  0,  0,  0);  
  rotateY(PI * frameCount / 300);
  translate(0, 0, 300);

  shape(planet2);  
  
  popMatrix();
  
  noLights();
  pointLight(255,  255,  255,  0,  0,  100); 
  
  translate(0.75f * width,  0.6f * height,  350);
  shape(planet1);
}

// Implementation of 1D, 2D, and 3D Perlin noise. Based on the 
// C code by Paul Bourke:
// http://local.wasp.uwa.edu.au/~pbourke/texture_colour/perlin/
class Perlin {
  int B = 0x100;
  int BM = 0xff;
  int N = 0x1000;
  int NP = 12; 
  int NM = 0xfff;

  int p[];
  float g3[][];
  float g2[][];
  float g1[];

  public void normalize2(float v[]) {
    float s = sqrt(v[0] * v[0] + v[1] * v[1]);
    v[0] = v[0] / s;
    v[1] = v[1] / s;
  }

  public void normalize3(float v[]) {
    float s = sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
    v[0] = v[0] / s;
    v[1] = v[1] / s;
    v[2] = v[2] / s;
  }

  public float sCurve(float t) {
    return t * t * (3.0f - 2.0f * t);
  }

  public float at2(float q[], float rx, float ry) { 
    return rx * q[0] + ry * q[1];
  }

  public float at3(float q[], float rx, float ry, float rz) { 
    return rx * q[0] + ry * q[1] + rz * q[2];
  }

  Perlin() {
    p = new int[B + B + 2];
    g3 = new float[B + B + 2][3];
    g2 = new float[B + B + 2][2];
    g1 = new float[B + B + 2];  

    init();
  }  

  public void init() {
    int i, j, k;

    for (i = 0 ; i < B ; i++) {
      p[i] = i;
      g1[i] = (random(B + B) - B) / B;

      for (j = 0 ; j < 2 ; j++)
        g2[i][j] = (random(B + B) - B) / B;
      normalize2(g2[i]);

      for (j = 0 ; j < 3 ; j++)
        g3[i][j] = (random(B + B) - B) / B;
      normalize3(g3[i]);
    }

    while (0 < --i) {
      k = p[i];
      p[i] = p[j = PApplet.parseInt(random(B))];
      p[j] = k;
    }

    for (i = 0 ; i < B + 2 ; i++) {
      p[B + i] = p[i];
      g1[B + i] = g1[i];
      for (j = 0 ; j < 2 ; j++)
        g2[B + i][j] = g2[i][j];
      for (j = 0 ; j < 3 ; j++)
        g3[B + i][j] = g3[i][j];
    }
  }

  public float noise1(float[] vec) {
    int bx0, bx1;
    float rx0, rx1, sx, t, u, v;

    t = vec[0] + N;
    bx0 = PApplet.parseInt(t) & BM;
    bx1 = (bx0 + 1) & BM;
    rx0 = t - PApplet.parseInt(t);
    rx1 = rx0 - 1.0f; 

    sx = sCurve(rx0);
    u = rx0 * g1[p[bx0]];
    v = rx1 * g1[p[bx1]];

    return lerp(u, v, sx);
  }

  public float noise2(float[] vec) {
    int bx0, bx1, by0, by1, b00, b10, b01, b11;
    float rx0, rx1, ry0, ry1, sx, sy, a, b, t, u, v;
    float[] q;    
    int i, j;

    t = vec[0] + N;
    bx0 = PApplet.parseInt(t) & BM;
    bx1 = (bx0 + 1) & BM;
    rx0 = t - PApplet.parseInt(t);
    rx1 = rx0 - 1.0f; 

    t = vec[1] + N;
    by0 = PApplet.parseInt(t) & BM;
    by1 = (by0 + 1) & BM;
    ry0 = t - PApplet.parseInt(t);
    ry1 = ry0 - 1.0f;

    i = p[bx0];
    j = p[bx1];

    b00 = p[i + by0];
    b10 = p[j + by0];
    b01 = p[i + by1];
    b11 = p[j + by1];

    sx = sCurve(rx0);
    sy = sCurve(ry0);

    q = g2[b00]; 
    u = at2(q, rx0, ry0);
    q = g2[b10]; 
    v = at2(q, rx1, ry0);
    a = lerp(u, v, sx);

    q = g2[b01] ; 
    u = at2(q, rx0, ry1);
    q = g2[b11] ; 
    v = at2(q, rx1, ry1);
    b = lerp(u, v, sx);

    return lerp(a, b, sy);
  }

  public float noise3(float[] vec) {
    int bx0, bx1, by0, by1, bz0, bz1, b00, b10, b01, b11;
    float rx0, rx1, ry0, ry1, rz0, rz1, sy, sz, a, b, c, d, t, u, v;
    float[] q;
    int i, j;

    t = vec[0] + N;
    bx0 = PApplet.parseInt(t) & BM;
    bx1 = (bx0 + 1) & BM;
    rx0 = t - PApplet.parseInt(t);
    rx1 = rx0 - 1.0f;

    t = vec[1] + N;
    by0 = PApplet.parseInt(t) & BM;
    by1 = (by0 + 1) & BM;
    ry0 = t - PApplet.parseInt(t);
    ry1 = ry0 - 1.0f;

    t = vec[2] + N;
    bz0 = PApplet.parseInt(t) & BM;
    bz1 = (bz0 + 1) & BM;
    rz0 = t - PApplet.parseInt(t);
    rz1 = rz0 - 1.0f;

    i = p[bx0];
    j = p[bx1];

    b00 = p[i + by0];
    b10 = p[j + by0];
    b01 = p[i + by1];
    b11 = p[j + by1];

    t  = sCurve(rx0);
    sy = sCurve(ry0);
    sz = sCurve(rz0);

    q = g3[b00 + bz0]; 
    u = at3(q, rx0, ry0, rz0);
    q = g3[b10 + bz0]; 
    v = at3(q, rx1, ry0, rz0);
    a = lerp(u, v, t);

    q = g3[b01 + bz0]; 
    u = at3(q, rx0, ry1, rz0);
    q = g3[b11 + bz0]; 
    v = at3(q, rx1, ry1, rz0);
    b = lerp(u, v, t);

    c = lerp(a, b, sy);

    q = g3[b00 + bz1]; 
    u = at3(q, rx0, ry0, rz1);
    q = g3[b10 + bz1]; 
    v = at3(q, rx1, ry0, rz1);
    a = lerp(u, v, t);

    q = g3[b01 + bz1]; 
    u = at3(q, rx0, ry1, rz1);
    q = g3[b11 + bz1]; 
    v = at3(q, rx1, ry1, rz1);
    b = lerp(u, v, t);

    d = lerp(a, b, sy);

    return lerp(c, d, sz);
  }

  // In what follows "nalpha" is the weight when the sum is formed.
  // Typically it is 2, as this approaches 1 the function is noisier.
  // "nbeta" is the harmonic scaling/spacing, typically 2. n is the
  // number of harmonics added up in the final result. Higher number 
  // results in more detailed noise.

  public float noise1D(float x, float nalpha, float nbeta, int n) {
    float val, sum = 0;
    float v[] = {x};
    float nscale = 1;

    for (int i = 0; i < n; i++) {
      val = noise1(v);
      sum += val / nscale;
      nscale *= nalpha;
      v[0] *= nbeta;
    }
    return sum;
  }

  public float noise2D(float x, float y, float nalpha, float nbeta, int n) {
   float val,sum = 0;
   float v[] = {x, y};
   float nscale = 1;

   for (int i = 0; i < n; i++) {
      val = noise2(v);
      sum += val / nscale;
      nscale *= nalpha;
      v[0] *= nbeta;
      v[1] *= nbeta;
   }
   return sum;
  }

  public float noise3D(float x, float y, float z, float nalpha, float nbeta, int n) {
    float val, sum = 0;
    float v[] = {x, y, z};
    float nscale = 1;

    for (int i = 0 ; i < n; i++) {
      val = noise3(v);
      sum += val / nscale;
      nscale *= nalpha;
      v[0] *= nbeta;
      v[1] *= nbeta;
      v[2] *= nbeta;
    }
    return sum;
  }
}

// Just draws an sphere of the given radius and resolutoin, using up to
// two images for texturing.
public void drawSphere(float r, int n, PImage tex0, PImage tex1) {
  float startLat = -90;
  float startLon = 0.0f;

  float latInc = 180.0f / n;
  float lonInc = 360.0f / n;

  float u,  v;
  float phi1,  phi2;
  float theta1,  theta2;
  PVector p0 = new PVector();
  PVector p1 = new PVector();
  PVector p2 = new PVector();
  beginShape(TRIANGLES);
  if (tex1 != null) {
    texture(tex0, tex1);
  } else {
    texture(tex0);
  }

  for (int col = 0; col < n; col++) {
    phi1 = (startLon + col * lonInc) * DEG_TO_RAD;
    phi2 = (startLon + (col + 1) * lonInc) * DEG_TO_RAD;
    for (int row = 0; row < n; row++) {
      theta1 = (startLat + row * latInc) * DEG_TO_RAD;
      theta2 = (startLat + (row + 1) * latInc) * DEG_TO_RAD;

      p0.x = cos(phi1) * cos(theta1);
      p0.y = sin(theta1);            
      p0.z = sin(phi1) * cos(theta1);

      p1.x = cos(phi1) * cos(theta2);
      p1.y = sin(theta2);            
      p1.z = sin(phi1) * cos(theta2);

      p2.x = cos(phi2) * cos(theta2);
      p2.y = sin(theta2);            
      p2.z = sin(phi2) * cos(theta2);

      normal(p0.x,  p0.y,  p0.z);     
      u = map(phi1, TWO_PI, 0, 0, 1);
      v = map(theta1, -HALF_PI, HALF_PI, 0, 1);
      vertex(r * p0.x,  r * p0.y,  r * p0.z,  u,  v);
 
      normal(p1.x,  p1.y,  p1.z);
      u = map(phi1, TWO_PI, 0, 0, 1);
      v = map(theta2, -HALF_PI, HALF_PI, 0, 1);
      vertex(r * p1.x,  r * p1.y,  r * p1.z,  u,  v);

      normal(p2.x,  p2.y,  p2.z);
      u = map(phi2, TWO_PI, 0, 0, 1);
      v = map(theta2, -HALF_PI, HALF_PI, 0, 1);      
      vertex(r * p2.x,  r * p2.y,  r * p2.z,  u,  v);

      p1.x = cos(phi2) * cos(theta1);
      p1.y = sin(theta1);            
      p1.z = sin(phi2) * cos(theta1);

      normal(p0.x,  p0.y,  p0.z);
      u = map(phi1, TWO_PI, 0, 0, 1);
      v = map(theta1, -HALF_PI, HALF_PI, 0, 1);      
      vertex(r * p0.x,  r * p0.y,  r * p0.z,  u,  v);

      normal(p2.x,  p2.y,  p2.z);
      u = map(phi2, TWO_PI, 0, 0, 1);
      v = map(theta2, -HALF_PI, HALF_PI, 0, 1);            
      vertex(r * p2.x,  r * p2.y,  r * p2.z,  u,  v);

      normal(p1.x,  p1.y,  p1.z);
      u = map(phi2, TWO_PI, 0, 0, 1);
      v = map(theta1, -HALF_PI, HALF_PI, 0, 1);            
      vertex(r * p1.x,  r * p1.y,  r * p1.z,  u,  v);
    }
  }

  endShape();  
}
  static public void main(String args[]) {
    PApplet.main(new String[] { "--bgcolor=#FFFFFF", "Planets" });
  }
}
