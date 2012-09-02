// The Nature of Code
// <http://www.shiffman.net/teaching/nature>
// Spring 2011
// PBox2D example

// Just demo-ing the basics of Vec2 vs. PVector

import org.jbox2d.common.*;

void setup() {
  size(400,300);
//  PVector a = new PVector(1,-1);
//  PVector b = new PVector(3,4);
//  a.add(b);
//
//  PVector a = new PVector(1,-1);
//  PVector b = new PVector(3,4);
//  PVector c = PVector.add(a,b);
//
//  Vec2 a = new Vec2(1,-1);
//  Vec2 b = new Vec2(3,4);
//  a.addLocal(b);
//
//  Vec2 a = new Vec2(1,-1);
//  Vec2 b = new Vec2(3,4);
//  Vec2 c = a.add(b);
//
//  PVector a = new PVector(1,-1);
//  float n = 5;
//  a.mult(n);
//
//  PVector a = new PVector(1,-1);
//  float n = 5;
//  PVector c = PVector.mult(a,n);
//
//  Vec2 a = new Vec2(1,-1);
//  float n = 5;
//  a.mulLocal(n);
//
//  Vec2 a = new Vec2(1,-1);
//  float n = 5;
//  Vec2 c = a.mul(n);
//
//  PVector a = new PVector(1,-1);
//  float m = a.mag();
//  a.normalize();

  Vec2 a = new Vec2(1,-1);
  float m = a.length();
  a.normalize();
  println(a.x + "," + a.y);
}

void draw() {
  noLoop();
}

