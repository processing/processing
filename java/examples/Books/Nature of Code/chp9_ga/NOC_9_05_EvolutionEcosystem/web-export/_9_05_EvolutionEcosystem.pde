// Evolution EcoSystem
// Daniel Shiffman <http://www.shiffman.net>
// The Nature of Code

// A World of creatures that eat food
// The more they eat, the longer they survive
// The longer they survive, the more likely they are to reproduce
// The bigger they are, the easier it is to land on food
// The bigger they are, the slower they are to find food
// When the creatures die, food is left behind


World world;

void setup() {
  size(800, 200);
  // World starts with 20 creatures
  // and 20 pieces of food
  world = new World(20);
  smooth();
}

void draw() {
  background(255);
  world.run();
}

// We can add a creature manually if we so desire
void mousePressed() {
  world.born(mouseX,mouseY); 
}


// Evolution EcoSystem
// Daniel Shiffman <http://www.shiffman.net>

// Creature class

class Bloop {
  PVector location; // Location
  DNA dna;          // DNA
  float health;     // Life timer
  float xoff;       // For perlin noise
  float yoff;
  // DNA will determine size and maxspeed
  float r;
  float maxspeed;

  // Create a "bloop" creature
  Bloop(PVector l, DNA dna_) {
    location = l.get();
    health = 200;
    xoff = random(1000);
    yoff = random(1000);
    dna = dna_;
    // Gene 0 determines maxspeed and r
    // The bigger the bloop, the slower it is
    maxspeed = map(dna.genes[0], 0, 1, 15, 0);
    r = map(dna.genes[0], 0, 1, 0, 50);
  }

  void run() {
    update();
    borders();
    display();
  }

  // A bloop can find food and eat it
  void eat(Food f) {
    ArrayList<PVector> food = f.getFood();
    // Are we touching any food objects?
    for (int i = food.size()-1; i >= 0; i--) {
      PVector foodLocation = food.get(i);
      float d = PVector.dist(location, foodLocation);
      // If we are, juice up our strength!
      if (d < r/2) {
        health += 100; 
        food.remove(i);
      }
    }
  }

  // At any moment there is a teeny, tiny chance a bloop will reproduce
  Bloop reproduce() {
    // asexual reproduction
    if (random(1) < 0.0005) {
      // Child is exact copy of single parent
      DNA childDNA = dna.copy();
      // Child DNA can mutate
      childDNA.mutate(0.01);
      return new Bloop(location, childDNA);
    } 
    else {
      return null;
    }
  }

  // Method to update location
  void update() {
    // Simple movement based on perlin noise
    float vx = map(noise(xoff),0,1,-maxspeed,maxspeed);
    float vy = map(noise(yoff),0,1,-maxspeed,maxspeed);
    PVector velocity = new PVector(vx,vy);
    xoff += 0.01;
    yoff += 0.01;

    location.add(velocity);
    // Death always looming
    health -= 0.2;
  }

  // Wraparound
  void borders() {
    if (location.x < -r) location.x = width+r;
    if (location.y < -r) location.y = height+r;
    if (location.x > width+r) location.x = -r;
    if (location.y > height+r) location.y = -r;
  }

  // Method to display
  void display() {
    ellipseMode(CENTER);
    stroke(0,health);
    fill(0, health);
    ellipse(location.x, location.y, r, r);
  }

  // Death
  boolean dead() {
    if (health < 0.0) {
      return true;
    } 
    else {
      return false;
    }
  }
}

// Evolution EcoSystem
// Daniel Shiffman <http://www.shiffman.net>

// Class to describe DNA
// Has more features for two parent mating (not used in this example)

class DNA {

  // The genetic sequence
  float[] genes;
  
  // Constructor (makes a random DNA)
  DNA() {
    // DNA is random floating point values between 0 and 1 (!!)
    genes = new float[1];
    for (int i = 0; i < genes.length; i++) {
      genes[i] = random(0,1);
    }
  }
  
  DNA(float[] newgenes) {
    genes = newgenes;
  }
  
  DNA copy() {
    float[] newgenes = new float[genes.length];
    //arraycopy(genes,newgenes);
    // JS mode not supporting arraycopy
    for (int i = 0; i < newgenes.length; i++) {
      newgenes[i] = genes[i];
    }
    
    return new DNA(newgenes);
  }
  
  // Based on a mutation probability, picks a new random character in array spots
  void mutate(float m) {
    for (int i = 0; i < genes.length; i++) {
      if (random(1) < m) {
         genes[i] = random(0,1);
      }
    }
  }
}
// Evolution EcoSystem
// Daniel Shiffman <http://www.shiffman.net>

// A collection of food in the world

class Food {
  ArrayList<PVector> food;
 
  Food(int num) {
    // Start with some food
    food = new ArrayList();
    for (int i = 0; i < num; i++) {
       food.add(new PVector(random(width),random(height))); 
    }
  } 
  
  // Add some food at a location
  void add(PVector l) {
     food.add(l.get()); 
  }
  
  // Display the food
  void run() {
    for (PVector f : food) {
       rectMode(CENTER);
       stroke(0);
       fill(175);
       rect(f.x,f.y,8,8);
    } 
    
    // There's a small chance food will appear randomly
    if (random(1) < 0.001) {
       food.add(new PVector(random(width),random(height))); 
    }
  }
  
  // Return the list of food
  ArrayList getFood() {
    return food; 
  }
}
// Evolution EcoSystem
// Daniel Shiffman <http://www.shiffman.net>
// Spring 2007, The Nature of Code

// The World we live in
// Has bloops and food

class World {

  ArrayList<Bloop> bloops;    // An arraylist for all the creatures
  Food food;

  // Constructor
  World(int num) {
    // Start with initial food and creatures
    food = new Food(num);
    bloops = new ArrayList<Bloop>();              // Initialize the arraylist
    for (int i = 0; i < num; i++) {
      PVector l = new PVector(random(width),random(height));
      DNA dna = new DNA();
      bloops.add(new Bloop(l,dna));
    }
  }

  // Make a new creature
  void born(float x, float y) {
    PVector l = new PVector(x,y);
    DNA dna = new DNA();
    bloops.add(new Bloop(l,dna));
  }

  // Run the world
  void run() {
    // Deal with food
    food.run();
    
    // Cycle through the ArrayList backwards b/c we are deleting
    for (int i = bloops.size()-1; i >= 0; i--) {
      // All bloops run and eat
      Bloop b = bloops.get(i);
      b.run();
      b.eat(food);
      // If it's dead, kill it and make food
      if (b.dead()) {
        bloops.remove(i);
        food.add(b.location);
      }
      // Perhaps this bloop would like to make a baby?
      Bloop child = b.reproduce();
      if (child != null) bloops.add(child);
    }
  }
}




