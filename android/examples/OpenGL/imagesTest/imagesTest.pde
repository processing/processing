// Learning Processing
// Daniel Shiffman
// http://www.learningprocessing.com

// Example 15-4: Image sequence

int maxImages = 10; // Total # of images
int imageIndex = 0; // Initial image to be displayed is the first

// Declaring an array of images.
PImage[] images = new PImage[maxImages];

void setup() {
  size(640, 480, P3D);

  // Loading the images into the array
  // Don't forget to put the JPG files in the data folder!
  for (int i = 0; i < images.length; i ++ ) {
    images[i] = loadImage( "animal" + i + ".jpg" );
  }
}

void draw() {
  println(frameRate);
  background(0);

  translate(width/2, height/2,-250);
  rotateX(map(mouseY, 0, height, 0, TWO_PI));
  rotateY(map(mouseX, 0, width, 0, TWO_PI));

  imageMode(CENTER);

  translate(0,0,-250);
  for (int i = 0; i < images.length*10; i++) {
    translate(0, 0, 5);
    tint(255, 50);
    image(images[i%10], 0, 0,width,height);
  }
}

