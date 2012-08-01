// Cubic QTVR effect, by Robert Hodgin (flight404)
//
// Elves Chasm Colorado River QTVR source images by Taylor Harnisch and Gene Cooper
// From www.panoramas.dk
// Please do not reuse this QTVR source image. It is included for example purposes only.
//
// Original Arcball code is by Ariel and V3ga. 
//
// Android port by Andres Colubri:
// * Locked in landscape mode
// * Tap in the center of the screen to go inside/outside of the cube

int xMid, yMid;

int counter         = 0;
int totalDevices    = 4;

Arcball arcball;
POV pov;
VRCube vr;

boolean outside = true;

void setup() {
  size(800, 480, A3D);
  orientation(LANDSCAPE);
  
  xMid = width/2; 
  yMid = height/2;
  
  arcball = new Arcball(float(xMid), float(yMid), height);
  // arbitrary distance for PointOfView camera. if you change the 
  // dimensions of the applet, you will need to tweak this number
  pov = new POV(400.0);  
  vr = new VRCube("coloradoCube-quarter.jpg");

  frameRate(30);
}

void draw() {
  background(20);
  noStroke();
  
  pov.exist();
  arcball.run();
  vr.exist();
}

void mousePressed(){
  if (dist(mouseX, mouseY, width/2, height/2) < 100) {
    outside = !outside;
  } else {
    arcball.mousePressed();
  }
}

void mouseDragged(){
  arcball.mouseDragged();
}

