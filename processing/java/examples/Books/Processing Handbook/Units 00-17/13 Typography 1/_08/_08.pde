// Enlarging a font created at 12 pixels
PFont font;
font = loadFont("Ziggurat-12.vlw");
textFont(font);
textSize(32);
fill(0);
text("LNZ", 0, 40); // Large
textSize(18);
text("STN", 0, 75); // Medium
textSize(12);
text("BOS", 0, 100); // Small