
class Spring {
  Mass a,b;
  float restLength;

  Spring(Mass a,Mass b) {
    this.a = a;
    this.b = b;
    restLength = a.distanceTo(b);
  }

  void display() {
    if (this == overSpring) {
      stroke(0x00,0x99,0xFF);
      float vx = b.x-a.x;
      float vy = b.y-a.y;
      float dot =  (vx*vx + vy*vy);
      float rx = mouseX-a.x;
      float ry = mouseY-a.y;
      float dot2 =  (vx*rx + vy*ry);
      float value =  dot2/dot;
      value = min(value,1);
      value = max(value,0);
      float x = ((b.x*value)+(a.x*(1-value)));
      float y = ((b.y*value)+(a.y*(1-value)));
      line(x,y,mouseX,mouseY);
    }
    else {
      stroke(0);
    }
    line(a.x,a.y,b.x,b.y);
  }

  void applyForces() {
    double d = a.distanceTo(b);
    if (d>0)
    {
      double f = (d-restLength)*k.value;
      double fH = (f/d)*(a.x-b.x);
      double fV = (f/d)*(a.y-b.y);
      a.xv -= fH;
      a.yv -= fV;
      b.xv += fH;
      b.yv += fV;
    }
  }

  float distanceTo(float x,float y) {
    if (x>(min(a.x,b.x)-mouseTolerance)
    &&x<(max(a.x,b.x)+mouseTolerance)
    &&y>(min(a.y,b.y)-mouseTolerance)
    &&y<(max(a.y,b.y)+mouseTolerance))
    {
      float vx = b.x-a.x;
      float vy = b.y-a.y;
      float dot =  (vx*vx + vy*vy);
      float rx = x-a.x;
      float ry = y-a.y;
      float dot2 =  (vx*rx + vy*ry);
      float value =  dot2/dot;

      if (value<0) {
        float d = a.distanceTo(x,y);
        return d <= mouseTolerance?d:-1;
      } else if (value>1) {
        float d = b.distanceTo(x,y);
        return d <= mouseTolerance?d:-1;
      }

      float px = ((b.x*value)+(a.x*(1-value)))-x;
      float py = ((b.y*value)+(a.y*(1-value)))-y;

      float d = sqrt(px*px+py*py);

      return d <= mouseTolerance?d:-1;
    }
    else
    return  -1;
  }
}

