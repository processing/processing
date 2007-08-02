/**
 * True/False. 
 * 
 * Boolean data is one bit of information. True or false. 
 * It is common to use Booleans with control statements to 
 * determine the flow of a program. In this example, when the
 * boolean value "x" is true, vertical black lines are drawn and when
 * the boolean value "x" is false, horizontal gray lines are drawn. 
 */
 
boolean x = false;

size(200, 200);
background(0);
stroke(0);

for (int i = 1; i < width; i += 2) 
{
  if (i < width/2) {
    x = true;
  } else {
    x = false;
  }
  
  if (x) {
    stroke(255);
    line(i, 1, i, height-1);
  }
  
  if (!x) {
    stroke(126);
    line(width/2 , i, width-2, i);
  }
}
