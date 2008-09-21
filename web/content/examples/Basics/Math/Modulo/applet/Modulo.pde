/**
 * Modulo. 
 * 
 * The modulo operator (%) returns the remainder of a number 
 * divided by another. As in this example, it is often used 
 * to keep numerical values within a set range. 
 */
 
int num = 20;
float c;

void setup()
{
  size(200,200);
  fill(255);
  frameRate(30);
}

void draw() 
{ 
  background(0);
  c+=0.1;
  for(int i=1; i<height/num; i++) { 
    float x = (c%i)*i*i;
    stroke(102);
    line(0, i*num, x, i*num);
    noStroke();
    rect(x, i*num-num/2, 8, num);
  } 
} 

