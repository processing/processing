// Planets, by Andres Colubri
//
// Sun and mercury textures from http://planetpixelemporium.com
// Star field picture from http://www.galacticimages.com/

PImage starfield;

PShape sun;
PImage suntex;

PShape planet1;
PImage surftex1;
PImage cloudtex;

PShape planet2;
PImage surftex2;

void setup() {
  size(800,  480,  P3D);
  
  starfield = loadImage("starfield.jpg");
  suntex = loadImage("sun.jpg");  
  surftex1 = loadImage("planet.jpg");  
   
  // We need trilinear sampling for this texture so it looks good
  // even when rendered very small.
  //PTexture.Parameters params1 = PTexture.newParameters(ARGB, TRILINEAR);  
  surftex2 = loadImage("mercury.jpg");  
  
  /*
  // The clouds texture will "move" having the values of its u
  // texture coordinates displaced by adding a constant increment
  // in each frame. This requires REPEAT wrapping mode so texture 
  // coordinates can be larger than 1.
  //PTexture.Parameters params2 = PTexture.newParameters();
  //params2.wrapU = REPEAT;
  cloudtex = createImage(512, 256);

  // Using 3D Perlin noise to generate a clouds texture that is seamless on
  // its edges so it can be applied on a sphere.
  cloudtex.loadPixels();
  Perlin perlin = new Perlin();
  for (int j = 0; j < cloudtex.height; j++) {
    for (int i = 0; i < cloudtex.width; i++) {
      // The angle values corresponding to each u,v pair:
      float u = float(i) / cloudtex.width;
      float v = float(j) / cloudtex.height;
      float phi = map(u, 0, 1, TWO_PI, 0); 
      float theta = map(v, 0, 1, -HALF_PI, HALF_PI);
      // The x, y, z point corresponding to these angles:
      float x = cos(phi) * cos(theta);
      float y = sin(theta);            
      float z = sin(phi) * cos(theta);      
      float n = perlin.noise3D(x, y, z, 1.2, 2, 8);
      cloudtex.pixels[j * cloudtex.width + i] = color(255, 255,  255, 255 * n * n);
    }
  }  
  cloudtex.updatePixels();
  */

  noStroke();
  fill(255);
  sphereDetail(40);

  sun = createShape(SPHERE, 150);
  sun.texture(suntex);  

  planet1 = createShape(SPHERE, 150);
  planet1.texture(surftex1);
  
  planet2 = createShape(SPHERE, 50);
  planet2.texture(surftex2);
}

void draw() {
  // Even we draw a full screen image after this, it is recommended to use
  // background to clear the screen anyways, otherwise A3D will think
  // you want to keep each drawn frame in the framebuffer, which results in 
  // slower rendering.
  background(0);
  
  // Disabling writing to the depth mask so the 
  // background image doesn't occludes any 3D object.
  hint(DISABLE_DEPTH_MASK);
  image(starfield, 0, 0, width, height);
  hint(ENABLE_DEPTH_MASK);
  
  /*
  // Displacing the u texture coordinate of layer 1 in planet
  // so it creates the effect of moving clouds.
  PShape3D p = (PShape3D)planet1;
  p.loadTexcoords(1);
  for (int i = 0; i < p.getVertexCount(); i++) {
      float u = p.texcoords[2 * i + 0];
      u += 0.002;
      p.texcoords[2 * i + 0] = u;
  }
  p.updateTexcoords();
  */
  
  pushMatrix();
  translate(width/2, height/2, -300);  
  
  pushMatrix();
  rotateY(PI * frameCount / 500);
  shape(sun);
  popMatrix();

  pointLight(255,  255,  255,  0,  0,  0);  
  rotateY(PI * frameCount / 300);
  translate(0, 0, 300);

  shape(planet2);  
  
  popMatrix();
  
  noLights();
  pointLight(255,  255,  255,  0,  0,  -150); 
  
  translate(0.75 * width,  0.6 * height,  50);
  shape(planet1);
}
