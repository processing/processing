
// Based on code 34-05 (p. 319)


int numFrames = 37;  // The number of frames in the animation
PImage[] images = new PImage[numFrames];
int which = 0;

void setup() {
  size(200*11, 200);
  for (int i = 0; i < images.length; i++) {
    String imageName = "PT_Shifty_" + nf(i, 4) + ".gif";
    images[i] = loadImage(imageName);
  }
  noLoop();
} 


void draw() {
  background(255); 

  for(int i=0; i < 11; i++) { 
    image(images[which], i*200, 0);
    which = (which+1)%images.length;
  }
  save("page_314-1.tif");

  background(255); 

  for(int i=0; i < 11; i++) { 
    image(images[which], i*200, 0);
    which = (which+1)%images.length;
  }
  save("page_314-2.tif");

  background(255); 

  for(int i=0; i < 11; i++) { 
    image(images[which], i*200, 0);
    which = (which+1)%images.length;
  }
  save("page_314-3.tif");

  background(255); 

  for(int i=0; i < 11; i++) { 
    image(images[which], i*200, 0);
    which = (which+1)%images.length;
  }
  save("page_314-4.tif");

  background(255); 

  for(int i=0; i < 11; i++) { 
    image(images[which], i*200, 0);
    which = (which+1)%images.length;
  }
  save("page_314-5.tif");

}

