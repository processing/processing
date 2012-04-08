
class Segment {
  
  float x, y, z, len, angle;
  float xAbsolute, yAbsolute, zAbsolute, angleAbsolute;
  float originLength, futureLength;
  
  Segment parent = null;
  Segment[] children = new Segment[4];
  int childCounter = 0;
  int id;
  int depth;
  Branch branch = null;
  
  Segment(int _id) { id = _id;}
  
  void addChild(Segment _c) {children[childCounter++] = _c;} // println("adding child ->"+_c.id+"  "+childCounter);
  void setParent(Segment _p) {parent = _p;}
  
  void setXY(float _x,float _y) {x = _x; y = _y;}
  void setX(float _x) {x = _x;}
  void setY(float _y) {y = _y;}
  void setZ(float _z) {z = _z;}
  
  void setAngle(float _a) {angle = _a;}
  void setLength(float _len) {len = _len; originLength = _len; }
  void setFutureToOrigin() {futureLength = originLength;}
  void setFutureLength(float _len) {futureLength = _len;}
   
  void setXabsolute(float _xAbsolute) {xAbsolute = _xAbsolute;}
  void setYabsolute(float _yAbsolute) {yAbsolute = _yAbsolute;}
  void setZabsolute(float _zAbsolute) {zAbsolute = _zAbsolute;}
  void setAngleAbsolute(float _angleAbsolute) {angleAbsolute = _angleAbsolute;}
  
  void scaleLength(float _scalar) {len *= _scalar;}
  void scaleLengthFromOrigin(float _scalar) {len = originLength*_scalar;}
  void scaleFutureLength(float _scalar) {futureLength *= _scalar;}
  
  float getX() {return x;}
  float getY() {return y;}
  float getZ() {return z;}
  float getAngle() {return angle;}
  float getLength() {return len;}
  float getXabsolute() {return xAbsolute;}
  float getYabsolute() {return yAbsolute;}
  float getZabsolute() {return zAbsolute;}
  float getAngleAbsolute() {return angleAbsolute;}
  
  void calcCoords() {
    if(parent==null) {
       angleAbsolute = angle;
       setX(cos(radToPol(angle))*len);
       setY(sin(radToPol(angle))*len);
       setXabsolute(rootX+x); 
       setYabsolute(rootY-y);
    } else {
      angleAbsolute += ((parent.getAngleAbsolute()+angle)-angleAbsolute)/20;
      setX(cos(radToPol(angleAbsolute))*len);
      setY(sin(radToPol(angleAbsolute))*len);
      setXabsolute(parent.getXabsolute()+x);
      setYabsolute(parent.getYabsolute()-y);
    }
  }
  
  float radToPol(float _deg) { return _deg/57.2958; }
  float polToRad(float _pol) { return _pol*57.2958; }
  
  void render() {
    calcCoords();
    activateChildren();
    
    originLength += (futureLength-originLength)/100;
    len = originLength;
    
    if(parent==null) {
      drawAsLine(rootX,rootY,getXabsolute(), getYabsolute());
    } else {
      drawAsLine( parent.getXabsolute(),
                       parent.getYabsolute(),
                       getXabsolute(),
                       getYabsolute()
                     );
    }
  }
  
  void activateChildren() {
    for(int i=0;i<4;i++) {
      if(children[i]!=null) {
        children[i].render();
      }
    }
   }
  
  void setBranch(Branch _branch) {
    branch = _branch;
    try {
      parent = s[branch.parent.id];
       for(int i=0;i<4;i++) { 
         if(branch.children[i]!=null) {
           addChild(s[branch.children[i].id]); 
         }
      }
    } catch (NullPointerException npe) {
      for(int i=0; i<4; i++) { 
        if(branch.children[i] != null) { 
          addChild(s[branch.children[i].id]); 
        }
      }
    }
  }
  
  void setParamsFromBranch() {
    try {
      int tmp  = branch.parent.id;
      setParent(s[tmp]);
      setAngle(polToRad(branch.angle));
      setAngleAbsolute(polToRad(branch.angle));
    } catch (NullPointerException npe){
      setParent(null);
      setAngle(polToRad(branch.angle));
      setAngleAbsolute(polToRad(branch.angle));
      println("NULLPOINTER");
    }
    setLength(branch.len);
  }
  
  void adjustAngle(float _angle) {
     for(int i=0;i<4;i++) { 
       if(children[i]!=null) {
         children[i].adjustAngle(angle);
       }
     }
     angle -= _angle;
  }
  
  void adjustDepth(int _depth) {
    depth = _depth;
    for(int i=0;i<4;i++) { 
      if(children[i]!=null) { 
        children[i].adjustDepth(depth-1);
      }
    }
  }
  
  void drawAsLine(float _x1, float _y1, float _x2, float _y2) {
     strokeWeight(2);
     if(id == redNum) {
       stroke(255,0,0); 
     } else {
       stroke(0, 80);
     }
    line(_x1,_y1,_x2,_y2);
  }
  
}

