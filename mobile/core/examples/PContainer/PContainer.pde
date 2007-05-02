PScrollBar scrollbar;
PContainer screen;

void setup() {
  //// put a scrollbar on the right side of the screen
  scrollbar = new PScrollBar();
  scrollbar.setBounds(width - 4, 0, 4, height);
  
  //// let the container fill the rest of the screen
  screen = new PContainer();
  screen.scrolling = true;
  screen.scrollbar = scrollbar;
  screen.setBounds(0, 0, width - 4, height);
  
  //// create bunch of labels and buttons, using the
  //// PContainer.HEIGHT_UNBOUNDED constant to allow them to size
  //// themselves as tall as they need to be to fit.  the
  //// container will scroll the contents
  int y = 0;
  for (int i = 0; i < 4; i++) {
    PLabel label = new PLabel("The quick brown" +
      " fox jumped over the lazy dogs.");
    label.calculateBounds(4, y, width - 8, PContainer.HEIGHT_UNBOUNDED);
    y = label.y + label.height + 4;
    screen.add(label);
    
    PButton button = new PButton("Button " + i);
    button.calculateBounds(4, y, width - 8, PContainer.HEIGHT_UNBOUNDED);
    y = button.y + button.height + 4;
    screen.add(button);
  }
  
  //// initialize the container (which initializes
  //// all of its children and the scrollbar)
  screen.initialize();
}

void draw() {
  //// draw the container (which draws its children)
  screen.draw();
}

void keyPressed() {
  //// let the container handle the input, it will
  //// pass it down to the focused child
  screen.keyPressed();
}

void keyReleased() {
  //// let the container handle the input, it will
  //// pass it down to the focused child
  screen.keyReleased();
}

