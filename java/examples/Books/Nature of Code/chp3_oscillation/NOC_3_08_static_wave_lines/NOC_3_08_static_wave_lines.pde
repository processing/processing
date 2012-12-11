// The Nature of Code
// Daniel Shiffman
// http://natureofcode.com

float angle = 0;
float angleVel = 0.1;

size(800,200);
background(255);
stroke(0);
strokeWeight(2);
noFill();

beginShape();
for (int x = 0; x <= width; x += 5) {
  float y = map(sin(angle),-1,1,0,height);
  vertex(x,y);
  angle +=angleVel;
} 
endShape();

