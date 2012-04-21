/**
 * Textured Sphere 
 * by Mike 'Flux' Chang (cleaned up by Aaron Koblin). 
 * Based on code by Toxi. 
 * 
 * A 3D textured sphere with simple rotation control.
 * Note: Controls will be inverted when sphere is upside down. 
 * Use an "arc ball" to deal with this appropriately.
 */ 

PShape globe;
PImage texmap;

int sDetail = 35;  // Sphere detail setting
float rotationX = 0;
float rotationY = 0;
float velocityX = 0;
float velocityY = 0;
float globeRadius = 450;
float pushBack = -300;

float[] cx, cz, sphereX, sphereY, sphereZ;
float sinLUT[];
float cosLUT[];
float SINCOS_PRECISION = 0.5;
int SINCOS_LENGTH = int(360.0 / SINCOS_PRECISION);


void setup() {
  size(1024, 768, P3D);  
  smooth();
  
  sphereDetail(sDetail);
  texmap = loadImage("world32k.jpg");
  globe = createShape(SPHERE, globeRadius);    
  globe.noStroke();
  globe.texture(texmap);  
}

void draw() {    
  background(0);            
  renderGlobe(); 
}

void renderGlobe() {
  translate(width/2.0, height/2.0, pushBack);
  lights();    
  rotateX(radians(-rotationX));  
  rotateY(radians(-rotationY));
  shape(globe);

  rotationX += velocityX;
  rotationY += velocityY;
  velocityX *= 0.95;
  velocityY *= 0.95;
  
  // Implements mouse control (interaction will be inverse when sphere is  upside down)
  if(mousePressed){
    velocityX += (mouseY-pmouseY) * 0.01;
    velocityY -= (mouseX-pmouseX) * 0.01;
  }
}