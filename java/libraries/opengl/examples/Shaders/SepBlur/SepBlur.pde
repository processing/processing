// Separable-blur shader (works by applying two successive passes
// in each direction of the image)

PShader blur;
PGraphics src;
PGraphics pass1, pass2;

void setup() {
  size(200, 200, P2D);
  
  blur = (PShader)loadShader("blur.glsl", PShader.TEXTURED);
  blur.set("blurSize", 9);
  blur.set("sigma", 5.0f);  
  
  src = createGraphics(width, height, P2D); 
  
  pass1 = createGraphics(width, height, P2D);
  pass1.noSmooth();
  pass1.shader(blur, PShader.TEXTURED);
  
  pass2 = createGraphics(width, height, P2D);
  pass2.noSmooth();
  pass2.shader(blur, PShader.TEXTURED);    
}

void draw() {
  src.beginDraw();
  src.background(0);
  src.fill(255);
  src.ellipse(width/2, height/2, 100, 100);
  src.endDraw();
    
  // Applying the blur shader along the vertical direction   
  blur.set("horizontalPass", 0);
  pass1.beginDraw();                
  pass1.image(src, 0, 0);
  pass1.endDraw();
  
  // Applying the blur shader along the horizontal direction      
  blur.set("horizontalPass", 1);
  pass2.beginDraw();                
  pass2.image(pass1, 0, 0);
  pass2.endDraw();    
        
  image(pass2, 0, 0);   
}

void keyPressed() {
  if (key == '9') {
    blur.set("blurSize", 9);
    blur.set("sigma", 5.0);
  } else if (key == '7') {
    blur.set("blurSize", 7);
    blur.set("sigma", 3.0);
  } else if (key == '5') {
    blur.set("blurSize", 5);
    blur.set("sigma", 2.0);  
  } else if (key == '3') {
    blur.set("blurSize", 5);
    blur.set("sigma", 1.0);  
  }  
} 