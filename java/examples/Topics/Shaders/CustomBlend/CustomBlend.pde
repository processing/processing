/**
 * Custom Blend
 *
 * The OpenGL-based renderers (P2D and P3D) only support some of the
 * blending modes available in the default renderer. The reason for this 
 * is that the blend equations in OpenGL allow for combinations of the 
 * form dest_factor * dest_color + src_factor * src_color of the source and 
 * destination colors (see this page http://www.opengl.org/wiki/Blending
 * for an extensive discussion of blending in OpenGL). 
 * Complex blending modes typically available in photo editing tools, 
 * like hard light or dodge, cannot be modeled with those equations.
 * However, we can implement virtually any blending math directly in the
 * fragment shader. 
 * 
 * This example shows how custom blend shaders can be loaded and used in 
 * Processing.
 * For detailed information on how to implement Photoshop-like blending modes, 
 * check the following pages (a bit old but still useful):
 * http://www.pegtop.net/delphi/articles/blendmodes/index.htm
 * http://mouaif.wordpress.com/2009/01/05/photoshop-math-with-glsl-shaders/ 
 *
 */

PImage destImage;
PImage srcImage;
PShader dodge;
PShader burn;
PShader overlay;
PShader difference;

void setup() {
  size(640, 360, P2D);
  destImage = loadImage("leaves.jpg");
  srcImage = loadImage("moonwalk.jpg");
  
  initShaders();  
}

void draw() {
  background(0);
    
  shader(dodge);
  drawOutput(0, 0, width/2, height/2);
  shader(burn);
  drawOutput(width/2, 0, width/2, height/2);
  shader(overlay);
  drawOutput(0, height/2, width/2, height/2);
  shader(difference);
  drawOutput(width/2, height/2, width/2, height/2);
  
  noLoop();
}

void initShaders() {
  dodge = loadShader("dodge.glsl");
  burn = loadShader("burn.glsl");
  overlay = loadShader("overlay.glsl");
  difference = loadShader("difference.glsl");    
  
  // The names destination and source come from the OpenGL terminology: 
  // destination from the image already in the framebuffer, or "base layer",
  // and source for the image that will be blended into the framebuffer, or
  // "blend layer":    
  dodge.set("destSampler", destImage);
  dodge.set("srcSampler", srcImage);
  burn.set("destSampler", destImage);
  burn.set("srcSampler", srcImage);
  overlay.set("destSampler", destImage);
  overlay.set("srcSampler", srcImage);
  difference.set("destSampler", destImage);
  difference.set("srcSampler", srcImage); 
  
  // We set the sizes of de  st and src images, and the rectangular areas 
  // from the images that we will use for blending:
  dodge.set("destSize", 640, 360);
  dodge.set("destRect", 100, 50, 200, 200);  
  burn.set("destSize", 640, 360);
  burn.set("destRect", 100, 50, 200, 200);  
  overlay.set("destSize", 640, 360);
  overlay.set("destRect", 100, 50, 200, 200);  
  difference.set("destSize", 640, 360);
  difference.set("destRect", 100, 50, 200, 200);  
  
  dodge.set("srcSize", 640, 360);  
  dodge.set("srcRect", 0, 0, 640, 360);
  burn.set("srcSize", 640, 360);  
  burn.set("srcRect", 0, 0, 640, 360);
  overlay.set("srcSize", 640, 360);  
  overlay.set("srcRect", 0, 0, 640, 360);
  difference.set("srcSize", 640, 360);  
  difference.set("srcRect", 0, 0, 640, 360);  
}

void drawOutput(float x, float y, float w, float h) {
  pushMatrix();
  translate(x, y);
  noStroke();
  beginShape(QUAD);
  // Although we are not associating a texture to 
  // this shape, the uv coordinates will be stored
  // anyways so they can be used in the fragment 
  // shader to access the destination and source
  // images.
  vertex(0, 0, 0, 0);
  vertex(w, 0, 1, 0);
  vertex(w, h, 1, 1);
  vertex(0, h, 0, 1);
  endShape();
  popMatrix();  
}
