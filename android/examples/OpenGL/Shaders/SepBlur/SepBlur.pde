// Separable-blur shader (works by applying two successive passes
// in each direction of the image)

PShader blur;
PGraphics src;
PGraphics pass1, pass2;

void setup() {
  size(200, 200, P2D);
  
  blur = loadShader(PShader.TEXTURED, "blur.glsl");
  blur.set("sigma", 5.0f);  
  
  src = createGraphics(width, height, P2D); 
  
  pass1 = createGraphics(width, height, P2D);
  pass1.noSmooth();
  pass1.shader(blur);
  
  pass2 = createGraphics(width, height, P2D);
  pass2.noSmooth();
  pass2.shader(blur);    
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

