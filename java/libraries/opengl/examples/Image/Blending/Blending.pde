// Blending, by Andres Colubri
// Images can be blended using one of the 10 blending modes 
// available in OPENGL2.   
// Images by Kevin Bjorke.

PImage pic1, pic2;
int selMode = REPLACE;
String name = "replace";
int picAlpha = 255;

void setup() {
  size(800, 480, P3D);
  
  PFont font = createFont(PFont.list()[0], 20);
  textFont(font, 20);    
  
  pic1 = loadImage("bjorke1.jpg");
  pic2 = loadImage("bjorke2.jpg"); 
}

void draw() {
  background(0);
  
  tint(255, 255);
  image(pic1, 0, 0, pic1.width, pic1.height);
  
  blendMode(selMode);  
  tint(255, picAlpha);
  image(pic2, 0, 0, pic2.width, pic2.height);
    
  blendMode(REPLACE);    
  fill(200, 50, 50);
  rect(0, height - 50, map(picAlpha, 0, 255, 0, width), 50);
  fill(255);

  text("Selected blend mode: " + name + ". Click to move to next", 10, pic1.height + 30);
  text("Drag this bar to change alpha of image", 10, height - 18);
}

void mousePressed() {
  if (height - 50 < mouseY) return;
  
  if (selMode == REPLACE) { 
    selMode = BLEND;
    name = "blend";
  } else if (selMode == BLEND) { 
    selMode = ADD;
    name = "add";
  } else if (selMode == ADD) { 
    selMode = SUBTRACT;
    name = "subtract";
  } else if (selMode == SUBTRACT) { 
    selMode = LIGHTEST;
    name = "lightest";
  } else if (selMode == LIGHTEST) { 
    selMode = DARKEST;
    name = "darkest";
  } else if (selMode == DARKEST) { 
    selMode = DIFFERENCE;
    name = "difference";
  } else if (selMode == DIFFERENCE) { 
    selMode = EXCLUSION;  
    name = "exclusion";
  } else if (selMode == EXCLUSION) { 
    selMode = MULTIPLY;  
    name = "multiply";
  } else if (selMode == MULTIPLY) { 
    selMode = SCREEN;
    name = "screen";
  } else if (selMode == SCREEN) { 
    selMode = REPLACE;
    name = "replace";
  }
}

void mouseDragged() {
  if (height - 50 < mouseY) {
    picAlpha = int(map(mouseX, 0, width, 0, 255));
  }
}