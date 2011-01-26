
// Based on code 13-01 (p. 113) and code 33-11 (p. 305)


size(460, 225);
//size(460, 225, PDF, "page_222.pdf");
background(255);
fill(204);

println(PFont.list());  // Select a font from this list
String s = "Ziggurat-HTF-Black";
PFont font = createFont(s, 34);
textFont(font);

char[] c1 = {'1', '2', '3', '4', '5', '6', '7', '8', '9', '0', '-', '='};
char[] c2 = {'Q', 'W', 'E', 'R', 'T', 'Y', 'U', 'I', 'O', 'P', '[', ']'};
char[] c3 = {'A', 'S', 'D', 'F', 'G', 'H', 'J', 'K', 'L', ';'};
char[] c4 = {'Z', 'X', 'C', 'V', 'B', 'N', 'M', ',', '.', '/'};

textAlign(CENTER);
textSize(50);
stroke(204);

strokeWeight(0.5);

for (int i = 0; i < c1.length; i++) {
  text(c1[i], 20+i*78, 50);
  if(i%2 != 0) {
    line(20+i*39, 40, 20+i*39, 20);
  }
}

for (int i = 0; i < c2.length; i++) {
  text(c2[i], 46+i*78, 105);
    if(i%2 != 0) {
    line(46+i*39, 95, 46+i*39, 75);
  }
}

for (int i = 0; i < c3.length; i++) {
  text(c3[i], 65+i*78, 160);
  if(i%2 != 0) {
    line(65+i*39, 150, 65+i*39, 130);
  }
}

for (int i = 0; i < c4.length; i++) {
  text(c4[i], 90+i*78, 215);
  if(i%2 != 0) {
    line(90+i*39, 205, 90+i*39, 185);
  }
}
