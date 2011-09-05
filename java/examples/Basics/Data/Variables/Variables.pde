/**
 * Variables. 
 * 
 * Variables are used for storing values. In this example, change 
 * the values of variables 'a' and 'b' to affect the composition. 
 */
 
size(640, 360);
background(0);
stroke(153);
strokeWeight(2);

int a = 60;
int b = 100;
int c = 200;
int d = 120;

line(a, b, a+c, b);

a += 100;
b += 50;
line(a, b, a+c, b);



