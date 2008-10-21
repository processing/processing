/**
 * Request Image
 * by Ira Greenberg. 
 * From Processing for Flash Developers, Friends of ED, 2009. 
 * 
 * Shows how to use the requestImage() function with preloader animation. 
 * The requestImage() function loads images on a separate thread so that 
 * the sketch does not freeze while they load. It's very useful when you are
 * loading large images, as this example demonstrates. 
 * 
 * To work, this example requires 10 images named dublin0.jpg ... dublin9.jpg 
 * in the sketch data directory. To save space, these images are not included 
 * with the example.
 */

int imgCount = 10;
PImage[] imgs = new PImage[imgCount];
float imgW;

// Keeps track of loaded images (true or false)
boolean[] loadStates = new boolean[imgCount];

// For loading animation
float loaderX, loaderY, theta;

void setup() {
  size(800, 60);
  smooth();
  imgW = width/imgCount;

  // Load images asynchronously
  for (int i = 0; i < imgCount; i++){
    imgs[i] = requestImage("dublin"+i+".jpg");
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

void drawImages(){
  for (int i = 0; i < imgs.length; i++){
    image(imgs[i], width/10*i, 0, imgW, height);
  }
}

// loading animation
void runLoaderAni(){
  // Only run when images are loading
  if (!checkLoadStates()){
    ellipse(loaderX, loaderY, 10, 10);
    loaderX += 2;
    loaderY = height/2 + sin(theta) * (height/2.5);
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






