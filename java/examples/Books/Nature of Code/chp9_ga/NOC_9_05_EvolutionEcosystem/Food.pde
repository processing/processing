// The Nature of Code
// Daniel Shiffman
// http://natureofcode.com

// Evolution EcoSystem

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
