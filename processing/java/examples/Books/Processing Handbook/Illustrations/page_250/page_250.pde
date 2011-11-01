
size(442, 500);
//size(442, 500, PDF, "page_250.pdf");
background(255);
fill(0);

println(PFont.list());
String s = "TheSansMono Light Italic";
PFont font = createFont(s, 24);
textFont(font);

String s1 = "void draw() {";
String s2 = "  background(126);";
String s3 = "  ellipse(mouseX, mouseY, 33, 33);";
String s4 = "}";

String s5 = "void Draw() (";
String s6 = "  background(126)";
String s7 = "  ellipse(mouseX. mousey, 33, 33);";
String s8 = "}";

textAlign(LEFT);
textSize(26);
stroke(204);

text(s5, 0, 30);
text(s6, 0, 70);
text(s7, 0, 110);
text(s8, 0, 150);

text(s1, 0, 250);
text(s2, 0, 290);
text(s3, 0, 320);
text(s4, 0, 370);






