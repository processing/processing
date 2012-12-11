// The Nature of Code
// Daniel Shiffman
// http://natureofcode.com

size(1800,90);

int w = 90;

int total = width/w;

int[] cells = {1,0,1,0,0,0,0,1,0,1,1,1,0,0,0,1,1,1,0,0};


print("int[] cells = {");
for (int i = 0; i < cells.length; i++) {
  if (cells[i] == 0) fill(255);
  else fill(64);
  stroke(0);
  rect(i*w,0,w-1,w-1);
  print(cells[i] +",");
}

saveFrame("cells.png");








