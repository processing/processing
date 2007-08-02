/**
 * Simple 3D Bird. 
 * By Ira Greenberg 
 * 
 * Using a box and 2 rects to simulate a flying bird. 
 * Trig functions handle the flapping and sinuous movement.
 */

float ang = 0, ang2 = 0, ang3 = 0, ang4 = 0;
float px = 0, py = 0, pz = 0;
float flapSpeed = .2;

void setup(){
  size(200, 200, P3D);
  noStroke();
}

void draw(){
  background(51);
  lights();
  fill(153);

  //flight
  px = sin(radians(ang3))*170;
  py = cos(radians(ang3))*300;
  pz = sin(radians(ang4))*500;
  translate(width/2+ px, height/2+py, -700+pz);
  rotateX(sin(radians(ang2))*120);
  rotateY(sin(radians(ang2))*50);
  rotateZ(sin(radians(ang2))*65);
  
  //body
  box(20, 100, 20);
  fill(204);
  
  //left wing
  pushMatrix();
  rotateY(sin(radians(ang))*-20);
  rect(-75, -50, 75, 100);
  popMatrix();

  //right wing
  pushMatrix();
  rotateY(sin(radians(ang))*20);
  rect(0, -50, 75, 100);
  popMatrix();

  //wing flap
  ang+=flapSpeed;
  if (ang>3){
    flapSpeed*=-1;
  } 
  if (ang<-3){
    flapSpeed*=-1;
  }

  //increment angles
  ang2+=.01;
  ang3+=2;
  ang4+=.75;
}

