// Separable-blur shader (works by applying two succesive passes
// in each direction of the image)

PShader shader;
PGraphicsOpenGL src;
PGraphicsOpenGL pass1, pass2;

void setup() {
  size(200, 200, P2D);
  
  shader = ((PGraphicsOpenGL)g).loadShader("blur.glsl", PShader.TEXTURED);
  shader.set("blurSize", 9);
  shader.set("sigma", 5.0f);  
  
  src = (PGraphicsOpenGL)createGraphics(width, height, P2D); 
  
  pass1 = (PGraphicsOpenGL)createGraphics(width, height, P2D);
  pass1.noSmooth();
  pass1.setShader(shader, PShader.TEXTURED);
  
  pass2 = (PGraphicsOpenGL)createGraphics(width, height, P2D);
  pass2.noSmooth();
  pass2.setShader(shader, PShader.TEXTURED);    
}

void draw() {
  src.beginDraw();
  src.background(0);
  src.fill(255);
  src.ellipse(width/2, height/2, 100, 100);
  src.endDraw();
    
  // Applying the blur shader along the vertical direction   
  shader.set("horizontalPass", 0);
  pass1.beginDraw();                
  pass1.image(src, 0, 0);
  pass1.endDraw();
  
  // Applying the blur shader along the horizontal direction      
  shader.set("horizontalPass", 1);
  pass2.beginDraw();                
  pass2.image(pass1, 0, 0);
  pass2.endDraw();    
        
  image(pass2, 0, 0);   
}

void keyPressed() {
  if (key == '9') {
    shader.set("blurSize", 9);
    shader.set("sigma", 5.0);
  } else if (key == '7') {
    shader.set("blurSize", 7);
    shader.set("sigma", 3.0);
  } else if (key == '5') {
    shader.set("blurSize", 5);
    shader.set("sigma", 2.0);  
  } else if (key == '3') {
    shader.set("blurSize", 5);
    shader.set("sigma", 1.0);  
  }  
} 
