/**
 * Simple Radial Gradient 
 * by Ira Greenberg. 
 * 
 * Using the convenient red(), green() 
 * and blue() component functions,
 * generate an array of radial gradients.
 */
 
void setup(){
  size(200, 200);
  background(0);
  smooth();

  // create a simple table of gradients
  int columns = 4;
  int radius = (width/columns)/2;
  // create some gradients
  for (int i=radius; i< width; i+=radius*2){
    for (int j =radius; j< height; j+=radius*2){
      createGradient(i, j, radius, 
      color(int(random(255)), int(random(255)), int(random(255))), 
      color(int(random(255)), int(random(255)), int(random(255))));
    }
  }
}

void createGradient (float x, float y, float radius, color c1, color c2){
  float px = 0, py = 0, angle = 0;

  // calculate differences between color components 
  float deltaR = red(c2)-red(c1);
  float deltaG = green(c2)-green(c1);
  float deltaB = blue(c2)-blue(c1);
  // hack to ensure there are no holes in gradient
  // needs to be increased, as radius increases
  float gapFiller = 8.0;

  for (int i=0; i< radius; i++){
    for (float j=0; j<360; j+=1.0/gapFiller){
      px = x+cos(radians(angle))*i;
      py = y+sin(radians(angle))*i;
      angle+=1.0/gapFiller;
      color c = color(
      (red(c1)+(i)*(deltaR/radius)),
      (green(c1)+(i)*(deltaG/radius)),
      (blue(c1)+(i)*(deltaB/radius)) 
        );
      set(int(px), int(py), c);      
    }
  }
  // adds smooth edge 
  // hack anti-aliasing
  noFill();
  strokeWeight(3);
  ellipse(x, y, radius*2, radius*2);
}

