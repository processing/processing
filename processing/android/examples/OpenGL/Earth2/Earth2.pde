// Earth
// by Mike 'Flux' Chang (cleaned up by Aaron Koblin). 
// Based on code by Toxi. 
// Android port by Andres Colubri
//
// This example shows the shape recording functionality in A3D,
// using the object-oriented mode, where the drawing calls are methods
// in the PShape3D object.

PShape3D globe;
PImage texmap;

int sDetail = 32;  // Sphere detail setting
float rotationY = 0;
float globeRadius = 450;
float pushBack = 0;

float[] cx, cz, sphereX, sphereY, sphereZ;
float sinLUT[];
float cosLUT[];
float SINCOS_PRECISION = 0.5f;
int SINCOS_LENGTH = (int)(360.0f / SINCOS_PRECISION);  

boolean usingPShape = false;

public void setup() {
  size(480, 800, P3D);
  orientation(PORTRAIT);

  PFont font = createFont(PFont.list()[0], 24);
  textFont(font, 24);

  texmap = loadImage("world32k.jpg");    
  initializeSphere(sDetail);
  
  autoNormal(false);  
  noStroke();
  
  // Everything that is drawn between beginRecord/endRecord
  // is saved into the PShape3D object.
  // Drawing the PShape3D object is much faster than redrawing
  // all the geometry again in the draw() function.
  globe = new PShape3D(this);
  globe.beginRecord();
  recTexSphere(globe, globeRadius, texmap);
  globe.endRecord();
}

public void draw() {
  background(0);            
  renderGlobe();
  
  fill(255);
  if (usingPShape) {
    text("With PShape3D. FPS: " + frameRate, 10, height - 30);  
  } else {
    text("Without PShape3D. FPS: " + frameRate, 10, height - 30);  
  }
}

void mousePressed() {
  usingPShape = !usingPShape;
}

public void renderGlobe() {
  pushMatrix();
  translate(width/2.0f, height/2.0f, pushBack);
  lights();    
  
  pushMatrix();
  
  rotateY(rotationY);  
  
  if (usingPShape) {
    shape(globe);
  } else {
    drawTexSphere(globeRadius, texmap);
  }
  
  popMatrix();  
  popMatrix();

  rotationY += 0.01;
}

public void initializeSphere(int res) {
  sinLUT = new float[SINCOS_LENGTH];
  cosLUT = new float[SINCOS_LENGTH];

  for (int i = 0; i < SINCOS_LENGTH; i++) {
    sinLUT[i] = (float) Math.sin(i * DEG_TO_RAD * SINCOS_PRECISION);
    cosLUT[i] = (float) Math.cos(i * DEG_TO_RAD * SINCOS_PRECISION);
  }

  float delta = (float)SINCOS_LENGTH/res;
  float[] cx = new float[res];
  float[] cz = new float[res];

  // Calc unit circle in XZ plane
  for (int i = 0; i < res; i++) {
    cx[i] = -cosLUT[(int) (i*delta) % SINCOS_LENGTH];
    cz[i] = sinLUT[(int) (i*delta) % SINCOS_LENGTH];
  }

  // Computing vertexlist vertexlist starts at south pole
  int vertCount = res * (res-1) + 2;
  int currVert = 0;

  // Re-init arrays to store vertices
  sphereX = new float[vertCount];
  sphereY = new float[vertCount];
  sphereZ = new float[vertCount];
  float angle_step = (SINCOS_LENGTH*0.5f)/res;
  float angle = angle_step;

  // Step along Y axis
  for (int i = 1; i < res; i++) {
    float curradius = sinLUT[(int) angle % SINCOS_LENGTH];
    float currY = -cosLUT[(int) angle % SINCOS_LENGTH];
    for (int j = 0; j < res; j++) {
      sphereX[currVert] = cx[j] * curradius;
      sphereY[currVert] = currY;
      sphereZ[currVert++] = cz[j] * curradius;
    }
    angle += angle_step;
  }
  sDetail = res;
}

