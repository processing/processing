
// Based on code 36-07 (p. 331)


PFont f; 
String s = "012345678901234567890123456789";
float angle = 0.0;

boolean record = false;

void setup() {
  size(842, 550);
  println(PFont.list());  // Select a font from this list
  String s = "TheSansMono-ExtraLightItalic-48";
  f = createFont(s, 24);
  textFont(f);
  fill(204);
}

void draw() {
  
  if(record) {
    beginRecord(PDF, "page_326.pdf"); 
  }
  
  textFont(f);
  background(255);
  translate(10, 0);
  angle += 0.1;
  
  float cw = 0;
  //for (int i = 0; i < s.length(); i++) {
  for (int i = 0; i < s.length(); i++) {
    float c = sin(angle + i/PI);
    textSize((c + 1.0) * 18 + 6);
    text(s.charAt(i), cw, 60);
    cw += textWidth(s.charAt(i)) * 1.2;
  }
  
  cw = 0;
  for (int i = 0; i < s.length(); i++) {
    float c = sin((angle+QUARTER_PI/2) + i/PI);
    textSize((c + 1.0) * 18 + 6);
    text(s.charAt(i), cw, 120);
    cw += textWidth(s.charAt(i)) * 1.2;
  }
  
  cw = 0;
  for (int i = 0; i < s.length(); i++) {
    float c = sin((angle+QUARTER_PI) + i/PI);
    textSize((c + 1.0) * 18 + 6);
    text(s.charAt(i), cw, 180);
    cw += textWidth(s.charAt(i)) * 1.2;
  }
  
  cw = 0;
  for (int i = 0; i < s.length(); i++) {
    float c = sin((angle+QUARTER_PI+QUARTER_PI/2) + i/PI);
    textSize((c + 1.0) * 18 + 6);
    text(s.charAt(i), cw, 240);
    cw += textWidth(s.charAt(i)) * 1.2;
  }
  
  cw = 0;
  for (int i = 0; i < s.length(); i++) {
    float c = sin((angle+HALF_PI) + i/PI);
    textSize((c + 1.0) * 18 + 6);
    text(s.charAt(i), cw, 300);
    cw += textWidth(s.charAt(i)) * 1.2;
  }
  
  cw = 0;
  for (int i = 0; i < s.length(); i++) {
    float c = sin((angle+HALF_PI+QUARTER_PI/2) + i/PI);
    textSize((c + 1.0) * 18 + 6);
    text(s.charAt(i), cw, 360);
    cw += textWidth(s.charAt(i)) * 1.2;
  }
  
  cw = 0;
  for (int i = 0; i < s.length(); i++) {
    float c = sin((angle+HALF_PI+QUARTER_PI) + i/PI);
    textSize((c + 1.0) * 18 + 6);
    text(s.charAt(i), cw, 420);
    cw += textWidth(s.charAt(i)) * 1.2;
  }
  
  if(record) {
    endRecord();
    record = false; 
  }
  
}


void keyPressed() {
  record = true; 
}
