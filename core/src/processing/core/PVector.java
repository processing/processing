/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2004-07 Ben Fry and Casey Reas
  Copyright (c) 2001-04 Massachusetts Institute of Technology

  This library is free software; you can redistribute it and/or
  modify it under the terms of the GNU Lesser General Public
  License as published by the Free Software Foundation; either
  version 2.1 of the License, or (at your option) any later version.

  This library is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
  Lesser General Public License for more details.

  You should have received a copy of the GNU Lesser General
  Public License along with this library; if not, write to the
  Free Software Foundation, Inc., 59 Temple Place, Suite 330,
  Boston, MA  02111-1307  USA
 */

package processing.core;

/**
 * A class to describe a two or three dimensional vector.
 * <br>
 * Initially based on the Vector3D class by <a href="http://www.shiffman.net">Dan Shiffman</a>.
 */
public class PVector {
  
  /** The x component of the vector. */
  public float x;
  
  /** The y component of the vector. */
  public float y;
  
  /** The z component of the vector. */
  public float z;

  
  /**
   * Constructor for an empty vector: x, y, and z are set to 0.
   */
  public PVector() {
  }

  
  /**
   * Constructor for a 3D vector.
   *
   * @param  x the x coordinate.
   * @param  y the y coordinate.
   * @param  z the y coordinate.
   */
  public PVector(float x, float y, float z) {
    this.x = x;
    this.y = y;
    this.z = z;
  }

  
  /**
   * Constructor for a 2D vector: z coordinate is set to 0.
   *
   * @param  x the x coordinate.
   * @param  y the y coordinate.
   */
  public PVector(float x, float y) {
    this.x = x;
    this.y = y;
    this.z = 0;
  }
  
  
  /**
   * Set the x coordinate.
   *     
   *  @param  x the x coordinate.
   */
  public void setX(float x) {
    this.x = x;
  }

  /**
   * Set the y coordinate.
   *     
   * @param  y the y coordinate.
   */
  public void setY(float y) {
    this.y = y;
  }

  
  /**
   * Set the z coordinate.
   *     
   *  @param  z the z coordinate.
   */
  public void setZ(float z) {
    this.z = z;
  }

  
  /**
   * Set x,y, and z coordinates.
   *     
   *  @param  x_ the x coordinate.
   *  @param  y_ the y coordinate.
   *  @param  z_ the z coordinate.
   */
  public void set(float x, float y, float z) {
    this.x = x;
    this.y = y;
    this.z = z;
  }

  
  /**
   * Set x,y, and z coordinates from a Vector3D object.
   *     
   *  @param  v the Vector3D object to be copied
   */
  public void setXYZ(PVector v) {
    x = v.x;
    y = v.y;
    z = v.z;
  }

  /**
   * Calculate the magnitude (length) of the vector
   * @return      the magnitude of the vector    
   */
  public float magnitude() {
    return (float) Math.sqrt(x*x + y*y + z*z);
  }

  /**
   * Copy the vector
   * @return      a copy of the vector   
   */
  public PVector copy() {
    return new PVector(x,y,z);
  }

  /**
   * Copy the vector
   * @param      v the vector to be copied   
   * @return      a copy of the vector   
   */
  public static PVector copy(PVector v) {
    return new PVector(v.x, v.y,v.z);
  }

  /**
   * Add a vector to this vector
   * @param      v the vector to be added  
   */   
  public void add(PVector v) {
    x += v.x;
    y += v.y;
    z += v.z;
  }

  /**
   * Subtract a vector from this vector
   * @param      v the vector to be subtracted  
   */   
  public void sub(PVector v) {
    x -= v.x;
    y -= v.y;
    z -= v.z;
  }

  /**
   * Multiply this vector by a scalar
   * @param      n the value to multiply by 
   */     
  public void mult(float n) {
    x *= n;
    y *= n;
    z *= n;
  }

  /**
   * Divide this vector by a scalar
   * @param      n the value to divide by 
   */     
  public void div(float n) {
    x /= n;
    y /= n;
    z /= n;
  }


  /**
   * Calculate the dot product with another vector
   * @return  the dot product
   */     
  public float dot(PVector v) {
    float dot = x*v.x + y*v.y;
    return dot;
  }

  /**
   * Calculate the cross product with another vector
   * @return  the cross product
   */     
  public PVector cross(PVector v) {
    float crossX = y * v.z - v.y * z;
    float crossY = z * v.x - v.z * x;
    float crossZ = x * v.y - v.x * y;
    return(new PVector(crossX,crossY,crossZ));
  }

  /**
   * Normalize the vector to length 1 (make it a unit vector)
   */     
  public void normalize() {
    float m = magnitude();
    if (m > 0) {
      div(m);
    }
  }

  /**
   * Limit the magnitude of this vector
   * @param max the maximum length to limit this vector
   */     
  public void limit(float max) {
    if (magnitude() > max) {
      normalize();
      mult(max);
    }
  }

  /**
   * Calculate the angle of rotation for this vector (only 2D vectors)
   * @return the angle of rotation
   */    
  public float heading2D() {
    float angle = (float) Math.atan2(-y, x);
    return -1*angle;
  }

  /**
   * Add two vectors
   * @param      v1 a vector
   * @param v2 another vector   
   * @return a new vector that is the sum of v1 and v2  
   */   
  public static PVector add(PVector v1, PVector v2) {
    PVector v = new PVector(v1.x + v2.x,v1.y + v2.y, v1.z + v2.z);
    return v;
  }

  /**
   * Subtract one vector from another
   * @param      v1 a vector
   * @param v2 another vector   
   * @return a new vector that is v1 - v2 
   */    
  public static PVector sub(PVector v1, PVector v2) {
    PVector v = new PVector(v1.x - v2.x,v1.y - v2.y,v1.z - v2.z);
    return v;
  }

  /**
   * Divide a vector by a scalar
   * @param      v1 a vector
   * @param n scalar 
   * @return a new vector that is v1 / n
   */ 
  public static PVector div(PVector v1, float n) {
    PVector v = new PVector(v1.x/n,v1.y/n,v1.z/n);
    return v;
  }

  /**
   * Multiply a vector by a scalar
   * @param      v1 a vector
   * @param n scalar 
   * @return a new vector that is v1 * n
   */ 
  public static PVector mult(PVector v1, float n) {
    PVector v = new PVector(v1.x*n,v1.y*n,v1.z*n);
    return v;
  }


  /**
   * Calculate the Euclidean distance between two points (considering a point as a vector object)
   * @param      v1 a vector
   * @param v2 another vector
   * @return the Euclidean distance between v1 and v2
   */ 
  public static float distance (PVector v1, PVector v2) {
    float dx = v1.x - v2.x;
    float dy = v1.y - v2.y;
    float dz = v1.z - v2.z;
    return (float) Math.sqrt(dx*dx + dy*dy + dz*dz);
  }

  /**
   * Calculate the angle between two vectors, using the dot product
   * @param      v1 a vector
   * @param v2 another vector
   * @return the angle between the vectors
   */ 
  public static float angleBetween(PVector v1, PVector v2) {
    float dot = v1.dot(v2);
    float theta = (float) Math.acos(dot / (v1.magnitude() * v2.magnitude()));
    return theta;
  }

}


