import processing.sound.*;
PFont font = loadFont();
textFont(font);
background(255);
fill(0);

// Get a list of the supported types of media on the phone
String[] types = Sound.supportedTypes();
// Start at the top of the screen
int y = font.baseline;
// Draw each of the supported types on the screen
for (int i = 0, length = types.length; i < length; i++) {
// Draw the supported type (represented as an
// Internet MIME type string, such as audio/x-wav)
  text(types[i], 0, y);
// Go to the next line
  y += font.height;
}
