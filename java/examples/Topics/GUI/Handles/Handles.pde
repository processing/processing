/**
 * Handles. 
 * 
 * Click and drag the white boxes to change their position. 
 */
 
Handle[] handles;

void setup() {
  size(640, 360);
  int num = height/15;
  handles = new Handle[num];
  int hsize = 10;
  for (int i = 0; i < handles.length; i++) {
    handles[i] = new Handle(width/2, 10+i*15, 50-hsize/2, 10, handles);
  }
}

void draw() {
  background(153);
  
  for (int i = 0; i < handles.length; i++) {
    handles[i].update();
    handles[i].display();
  }
  
  fill(0);
  rect(0, 0, width/2, height);
}

void mouseReleased()  {
  for (int i = 0; i < handles.length; i++) {
    handles[i].releaseEvent();
  }
}

class Handle {
  
  int x, y;
  int boxx, boxy;
  int stretch;
  int size;
  boolean over;
  boolean press;
  boolean locked = false;
  boolean otherslocked = false;
  Handle[] others;
  
  Handle(int ix, int iy, int il, int is, Handle[] o) {
    x = ix;
    y = iy;
    stretch = il;
    size = is;
    boxx = x+stretch - size/2;
    boxy = y - size/2;
    others = o;
  }
  
  void update() {
    boxx = x+stretch;
    boxy = y - size/2;
    
    for (int i=0; i<others.length; i++) {
      if (others[i].locked == true) {
        otherslocked = true;
        break;
      } else {
        otherslocked = false;
      }  
    }
    
    if (otherslocked == false) {
      overEvent();
      pressEvent();
    }
    
    if (press) {
      stretch = lock(mouseX-width/2-size/2, 0, width/2-size-1);
    }
  }
  
  void overEvent() {
    if (overRect(boxx, boxy, size, size)) {
      over = true;
    } else {
      over = false;
    }
  }
  
  void pressEvent() {
    if (over && mousePressed || locked) {
      press = true;
      locked = true;
    } else {
      press = false;
    }
  }
  
  void releaseEvent() {
    locked = false;
  }
  
  void display() {
    line(x, y, x+stretch, y);
    fill(255);
    stroke(0);
    rect(boxx, boxy, size, size);
    if (over || press) {
      line(boxx, boxy, boxx+size, boxy+size);
      line(boxx, boxy+size, boxx+size, boxy);
    }

  }
}

boolean overRect(int x, int y, int width, int height) {
  if (mouseX >= x && mouseX <= x+width && 
      mouseY >= y && mouseY <= y+height) {
    return true;
  } else {
    return false;
  }
}

int lock(int val, int minv, int maxv) { 
  return  min(max(val, minv), maxv); 
} 
