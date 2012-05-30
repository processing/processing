PShape world;
float resizeFactor;
float offsetX, offsetY;
  
void setup() {
  size(displayWidth, displayWidth, P2D);
  orientation(LANDSCAPE);
  
  world = loadShape("World_map_with_nations.svg");
    
  PVector top = world.getTop();
  world.translate(-top.x, -top.y);
    
    
  float r = world.getWidth() / world.getHeight();
  float w = width;
  float h = width / r;
  if (height < h) {
    h = height;
    w = r * h;
    offsetX = (width - w) / 2;
    offsetY = 0;
  } else {
    offsetX = 0;
    offsetY = (height - h) / 2;      
  }
  resizeFactor = w / world.getWidth();    
    
  world.scale(resizeFactor);
}

public void draw() {
  shape(world);
}

