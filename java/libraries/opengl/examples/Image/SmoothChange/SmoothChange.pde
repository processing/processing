PImage img;
int level = 2;  
  
public void setup() {
  size(200, 200, P3D);
  frameRate(90);
  smooth(level);
  
  img = loadImage("milan_rubbish.jpg");
} 

public void draw () {	  
  background(0);
    
  tint(255, 150);
  image(img, 0, 0, width, height);
    
  fill(255);
  text("current smooth: " + level, mouseX, mouseY);
}
  
void keyPressed() {
 level = 4; 
  smooth(level);	  
}

