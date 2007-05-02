PButton button;
PFont font;
boolean toggle;

void setup() {
  //// create the button with the label "Toggle"
  button = new PButton("Toggle");
  //// let the button size itself based on the label
  button.calculateBounds(0, 0, width, height);
  //// now move it to the middle of the screen,
  //// without changing its dimensions
  button.setBounds((width - button.width) / 2, 
                   (height - button.height) / 2, 
                   button.width, 
                   button.height);
  //// let the button do any final initialization
  button.initialize();
  
  //// set up the font for drawing to the screen
  font = loadFont();
  textFont(font);
  textAlign(CENTER);
}

void draw() {
  background(200);
  
  //// draw the button
  button.draw();
  if (toggle) {
    //// draw the toggle text underneath the button
    text("On!", width / 2, 
         button.y + button.height + 4 + font.baseline);
  }
}

void keyPressed() {
  //// let the button handle any keypresses
  button.keyPressed();
}

void keyReleased() {
  //// let the button handle any keyreleases. this is
  //// important as PButton sends its event on release!
  button.keyReleased();
}

void libraryEvent(Object library, int event, Object data) {
  if (library == button) {
    //// if the button sent a library event, it was pressed!
    toggle = !toggle;
  }
}
