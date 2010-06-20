size(480, 120);
int x = 25;
int h = 20;
int y = 25;
rect(x, y, 300, h);        // Top
x = x + 100;
rect(x, y + h, 300, h);    // Middle
x = x - 250;
rect(x, y + h*2, 300, h);  // Bottom
