PContainer container;
PImageLabel imglabel;
PLabel label;

void setup() {
  container = new PContainer();
  
  PImage img = loadImage("mobile.png");
  imglabel = new PImageLabel(img);
  imglabel.setBounds((width - img.width) / 2, 
                     (height - img.height) / 2,
                     img.width,
                     img.height);  
  container.add(imglabel);
  
  int y = imglabel.y + imglabel.height;  
  label = new PLabel("Text Label");
  label.align = CENTER;
  label.setBounds(0, y, width, height - y);
  container.add(label);
}

void draw() {
  background(255);
  container.draw();
}
