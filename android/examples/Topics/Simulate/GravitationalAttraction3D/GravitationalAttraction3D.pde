/**
 * Gravitational Attraction (3D) 
 * by Daniel Shiffman.  
 * 
 * Simulating gravitational attraction 
 * G ---> universal gravitational constant
 * m1 --> mass of object #1
 * m2 --> mass of object #2
 * d ---> distance between objects
 * F = (G*m1*m2)/(d*d)
 *
 * For the basics of working with PVector, see
 * http://processing.org/learning/pvector/
 * as well as examples in Topics/Vectors/
 * 
 */

// A bunch of planets
Planet[] planets = new Planet[10];
// One sun (note sun is not attracted to planets (violation of Newton's 3rd Law)
Sun s;

// An angle to rotate around the scene
float angle = 0;

void setup() {
  size(displayWidth, displayHeight, P3D);
  orientation(LANDSCAPE);
  // Some random planets
  for (int i = 0; i < planets.length; i++) {
    planets[i] = new Planet(random(0.1, 2), random(-width/2, width/2), random(-height/2, height/2), random(-100, 100));
  }
  // A single sun
  s = new Sun();
}

void draw() {
  background(0);
  // Setup the scene
  sphereDetail(8);
  lights();
  translate(width/2, height/2);
  rotateY(angle);


  // Display the Sun
  s.display();

  // All the Planets
  for (int i = 0; i < planets.length; i++) {
    // Sun attracts Planets
    PVector force = s.attract(planets[i]);
    planets[i].applyForce(force);
    // Update and draw Planets
    planets[i].update();
    planets[i].display();
  }

  // Rotate around the scene
  angle += 0.003;
}










