size(120, 100);
int front = int(random(1, 10)); // Select the front card
int back = int(random(1, 10)); // Select the back card
PImage imgFront = loadImage(front + "f.jpg");
PImage imgBack = loadImage(back + "b.jpg");
image(imgFront, 0, 0);
image(imgBack, 60, 0);