/**
 * DomeProjection
 * 
 * This sketch uses use environmental mapping to render the output 
 * on a full spherical dome.
 * 
 * Based on the FullDomeTemplate code from Christopher Warnow: 
 * https://github.com/mphasize/FullDome
 * 
 */
 
import java.nio.IntBuffer;

PShader cubemapShader;
PShape domeSphere;

IntBuffer fbo;
IntBuffer rbo;
IntBuffer envMapTextureID;

int envMapSize = 1024;   

void setup() {
  size(640, 640, P3D);
  initCubeMap();
}

void draw() {
  background(0);
  drawCubeMap();  
}

void drawScene() {  
  background(0);
  
  stroke(255, 0, 0);
  strokeWeight(2);
  for (int i = -width; i < 2 * width; i += 50) {
    line(i, -height, -100, i, 2 *height, -100);
  }
  for (int i = -height; i < 2 * height; i += 50) {
    line(-width, i, -100, 2 * width, i, -100);
  }
  
  lights();
  noStroke();
  translate(mouseX, mouseY, 200);
  rotateX(frameCount * 0.01);
  rotateY(frameCount * 0.01);  
  box(100);
}

