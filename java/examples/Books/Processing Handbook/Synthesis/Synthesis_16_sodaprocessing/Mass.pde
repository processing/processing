
class Mass {
  static final float diamter = 5;
  static final float radius = 1+diamter/2;

  float x,y,xv,yv;

  Mass(int x, int y) {
    this.x = x;
    this.y = y;
  }

  void update() {
    yv += g.value;

    double speed = sqrt(xv*xv+yv*yv);
    double fs = 1-f.value;
    if (speed>speedFrictionThreshold)
    fs *= speedFrictionThreshold/speed;
    xv *= fs;
    yv *= fs;

    x += xv;
    y += yv;

    if (x<radius) {
      x -= x-radius;
      xv = -xv;
    } else if (x>width-radius) {
      x -= x-(width-radius);
      xv = -xv;
    }
    if (y<sliderHeight+radius) {
      y -= y-(sliderHeight+radius);
      yv = -yv;
    } else if (y>height-radius) {
      y -= y-(height-radius);
      yv = -yv;
    }
  }

  void clamp() {
    if (x<radius) {
      x = radius;
    } else if (x>width-radius) {
      x = width-radius;
    }
    if (y<sliderHeight+radius) {
      y = sliderHeight+radius;
    } else if (y>height-radius) {
      y = height-radius;
    }
  }

  void display() {
    if (this == overMass) {
      stroke(0x00,0x99,0xFF);
      line(x,y,mouseX,mouseY);
      noStroke();
      fill(0x00,0x99,0xFF);
    }
    else {
      noStroke();
      fill(0);
    }
    ellipse(x,y,diamter,diamter);
  }

  float distanceTo(Mass m) {
    return distanceTo(m.x,m.y);
  }

  float distanceTo(float x,float y) {
    float dx = this.x-x;
    float dy = this.y-y;
    return sqrt(dx*dx+dy*dy);
  }
}

