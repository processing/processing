/**
 * Request Image
 * by Ira Greenberg ( From Processing for Flash Developers). 
 * 
 * Shows how to use the requestImage() function with preloader animation. 
 * The requestImage() function loads images on a separate thread so that 
 * the sketch does not freeze while they load. It's very useful when you are
 * loading large images. 
 * 
 * These images are small for a quick download, but try it with your own huge 
 * images to get the full effect. 
 */

int imgCount = 12;
PImage[] imgs = new PImage[imgCount];
float imgW;

// Keeps track of loaded images (true or false)
boolean[] loadStates = new boolean[imgCount];

// For loading animation
float loaderX, loaderY, theta;

void setup() {
  size(640, 360);
  smooth();
  imgW = width/imgCount;

  // Load images asynchronously
  for (int i = 0; i < imgCount; i++){
    imgs[i] = requestImage("PT_anim"+nf(i, 4)+".gif");
  }
}

void draw(){
  background(0);
  
  // Start loading animation
  runLoaderAni();
  
  for (int i = 0; i < imgs.length; i++){
    // Check if individual images are fully loaded
    if ((imgs[i].width != 0) && (imgs[i].width != -1)){
      // As images are loaded set true in boolean array
      loadStates[i] = true;
    }
  }
  // When all images are loaded draw them to the screen
  if (checkLoadStates()){
    drawImages();
  }
}

void drawImages() {
  int y = (height - imgs[0].height) / 2;
  for (int i = 0; i < imgs.length; i++){
    image(imgs[i], width/imgs.length*i, y, imgs[i].height, imgs[i].height);
  }
}

// Loading animation
void runLoaderAni(){
  // Only run when images are loading
  if (!checkLoadStates()){
    ellipse(loaderX, loaderY, 10, 10);
    loaderX += 2;
    loaderY = height/2 + sin(theta) * (height/8);
    theta += PI/22;
    // Reposition ellipse if it goes off the screen
    if (loaderX > width + 5){
      loaderX = -5;
    }
  }
}

// Return true when all images are loaded - no false values left in array 
boolean checkLoadStates(){
  for (int i = 0; i < imgs.length; i++){
    if (loadStates[i] == false){
      return false;
    } 
  }
  return true;
}






