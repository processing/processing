// Code from Visualizing Data, First Edition, Copyright 2008 Ben Fry.


public class BoundsIntegrator {

  static final float ATTRACTION = 0.2f;
  static final float DAMPING = 0.5f;

  float valueX, velocityX, accelerationX;
  float valueY, velocityY, accelerationY;
  float valueW, velocityW, accelerationW;
  float valueH, velocityH, accelerationH;

  float damping;
  float attraction;
  
  boolean targeting;
  float targetX, targetY, targetW, targetH;


  public BoundsIntegrator() {
    this.valueX = 0;
    this.valueY = 0;
    this.valueW = 1;
    this.valueH = 1;

    this.damping = DAMPING;
    this.attraction = ATTRACTION;
  }


  public BoundsIntegrator(float x, float y, float w, float h) {
    this.valueX = x;
    this.valueY = y;
    this.valueW = w;
    this.valueH = h;
    
    this.damping = DAMPING;
    this.attraction = ATTRACTION;
  }


  public void set(float x, float y, float w, float h) {
    this.valueX = x;
    this.valueY = y;
    this.valueW = w;
    this.valueH = h;
  }
  
  
  public float getX() {
    return valueX;
  }
  
  
  public float getY() {
    return valueY;
  }
  
  
  public float getW() {
    return valueW;
  }
  
  
  public float getH() {
    return valueH;
  }
  
  
  public float spanX(float pointX, float start, float span) {
    if (valueW != 0) {
      //return (pointX - valueX) / valueW;
      float n = (pointX - valueX) / valueW;
      return start + n*span;
    } else {
      return Float.NaN;
    }
  }
    
  
  public float spanY(float pointY, float start, float span) {
    if (valueH != 0) {
      //return (pointY - valueY) / valueH;
      float n = (pointY - valueY) / valueH;
      return start + n*span;
    } else {
      return Float.NaN;
    }
  }
  
  
  public void setAttraction(float a) {
    attraction = a;
  }
  
  
  public void setDamping(float d) {
    damping = d;
  }


  public boolean update() {
    if (targeting) {
      accelerationX += attraction * (targetX - valueX);
      velocityX = (velocityX + accelerationX) * damping;
      valueX += velocityX;
      accelerationX = 0;
      boolean updated = (Math.abs(velocityX) > 0.0001f);
      
      accelerationY += attraction * (targetY - valueY);
      velocityY = (velocityY + accelerationY) * damping;
      valueY += velocityY;
      accelerationY = 0;
      updated |= (Math.abs(velocityY) > 0.0001f);

      accelerationW += attraction * (targetW - valueW);
      velocityW = (velocityW + accelerationW) * damping;
      valueW += velocityW;
      accelerationW = 0;
      updated |= (Math.abs(velocityW) > 0.0001f);
      
      accelerationH += attraction * (targetH - valueH);
      velocityH = (velocityH + accelerationH) * damping;
      valueH += velocityH;
      accelerationH = 0;
      updated |= (Math.abs(velocityH) > 0.0001f);
    }
    return false;
  }


  public void target(float tx, float ty, float tw, float th) {
    targeting = true;
    targetX = tx;
    targetY = ty; 
    targetW = tw;
    targetH = th;
  }


  public void targetLocation(float tx, float ty) {
    targeting = true;
    targetX = tx;
    targetY = ty;
  }
  
  
  public void targetSize(float tw, float th) {
    targeting = true;
    targetW = tw;
    targetH = th;
  }
  
  
  public void targetX(float tx) {
    targeting = true;
    targetX = tx;
  }
  
  
  public void targetY(float ty) {
    targeting = true;
    targetY = ty;
  }

  
  public void targetW(float tw) {
    targeting = true;
    targetW = tw;
  }

  
  public void targetH(float th) {
    targeting = true;
    targetH = th;
  }
}
