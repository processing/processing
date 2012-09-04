// Pathfinding w/ Genetic Algorithms
// Daniel Shiffman <http://www.shiffman.net>

// Rocket class -- this is just like our Boid / Particle class
// the only difference is that it has DNA & fitness

class Rocket {

  // All of our physics stuff
  PVector location;
  PVector velocity;
  PVector acceleration;
  float r;
  float recordDist;

  float fitness;
  DNA dna;

  boolean stopped;  // Am I stuck?
  int finish;       // What was my finish? (first, second, etc. . . )

  //constructor
  Rocket(PVector l, DNA dna_) {
    acceleration = new PVector();
    velocity = new PVector();
    location = l.get();
    r = 2;
    dna = dna_;
    stopped = false;
    finish = 100000;  // Some high number to begin with
    recordDist = width;
  }

  // FITNESS FUNCTION 
  // distance = distance from target
  // finish = what order did i finish (first, second, etc. . .)
  // f(distance,finish) =   (1.0f / finish^1.5) * (1.0f / distance^6);
  // a lower finish is rewarded (exponentially) and/or shorter distance to target (exponetially)
  void calcFitness() {
    float d = recordDist;
    if (d < diam/2) {
      d = 1.0;
    }
    // Reward finishing faster and getting closer
    fitness = (1.0f / pow(finish,1.5)) * (1 / (pow(d,6)));
  }

  void setFinish(int f) {
    finish = f;
  }

  // Run in relation to all the obstacles
  // If I'm stuck, don't bother updating or checking for intersection
  void run(ArrayList<Obstacle> o) {
    if (!stopped) {
      update();
      // If I hit an edge or an obstacle
      if ((borders()) || (obstacles(o))) {
        stopped = true;
      }
    }
    // Draw me!
    display();
  }

   // Did I hit an edge?
   boolean borders() {
    if ((location.x < 0) || (location.y < 0) || (location.x > width) || (location.y > height)) {
      return true;
    } else {
      return false;
    }
  }

  // Did I make it to the target?
  boolean finished() {
    float d = dist(location.x,location.y,target.r.x,target.r.y);
    if (d < recordDist) recordDist = d;
    if (target.contains(location)) {
      stopped = true;
      return true;
    }
    return false;
  }

  // Did I hit an obstacle?
  boolean obstacles(ArrayList<Obstacle> o) {
    for (Obstacle obs : o) {
      if (obs.contains(location)) {
        return true;
      }
    }
    return false;
  }

  void update() {
    if (!finished()) {
      // Where are we?  Our location will tell us what steering vector to look up in our DNA;
      int x = (int) location.x/gridscale;
      int y = (int) location.y/gridscale;
      x = constrain(x,0,width/gridscale-1);  // Make sure we are not off the edge
      y = constrain(y,0,height/gridscale-1); // Make sure we are not off the edge

      // Get the steering vector out of our genes in the right spot
      // We could do (desired - velocity) to be more in line with the Reynolds flow field following
      acceleration.add(dna.genes[x+y*width/gridscale]);

      // This is all the same stuff we've done before
      acceleration.mult(maxforce);
      velocity.add(acceleration);
      velocity.limit(maxspeed);
      location.add(velocity);
      acceleration.mult(0);
    }
  }

  void display() {
    //fill(0,150);
    //stroke(0);
    //ellipse(location.x,location.y,r,r);
    float theta = velocity.heading2D() + PI/2;
    fill(200,100);
    stroke(0);
    pushMatrix();
    translate(location.x,location.y);
    rotate(theta);
    beginShape(TRIANGLES);
    vertex(0, -r*2);
    vertex(-r, r*2);
    vertex(r, r*2);
    endShape();
    popMatrix();
    
    
  }

  float getFitness() {
    return fitness;
  }

  DNA getDNA() {
    return dna;
  }

  boolean stopped() {
    return stopped;
  }

}
