/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2005-08 Ben Fry and Casey Reas

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
 * A matrix is used to define graphical transformations. PMatrix is the common
 * interface for both the 2D and 3D matrix classes in Processing. A matrix is a
 * grid of numbers, which can be multiplied by a vector to give another vector.
 * Multiplying a point by a particular matrix might translate it, rotate it,
 * or carry out a combination of transformations.
 *
 * Multiplying matrices by each other combines their effects; use the
 * {@code apply} and {@code preApply} methods for this.
 */
public interface PMatrix {
  
  /**
   * Make this an identity matrix. Multiplying by it will have no effect.
   */
  void reset();
  
  /**
   * Returns a copy of this PMatrix.
   */
  PMatrix get();

  /**
   * Copies the matrix contents into a float array.
   * If target is null (or not the correct size), a new array will be created.
   */
  float[] get(float[] target);
  
  
  /**
   * Make this matrix become a copy of src.
   */
  void set(PMatrix src);

  /**
   * Set the contents of this matrix to the contents of source. Fills the
   * matrix left-to-right, starting in the top row.
   */
  void set(float[] source);

  /**
   * Set the matrix content to this 2D matrix or its 3D equivalent.
   */
  void set(float m00, float m01, float m02,
           float m10, float m11, float m12);

  /**
   * Set the matrix content to the 3D matrix supplied, if this matrix is 3D.
   */
  void set(float m00, float m01, float m02, float m03,
           float m10, float m11, float m12, float m13,
           float m20, float m21, float m22, float m23,
           float m30, float m31, float m32, float m33);

  
  void translate(float tx, float ty);
  
  void translate(float tx, float ty, float tz);

  void rotate(float angle);

  void rotateX(float angle);

  void rotateY(float angle);

  void rotateZ(float angle);

  void rotate(float angle, float v0, float v1, float v2);

  void scale(float s);

  void scale(float sx, float sy);

  void scale(float x, float y, float z);
  
  void shearX(float angle);
  
  void shearY(float angle);

  /**
   * Multiply this matrix by another.
   */
  void apply(PMatrix source);

  /**
   * Multiply this matrix by another.
   */
  void apply(PMatrix2D source);

  /**
   * Multiply this matrix by another.
   */
  void apply(PMatrix3D source);

  /**
   * Multiply this matrix by another.
   */
  void apply(float n00, float n01, float n02,
             float n10, float n11, float n12);

  /**
   * Multiply this matrix by another.
   */
  void apply(float n00, float n01, float n02, float n03,
             float n10, float n11, float n12, float n13,
             float n20, float n21, float n22, float n23,
             float n30, float n31, float n32, float n33);

  /**
   * Apply another matrix to the left of this one.
   */
  void preApply(PMatrix left);

  /**
   * Apply another matrix to the left of this one.
   */
  void preApply(PMatrix2D left);

  /**
   * Apply another matrix to the left of this one. 3D only.
   */
  void preApply(PMatrix3D left);

  /**
   * Apply another matrix to the left of this one.
   */
  void preApply(float n00, float n01, float n02,
                float n10, float n11, float n12);

  /**
   * Apply another matrix to the left of this one. 3D only.
   */
  void preApply(float n00, float n01, float n02, float n03,
                float n10, float n11, float n12, float n13,
                float n20, float n21, float n22, float n23,
                float n30, float n31, float n32, float n33);

  
  /**
   * Multiply source by this matrix, and return the result.
   * The result will be stored in target if target is non-null, and target
   * will then be the matrix returned. This improves performance if you reuse
   * target, so it's recommended if you call this many times in draw().
   */
  PVector mult(PVector source, PVector target);
  
  
  /**
   * Multiply a multi-element vector against this matrix.
   * Supplying and recycling a target array improves performance, so it's
   * recommended if you call this many times in draw().
   */
  float[] mult(float[] source, float[] target);
  
  
//  public float multX(float x, float y);
//  public float multY(float x, float y);
  
//  public float multX(float x, float y, float z);
//  public float multY(float x, float y, float z);
//  public float multZ(float x, float y, float z);  
  
  
  /**
   * Transpose this matrix; rows become columns and columns rows.
   */
  void transpose();

  
  /**
   * Invert this matrix. Will not necessarily succeed, because some matrices
   * map more than one point to the same image point, and so are irreversible.
   * @return true if successful
   */
  boolean invert();
  
  
  /**
   * @return the determinant of the matrix
   */
  float determinant();
}
