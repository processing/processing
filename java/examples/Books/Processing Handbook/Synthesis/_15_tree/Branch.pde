
class Branch {
  int x1, y1, z1, x2, y2, z2, id;
  String xyz1, xyz2;  // xyz1 = starting point; xyz2 = endingpoint
  
  Branch parent = null;
  Branch[] children;
  int childCounter = 0;
  float len,angle;
  
  Branch(String _id, String _xyz1, String _xyz2) {
    id = stringToInt(_id);
    String[] tmpList = split(_xyz1,' ');
    x1 = stringToInt(tmpList[0]);
    y1 = stringToInt(tmpList[1]);
    z1 = stringToInt(tmpList[2]);
    tmpList = split(_xyz2,' ');
    x2 = stringToInt(tmpList[0]);
    y2 = stringToInt(tmpList[1]);
    z2 = stringToInt(tmpList[2]);
    xyz1 = _xyz1;
    xyz2 = _xyz2;
    children = new Branch[4];
    calc2Dcoords();
  }
  
  void calc2Dcoords() {
    len = sqrt(sq(this.x2-this.x1)+sq(this.y2-this.y1));
    angle = atan2(this.y2-this.y1,this.x2-this.x1);
  }
  
  int stringToInt(String s) {
    Integer tmp = new Integer(s);
    return tmp.intValue();
  }

  boolean checkParent(Branch _b) {
    if((x2==_b.x1) && (y2==_b.y1) && (z2==_b.z1)) {
      children[this.childCounter++] = _b;
      return true;
    } else {
      return false;
    }
  }
  
  void checkForParents() {
    for(int i=0;i<b.length;i++) {
      if(i!=id) {
        if(b[i].checkParent(this)) parent = b[i];
      }
    }
  }
  
  void printChildren() {
    print("this->"+this.id+" ***   child(s) ->");
    for(int i=0;i<children.length;i++) { 
      if(children[i]!=null) {
        print(" ,"+children[i].id);
      }
    }
    if(parent!=null) {
      print(" ***  parent->"+parent.id); 
    } else {
      print(" *** ROOT");
    }
    println("");
  }
  
}
