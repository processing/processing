float angle = 0;
void setup() {
  size(400,400); 
}

void draw() {
 background(255);
  float y = 100*sin(angle);
  angle += 0.02;
  
  fill(127);
  translate(width/2,height/2);
  line(0,0,0,y);
  ellipse(0,y,16,16);
}
