/**
 * Wormhole Demo Effect 
 * by luis2048.
 * 
 * A funnel-shaped hole sucking its texture to the middle.
 * The effect is accomplished like the tunnel effect but with 
 * a 15 x 15 texture and static lookup table. The texture is shifted 
 * and mapped to the static lookup table. 
 */

PImage wormImg, wormTexture;
int[] reg = new int[15];

void setup() {
  size(640, 360, P2D);
  noSmooth();

  // Reference image used to transpose texture 
  wormImg = loadImage("wormhole.png");
  wormImg.resize(width, height);
  wormImg.loadPixels();

  // Texture image array
  wormTexture = loadImage("texture.gif");
  wormTexture.loadPixels();
}

// Moves the bottom row of pixels to the top and shifting remaining pixels 1 over
void shiftup() {
  for (int k = 0; k < 15; k++) {
    reg[k] = wormTexture.pixels[k];
  }

  for (int k = 15; k < 225; k++) {
    wormTexture.pixels[k-15] = wormTexture.pixels[k];
  }
  for (int k = 0; k < 15; k++) {
    wormTexture.pixels[k+210] = reg[k];
  }
}

// Moves left column of pixels to the right and shifting remaining pixels 1 over
void shiftright() {
  for(int k = 0; k < 15; k++) {
    reg[k] = wormTexture.pixels[15*k+14];
    for(int i = 14;i > 0; i--) {
        wormTexture.pixels[15*k+i] = wormTexture.pixels[15*k+(i-1)];
    }
    wormTexture.pixels[15*k] = reg[k];
  }
}

void draw() {
  // Load pixel data array
  loadPixels();

  // Loop through all pixels
  for (int i = 0; i < pixels.length; i++){
    // Map texture to wormhole in a bit shift blue 
    pixels[i] = wormTexture.pixels[constrain(wormImg.pixels[i] & 0xFF, 0, 224)];
  }  

  updatePixels();

  shiftright();
  shiftup();
}



