/**
 * Image Mask
 * 
 * Move the mouse to reveal the image through the dynamic mask.
 */
 
PShader maskShader;
PImage srcImage;
PGraphics maskImage;

void setup() {
  size(640, 360, P2D);
  srcImage = loadImage("leaves.jpg");
  maskImage = createGraphics(srcImage.width, srcImage.height, P2D);
  maskImage.noSmooth();
  maskShader = loadShader("mask.glsl");
  maskShader.set("mask", maskImage);
  background(255);
}

void draw() { 
  maskImage.beginDraw();
  maskImage.background(0);
  if (mouseX != 0 && mouseY != 0) {  
    maskImage.noStroke();
    maskImage.fill(255, 0, 0);
    maskImage.ellipse(mouseX, mouseY, 50, 50);
  }
  maskImage.endDraw();

  shader(maskShader);    
  image(srcImage, 0, 0, width, height);
}

