
// Based on code 13-05 (p. 113)


size(360, 550);
//size(360, 550, PDF, "page_110.pdf");
background(255);

println(PFont.list());  // List the available fonts
String s = "Ziggurat-HTF-Black";
PFont font = createFont(s, 34);
//PFont font = createFont("ZigguratHTFBlack", 34);
textFont(font);

textAlign(CENTER);

fill(0, 50);  // Black with low opacity

textSize(460);

for(int i=0; i<6; i++) {
  text(i, width/2, height*0.98 - i*40); 
}


/*
text("1", width/2, height*.9);
text("2", width/2, height*.9);
text("3", width/2, height*.9);
text("4", width/2, height*.9);
text("5", width/2, height*.9);
*/
