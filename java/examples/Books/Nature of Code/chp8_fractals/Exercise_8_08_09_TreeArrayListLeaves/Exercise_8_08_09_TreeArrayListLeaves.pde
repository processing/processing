// The Nature of Code
// Daniel Shiffman
// http://natureofcode.com

// Recursive Tree (w/ ArrayList)
// Nature of Code, Chapter 8

// Recursive branching "structure" without an explicitly recursive function
// Instead we have an ArrayList to hold onto N number of elements
// For every element in the ArrayList, we add 2 more elements, etc. (this is the recursion)

// An arraylist that will keep track of all current branches
ArrayList<Branch> tree;
ArrayList<Leaf> leaves;

void setup() {
  size(640,360);
  background(255);
  // Setup the arraylist and add one branch to it
  tree = new ArrayList<Branch>();
  leaves = new ArrayList<Leaf>();
  // A branch has a starting location, a starting "velocity", and a starting "timer" 
  Branch b = new Branch(new PVector(width/2,height),new PVector(0,-1),100);
  // Add to arraylist
  tree.add(b);
}

void draw() {
  background(255);

  // Let's stop when the arraylist gets too big
  // For every branch in the arraylist
  for (int i = tree.size()-1; i >= 0; i--) {
    // Get the branch, update and draw it
    Branch b = tree.get(i);
    b.update();
    b.render();
    // If it's ready to split
    if (b.timeToBranch()) {
      if (tree.size() < 1024) {
        //tree.remove(i);             // Delete it
        tree.add(b.branch( 30));   // Add one going right
        tree.add(b.branch(-25));   // Add one going left
      } 
      else {
        leaves.add(new Leaf(b.end));
      }
    }
  }
  
  for (Leaf leaf : leaves) {
     leaf.display(); 
  }

}




