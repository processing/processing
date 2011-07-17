/**
 * Simple Linear Gradient 
 * by Ira Greenberg. 
 * 
 * Using the convenient red(), green() 
 * and blue() component functions,
 * generate some linear gradients.
 */

// constants
int Y_AXIS = 1;
int X_AXIS = 2;
color b1, b2, c1, c2, c3, c4, c5, c6;

void setup(){
  size(640, 360);

  // create some gradients

  b1 = color(190, 190, 190);
  b2 = color(20, 20, 20);


  c1 = color(255, 120, 0);
  c2 = color(10, 45, 255);
  c3 = color(10, 255, 15);
  c4 = color(125, 2, 140);
  c5 = color(255, 255, 0);
  c6 = color(25, 255, 200);
  noLoop();
}

void draw() {
    // background
    setGradient(0, 0, width, height, b1, b2, Y_AXIS);
    //center squares
  setGradient(25, 25, 75, 75, c1, c2, Y_AXIS);
  setGradient(100, 25, 75, 75, c3, c4, X_AXIS);
  setGradient(25, 100, 75, 75, c2, c5, X_AXIS);
  setGradient(100, 100, 75, 75, c4, c6, Y_AXIS); 
}

void setGradient(int x, int y, float w, float h, color c1, color c2, int axis ){
  // calculate differences between color components 
  float deltaR = red(c2)-red(c1);
  float deltaG = green(c2)-green(c1);
  float deltaB = blue(c2)-blue(c1);

  // choose axis
  if(axis == Y_AXIS){
    /*nested for loops set pixels
     in a basic table structure */
    // column
    for (int i=x; i<=(x+w); i++){
      // row
      for (int j = y; j<=(y+h); j++){
        color c = color(
        (red(c1)+(j-y)*(deltaR/h)),
        (green(c1)+(j-y)*(deltaG/h)),
        (blue(c1)+(j-y)*(deltaB/h)) 
          );
        set(i, j, c);
      }
    }  
  }  
  else if(axis == X_AXIS){
    // column 
    for (int i=y; i<=(y+h); i++){
      // row
      for (int j = x; j<=(x+w); j++){
        color c = color(
        (red(c1)+(j-x)*(deltaR/h)),
        (green(c1)+(j-x)*(deltaG/h)),
        (blue(c1)+(j-x)*(deltaB/h)) 
          );
        set(j, i, c);
      }
    }  
  }
}

