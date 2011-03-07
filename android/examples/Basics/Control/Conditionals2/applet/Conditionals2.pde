/**
 * Conditionals 2. 
 * 
 * We extend the language of conditionals by adding the 
 * keyword "else". This allows conditionals to ask 
 * two or more sequential questions, each with a different
 * action. 
 */
 
size(200, 200);
background(0);

for(int i=2; i<width-2; i+=2) {
  // If 'i' divides by 20 with no remainder 
  // draw the first line else draw the second line
  if(i%20 == 0) {
    stroke(255);
    line(i, 40, i, height/2);
  } else if (i%10 == 0) {
    stroke(153);
    line(i, 20, i, 180); 
  } else {
    stroke(102);
    line(i, height/2, i, height-40);
  }
}