// Generic routine to draw textured sphere
void drawTexSphere(float r, PImage t) {
  int v1,v11,v2;
  r = (r + 240 ) * 0.33;
  beginShape(TRIANGLE_STRIP);
  texture(t);
  float iu=(float)(t.width-1)/(sDetail);
  float iv=(float)(t.height-1)/(sDetail);
  float u=0,v=iv;
  for (int i = 0; i < sDetail; i++) {
    normal(0, -1, 0);
    vertex(0, -r, 0,u,0);
    normal(sphereX[i], sphereY[i], sphereZ[i]);
    vertex(sphereX[i]*r, sphereY[i]*r, sphereZ[i]*r, u, v);
    u+=iu;
  }
  vertex(0, -r, 0,u,0);
  normal(sphereX[0], sphereY[0], sphereZ[0]);
  vertex(sphereX[0]*r, sphereY[0]*r, sphereZ[0]*r, u, v);
  endShape();   
  
  // Middle rings
  int voff = 0;
  for(int i = 2; i < sDetail; i++) {
    v1=v11=voff;
    voff += sDetail;
    v2=voff;
    u=0;
    beginShape(TRIANGLE_STRIP);
    texture(t);
    for (int j = 0; j < sDetail; j++) {
      normal(sphereX[v1], sphereY[v1], sphereZ[v1]);
      vertex(sphereX[v1]*r, sphereY[v1]*r, sphereZ[v1++]*r, u, v);
      normal(sphereX[v2], sphereY[v2], sphereZ[v2]);
      vertex(sphereX[v2]*r, sphereY[v2]*r, sphereZ[v2++]*r, u, v+iv);
      u+=iu;
    }
  
    // Close each ring
    v1=v11;
    v2=voff;
    normal(sphereX[v1], sphereY[v1], sphereZ[v1]);
    vertex(sphereX[v1]*r, sphereY[v1]*r, sphereZ[v1]*r, u, v);
    normal(sphereX[v2], sphereY[v2], sphereZ[v2]);
    vertex(sphereX[v2]*r, sphereY[v2]*r, sphereZ[v2]*r, u, v+iv);
    endShape();
    v+=iv;
  }
  u=0;
  
  // Add the northern cap
  beginShape(TRIANGLE_STRIP);
  texture(t);
  for (int i = 0; i < sDetail; i++) {
    v2 = voff + i;
    normal(sphereX[v2], sphereY[v2], sphereZ[v2]);
    vertex(sphereX[v2]*r, sphereY[v2]*r, sphereZ[v2]*r, u, v);
    normal(0, 1, 0);
    vertex(0, r, 0, u, v+iv); 
    u+=iu;
  }
  normal(sphereX[voff], sphereY[voff], sphereZ[voff]);
  vertex(sphereX[voff]*r, sphereY[voff]*r, sphereZ[voff]*r, u, v);

  endShape(); 
}

void recTexSphere(PShape3D obj, float r, PImage t) {
  int v1,v11,v2;
  r = (r + 240 ) * 0.33;
  obj.beginShape(TRIANGLE_STRIP);
  obj.texture(t);
  float iu=(float)(t.width-1)/(sDetail);
  float iv=(float)(t.height-1)/(sDetail);
  float u=0,v=iv;
  for (int i = 0; i < sDetail; i++) {
    obj.normal(0, -1, 0);
    obj.vertex(0, -r, 0,u,0);
    obj.normal(sphereX[i], sphereY[i], sphereZ[i]);
    obj.vertex(sphereX[i]*r, sphereY[i]*r, sphereZ[i]*r, u, v);
    u+=iu;
  }
  obj.vertex(0, -r, 0,u,0);
  obj.normal(sphereX[0], sphereY[0], sphereZ[0]);
  obj.vertex(sphereX[0]*r, sphereY[0]*r, sphereZ[0]*r, u, v);
  obj.endShape();   
  
  // Middle rings
  int voff = 0;
  for(int i = 2; i < sDetail; i++) {
    v1=v11=voff;
    voff += sDetail;
    v2=voff;
    u=0;
    obj.beginShape(TRIANGLE_STRIP);
    obj.texture(t);
    for (int j = 0; j < sDetail; j++) {
      normal(sphereX[v1], sphereY[v1], sphereZ[v1]);
      obj.vertex(sphereX[v1]*r, sphereY[v1]*r, sphereZ[v1++]*r, u, v);
      obj.normal(sphereX[v2], sphereY[v2], sphereZ[v2]);
      obj.vertex(sphereX[v2]*r, sphereY[v2]*r, sphereZ[v2++]*r, u, v+iv);
      u+=iu;
    }
  
    // Close each ring
    v1=v11;
    v2=voff;
    obj.normal(sphereX[v1], sphereY[v1], sphereZ[v1]);
    obj.vertex(sphereX[v1]*r, sphereY[v1]*r, sphereZ[v1]*r, u, v);
    obj.normal(sphereX[v2], sphereY[v2], sphereZ[v2]);
    obj.vertex(sphereX[v2]*r, sphereY[v2]*r, sphereZ[v2]*r, u, v+iv);
    obj.endShape();
    v+=iv;
  }
  u=0;
  
  // Add the northern cap
  obj.beginShape(TRIANGLE_STRIP);
  obj.texture(t);
  for (int i = 0; i < sDetail; i++) {
    v2 = voff + i;
    obj.normal(sphereX[v2], sphereY[v2], sphereZ[v2]);
    obj.vertex(sphereX[v2]*r, sphereY[v2]*r, sphereZ[v2]*r, u, v);
    obj.normal(0, 1, 0);
    obj.vertex(0, r, 0, u, v+iv); 
    u+=iu;
  }
  obj.normal(sphereX[voff], sphereY[voff], sphereZ[voff]);
  obj.vertex(sphereX[voff]*r, sphereY[voff]*r, sphereZ[voff]*r, u, v);

  obj.endShape(); 
}
