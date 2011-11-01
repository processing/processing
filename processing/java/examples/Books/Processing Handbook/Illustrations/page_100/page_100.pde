
// Based on code 11-04 (p. 103) and code 13-01 (p. 113)


//size(360, 666, PDF, "page_100.pdf");
size(360, 666);
background(255);

println(PFont.list());  // List the available fonts
String s = "Ziggurat-HTF-Black";
PFont font = createFont(s, 34);
textFont(font);

fill(204);

int a = 37;

for (int i = 20; i < height+40; i += 38) {
  textSize(34);
  text(char(a), 50, i);
  textSize(10);
  text(a, 10, i-9);
  a++;
}

for (int i = 20; i < height+40; i += 38) {
  textSize(34);
  text(char(a), 170, i);
  textSize(10);
  text(a, 130, i-9);
  a++;
}

for (int i = 20; i < height+40; i += 38) {
  textSize(34);
  text(char(a), 290, i);
  textSize(10);
  text(a, 250, i-9);
  a++;
}





