// The Nature of Code
// Daniel Shiffman
// http://natureofcode.com

// Interactive Selection
// http://www.genarts.com/karl/papers/siggraph91.html

Population population;
Button button;

void setup() {
  size(800,200);
  colorMode(RGB,1.0);
  int popmax = 10;
  float mutationRate = 0.05;  // A pretty high mutation rate here, our population is rather small we need to enforce variety
  // Create a population with a target phrase, mutation rate, and population max
  population = new Population(mutationRate,popmax);
  // A simple button class
  button = new Button(15,150,160,20, "evolve new generation");
}

void draw() {
  background(1.0);
  // Display the faces
  population.display();
  population.rollover(mouseX,mouseY);
  // Display some text
  textAlign(LEFT);
  fill(0);
  text("Generation #:" + population.getGenerations(),15,190);

  // Display the button
  button.display();
  button.rollover(mouseX,mouseY);

}

// If the button is clicked, evolve next generation
void mousePressed() {
  if (button.clicked(mouseX,mouseY)) {
    population.selection();
    population.reproduction();
  }
}

void mouseReleased() {
  button.released();
}
