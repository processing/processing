import processing.opengl.*;

Mover[] movers = new Mover[10];

Attractor a;

float angle = 0;

void setup() {
  size(800,200,OPENGL);
  smooth();
  background(255);
  for (int i = 0; i < movers.length; i++) {
    movers[i] = new Mover(random(0.1,2),random(-width/2,width/2),random(-height/2,height/2),random(-100,100)); 
  }
  a = new Attractor();
}

void draw() {
  background(0);
  sphereDetail(8);
  lights();
  translate(width/2,height/2);
  rotateY(angle);

  
  a.display();

  for (int i = 0; i < movers.length; i++) {
    PVector force = a.attract(movers[i]);
    movers[i].applyForce(force);

    movers[i].update();
    movers[i].display();
  }
  
  angle += 0.003;

}











