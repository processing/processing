PGraphics big; // Declare a PGraphics variable

void setup() {
  big = createGraphics(3000, 3000, JAVA2D); // Create a new PGraphics object
  big.beginDraw(); // Start drawing to the PGraphics object
  big.background(128); // Set the background
  big.line(20, 1800, 1800, 900); // Draw a line
  big.endDraw(); // Stop drawing to the PGraphics object
  big.save("big.tif");
}
