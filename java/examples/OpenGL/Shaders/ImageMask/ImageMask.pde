PImage srcImage;
PGraphics maskImage;
PShader maskShader;
  
void setup() {
  size(200, 200, P2D);
  srcImage = loadImage("milan_rubbish.jpg");
  maskImage = createGraphics(srcImage.width, srcImage.height, P2D);
  maskImage.noSmooth();
  maskShader = loadShader("mask.glsl");
  maskShader.set("maskSampler", maskImage);
}
    
void draw() {    
  maskImage.beginDraw();
  maskImage.background(0);
  maskImage.noStroke();
  maskImage.fill(255, 0, 0);
  maskImage.ellipse(mouseX, mouseY, 30, 30);
  maskImage.endDraw();
    
  shader(maskShader);    
  image(srcImage, 0, 0, width, height);
  resetShader();
}