// Reducing a font created at 32 pixels
PFont font;
font = loadFont("Ziggurat-32.vlw");
textFont(font);
fill(0);
text("LNZ", 0, 40); // Large
textSize(18);
text("STN", 0, 75); // Medium
textSize(12);
text("BOS", 0, 100); // Small