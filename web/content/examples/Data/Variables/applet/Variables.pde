// Variables
// by REAS <http://reas.com>

// Variables are used for storing values.
// Changing the values of variables 'a' and 'b' significantly change the composition. 

// Created 2 September 2002

size(200, 200);
background(0);
stroke(153);

int a = 20;
int b = 50;
int c = a*8;
int d = a*9;
int e = b-a;
int f = b*2;
int g = f+e;

line(a, f, b, g);
line(b, e, b, g);
line(b, e, d, c);
line(a, e, d-e, c);
