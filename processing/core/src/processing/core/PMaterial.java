/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2008 Ben Fry and Casey Reas

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


public class PMaterial {
  /** Ka parameters of the material. */ 
  public float ambientR, ambientG, ambientB;
  
  /** Kd parameters of the material */
  public float diffuseR, diffuseG, diffuseB;
  
  /** Ks parameters of the material */
  public float specularR, specularG, specularB;
  
  /** Ke parameters of the material */
  public float emissiveR, emissiveG, emissiveB;

  /** Se parameter of the material */
  public float shininess; // Se


  public PMaterial(float ambientR, float ambientG, float ambientB,
                   float diffuseR, float diffuseG, float diffuseB,
                   float specularR, float specularG, float specularB,
                   float emissiveR, float emissiveG, float emissiveB,
                   float shininess) {
    ambient(ambientR, ambientG, ambientB);
    diffuse(diffuseR, diffuseG, diffuseB);
    specular(specularR, specularG, specularB);
    emissive(emissiveR, emissiveG, emissiveB);
    shininess(shininess);
  }


  public void ambient(float r, float g, float b) {
    ambientR = r;
    ambientG = g;
    ambientB = b;
  }

  
  public void diffuse(float r, float g, float b) {
    diffuseR = r;
    diffuseG = g;
    diffuseB = b;
  }


  public void specular(float r, float g, float b) {
    specularR = r;
    specularG = g;
    specularB = b;
  }
  
  
  public void emissive(float r, float g, float b) {
    emissiveR = r;
    emissiveG = g;
    emissiveB = b;
  }
  
  
  public void shininess(float s) {
    shininess = s;
  }
  
  
  /*
  static public PMaterial BRASS =
    new PMaterial(0.329412f, 0.223529f, 0.027451f, 1,
                  0.780392f, 0.568627f, 0.113725f, 1,
                  0.992157f, 0.941176f, 0.807843f, 1,
                  27.89740f);

  static public PMaterial BRONZE =
    new PMaterial(0.212500f, 0.127500f, 0.054000f, 1,
                  0.714000f, 0.428400f, 0.181440f, 1,
                  0.393548f, 0.271906f, 0.166721f, 1,
                  25.60000f);

  static public PMaterial POLISHED_BRONZE =
    new PMaterial(0.250000f, 0.148000f, 0.064750f, 1,
                  0.400000f, 0.236800f, 0.103600f, 1,
                  0.774597f, 0.458561f, 0.200621f, 1,
                  76.80000f);

  static public PMaterial CHROME =
    new PMaterial(0.250000f, 0.250000f, 0.250000f, 1,
                  0.400000f, 0.400000f, 0.400000f, 1,
                  0.774597f, 0.774597f, 0.774597f, 1,
                  76.80000f);

  static public PMaterial COPPER =
    new PMaterial(0.191250f, 0.073500f, 0.022500f, 1,
                  0.703800f, 0.270480f, 0.082800f, 1,
                  0.256777f, 0.137622f, 0.086014f, 1,
                  12.80000f);

  static public PMaterial POLISHED_COPPER =
    new PMaterial(0.229500f, 0.088250f, 0.027500f, 1f,
                  0.550800f, 0.211800f, 0.066000f, 1f,
                  0.580594f, 0.223257f, 0.069570f, 1f,
                  51.20000f);

  static public PMaterial GOLD =
    new PMaterial(0.247250f, 0.199500f, 0.074500f, 1,
                  0.751640f, 0.606480f, 0.226480f, 1,
                  0.628281f, 0.555802f, 0.366065f, 1,
                  51.20000f);

  static public PMaterial POLISHED_GOLD =
    new PMaterial(0.247250f, 0.224500f, 0.064500f, 1,
                  0.346150f, 0.314300f, 0.090300f, 1,
                  0.797357f, 0.723991f, 0.208006f, 1,
                  83.20000f);

  static public PMaterial PEWTER =
    new PMaterial(0.105882f, 0.058824f, 0.113725f, 1,
                  0.427451f, 0.470588f, 0.541176f, 1,
                  0.333333f, 0.333333f, 0.521569f, 1,
                  9.846150f);

  static public PMaterial SILVER =
    new PMaterial(0.192250f, 0.192250f, 0.192250f, 1,
                  0.507540f, 0.507540f, 0.507540f, 1,
                  0.508273f, 0.508273f, 0.508273f, 1,
                  51.20000f);

  static public PMaterial POLISHED_SILVER =
    new PMaterial(0.231250f, 0.231250f, 0.231250f, 1,
                  0.277500f, 0.277500f, 0.277500f, 1,
                  0.773911f, 0.773911f, 0.773911f, 1,
                  89.59999f);

  static public PMaterial EMERALD =
    new PMaterial(0.021500f, 0.174500f, 0.021500f, 0.55f,
                  0.075680f, 0.614240f, 0.075680f, 0.55f,
                  0.633000f, 0.727811f, 0.633000f, 0.55f,
                  76.80000f);

  static public PMaterial JADE =
    new PMaterial(0.135000f, 0.222500f, 0.157500f, 0.95f,
                  0.540000f, 0.890000f, 0.630000f, 0.95f,
                  0.316228f, 0.316228f, 0.316228f, 0.95f,
                  12.80000f);

  static public PMaterial OBSIDIAN =
    new PMaterial(0.053750f, 0.050000f, 0.066250f, 0.82f,
                  0.182750f, 0.170000f, 0.225250f, 0.82f,
                  0.332741f, 0.328634f, 0.346435f, 0.82f,
                  38.40000f);

  static public PMaterial PEARL =
    new PMaterial(0.250000f, 0.207250f, 0.207250f, 0.922f,
                  1.000000f, 0.829000f, 0.829000f, 0.922f,
                  0.296648f, 0.296648f, 0.296648f, 0.922f,
                  11.26400f);

  static public PMaterial RUBY =
    new PMaterial(0.174500f, 0.011750f, 0.011750f, 0.55f,
                  0.614240f, 0.041360f, 0.041360f, 0.55f,
                  0.727811f, 0.626959f, 0.626959f, 0.55f,
                  76.80000f);

  static public PMaterial TURQUOISE =
    new PMaterial(0.100000f, 0.187250f, 0.174500f, 0.8f,
                  0.396000f, 0.741510f, 0.691020f, 0.8f,
                  0.297254f, 0.308290f, 0.306678f, 0.8f,
                  12.80000f);

  static public PMaterial BLACK_PLASTIC =
    new PMaterial(0.000000f, 0.000000f, 0.000000f, 1,
                  0.010000f, 0.010000f, 0.010000f, 1,
                  0.500000f, 0.500000f, 0.500000f, 1,
                  32.00000f);

  static public PMaterial BLACK_RUBBER =
    new PMaterial(0.020000f, 0.020000f, 0.020000f, 1,
                  0.010000f, 0.010000f, 0.010000f, 1,
                  0.400000f, 0.400000f, 0.400000f, 1,
                  10.00000f);
  */
}
