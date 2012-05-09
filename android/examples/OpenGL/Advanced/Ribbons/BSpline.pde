final int MAX_BEZIER_ORDER = 10; // Maximum curve order.

final float[][] BSplineMatrix = {
  {-1.0/6.0,  1.0/2.0, -1.0/2.0, 1.0/6.0},
  { 1.0/2.0,     -1.0,  1.0/2.0,     0.0},
  {-1.0/2.0,      0.0,  1.0/2.0,     0.0},
  { 1.0/6.0,  2.0/3.0,  1.0/6.0,     0.0}
};

// The element(i, n) of this array contains the binomial coefficient
// C(i, n) = n!/(i!(n-i)!)
final int[][] BinomialCoefTable = {
  {1, 1, 1, 1,  1,  1,  1,  1,   1,   1},
  {1, 2, 3, 4,  5,  6,  7,  8,   9,  10},
  {0, 1, 3, 6, 10, 15, 21, 28,  36,  45},
  {0, 0, 1, 4, 10, 20, 35, 56,  84, 120},
  {0, 0, 0, 1,  5, 15, 35, 70, 126, 210},
  {0, 0, 0, 0,  1,  6, 21, 56, 126, 252},
  {0, 0, 0, 0,  0,  1,  7, 28,  84, 210},
  {0, 0, 0, 0,  0,  0,  1,  8,  36, 120},
  {0, 0, 0, 0,  0,  0,  0,  1,   9,  45},
  {0, 0, 0, 0,  0,  0,  0,  0,   1,  10},
  {0, 0, 0, 0,  0,  0,  0,  0,   0,   1}
};

// The element of this(i, j) of this table contains(i/10)^(3-j).
final float[][] TVectorTable = {  
//   t^3,  t^2, t^1, t^0
  {    0,    0,   0,   1}, // t = 0.0
  {0.001, 0.01, 0.1,   1}, // t = 0.1
  {0.008, 0.04, 0.2,   1}, // t = 0.2
  {0.027, 0.09, 0.3,   1}, // t = 0.3
  {0.064, 0.16, 0.4,   1}, // t = 0.4
  {0.125, 0.25, 0.5,   1}, // t = 0.5
  {0.216, 0.36, 0.6,   1}, // t = 0.6
  {0.343, 0.49, 0.7,   1}, // t = 0.7
  {0.512, 0.64, 0.8,   1}, // u = 0.8
  {0.729, 0.81, 0.9,   1}, // t = 0.9
  {    1,    1,   1,   1}  // t = 1.0
};

// The element of this(i, j) of this table contains(3-j)*(i/10)^(2-j) if
// j < 3, 0 otherwise.
final float[][] DTVectorTable = { 
// 3t^2,  2t^1, t^0
  {   0,     0,   1, 0}, // t = 0.0
  {0.03,   0.2,   1, 0}, // t = 0.1
  {0.12,   0.4,   1, 0}, // t = 0.2
  {0.27,   0.6,   1, 0}, // t = 0.3
  {0.48,   0.8,   1, 0}, // t = 0.4
  {0.75,   1.0,   1, 0}, // t = 0.5
  {1.08,   1.2,   1, 0}, // t = 0.6
  {1.47,   1.4,   1, 0}, // t = 0.7
  {1.92,   1.6,   1, 0}, // t = 0.8
  {2.43,   1.8,   1, 0}, // t = 0.9
  {   3,     2,   1, 0}  // t = 1.0
};

abstract class Curve3D {
  abstract void feval(float t, PVector p);
  abstract void deval(float t, PVector d);
  abstract float fevalX(float t);
  abstract float fevalY(float t);
  abstract float fevalZ(float t);
  abstract float devalX(float t);
  abstract float devalY(float t);
  abstract float devalZ(float t);
}

abstract class Spline extends Curve3D {
  // The factorial of n.
  int factorial(int n) { 
    return n <= 0 ? 1 : n * factorial(n - 1); 
  }
  
  // Gives n!/(i!(n-i)!).
  int binomialCoef(int i, int n) {
    if ((i <= MAX_BEZIER_ORDER) && (n <= MAX_BEZIER_ORDER)) return BinomialCoefTable[i][n - 1];
    else return int(factorial(n) / (factorial(i) * factorial(n - i)));
  }
  
  // Evaluates the Berstein polinomial(i, n) at u.
  float bersteinPol(int i, int n, float u) {
    return binomialCoef(i, n) * pow(u, i) * pow(1 - u, n - i);
  }
  
  // The derivative of the Berstein polinomial.
  float dbersteinPol(int i, int n, float u) {
    float s1, s2; 
    if (i == 0) s1 = 0; 
    else s1 = i * pow(u, i-1) * pow(1 - u, n - i);
    if (n == i) s2 = 0; 
    else s2 = -(n - i) * pow(u, i) * pow(1 - u, n - i - 1);
    return binomialCoef(i, n) *(s1 + s2);
  }
}

class BSpline extends Spline {
  // Control points.
  float[][] bsplineCPoints;

  // Parameters.
  boolean lookup;

  // Auxiliary arrays used in the calculations.
  float[][] M3;
  float[] TVector, DTVector;

  // Point and tangent vectors.
  float[] pt, tg;
  
  BSpline() { 
    initParameters(true); 
  }
  
  BSpline(boolean t) { 
    initParameters(t); 
  }

  // Sets lookup table use.
  void initParameters(boolean t) { 
    bsplineCPoints = new float[4][3];
    TVector = new float[4];
    DTVector = new float[4]; 
    M3 = new float[4][3];
    pt = new float[3];
    tg = new float[3];        
    lookup = t;
  }
  
  // Sets n-th control point.
  void setCPoint(int n, PVector P) {
    bsplineCPoints[n][0] = P.x;
    bsplineCPoints[n][1] = P.y;
    bsplineCPoints[n][2] = P.z;        
    updateMatrix3();
  }

  // Gets n-th control point.
  void getCPoint(int n, PVector P) {
    P.set(bsplineCPoints[n]);
  }

  // Replaces the current B-spline control points(0, 1, 2) with(1, 2, 3). This
  // is used when a new spline is to be joined to the recently drawn.
  void shiftBSplineCPoints() {
    for (int i = 0; i < 3; i++) {
      bsplineCPoints[0][i] = bsplineCPoints[1][i];
      bsplineCPoints[1][i] = bsplineCPoints[2][i];
      bsplineCPoints[2][i] = bsplineCPoints[3][i];
    }
    updateMatrix3();
  }

  void copyCPoints(int n_source, int n_dest) {
    for (int i = 0; i < 3; i++) {
      bsplineCPoints[n_dest][i] = bsplineCPoints[n_source][i];
    }
  }

  // Updates the temporal matrix used in order 3 calculations.
  void updateMatrix3() {
    float s; 
    int i, j, k;
    for(i = 0; i < 4; i++) {
      for(j = 0; j < 3; j++) {
        s = 0;
        for(k = 0; k < 4; k++) s += BSplineMatrix[i][k] * bsplineCPoints[k][j];
        M3[i][j] = s;
      }
    }
  }    

  void feval(float t, PVector p) { 
    evalPoint(t); 
    p.set(pt); 
  }
  
  void deval(float t, PVector d) { 
    evalTangent(t); 
    d.set(tg); 
  }

  float fevalX(float t) { 
    evalPoint(t); 
    return pt[0]; 
  }
  
  float fevalY(float t) { 
    evalPoint(t); 
    return pt[1]; 
  }
  
  float fevalZ(float t) { 
    evalPoint(t); 
    return pt[2]; 
  }

  float devalX(float t) { 
    evalTangent(t); 
    return tg[0]; 
  }
  
  float devalY(float t) { 
    evalTangent(t); 
    return tg[1]; 
  }
  
  float devalZ(float t) { 
    evalTangent(t); 
    return tg[2]; 
  }

  // Point evaluation.
  void evalPoint(float t) {
    if (lookup) {
      bsplinePointI(int(10 * t));
    } else {
      bsplinePoint(t);
    }
  }    

  // Tangent evaluation.
  void evalTangent(float t) {
    if (lookup) {
      bsplineTangentI(int(10 * t));
    } else {
      bsplineTangent(t);
    }
  }    

  // Calculates the point on the cubic spline corresponding to the parameter value t in [0, 1].
  void bsplinePoint(float t) {
    // Q(u) = UVector * BSplineMatrix * BSplineCPoints

    float s;
    int i, j, k;

    for(i = 0; i < 4; i++) {
      TVector[i] = pow(t, 3 - i);
    }

    for(j = 0; j < 3; j++) {
      s = 0;
      for(k = 0; k < 4; k++) {
        s += TVector[k] * M3[k][j];
      }
      pt[j] = s;
    }
  }

  // Calculates the tangent vector of the spline at t.
  void bsplineTangent(float t) {
    // Q(u) = DTVector * BSplineMatrix * BSplineCPoints

    float s;
    int i, j, k;

    for(i = 0; i < 4; i++) {
      if (i < 3) {
        DTVector[i] = (3 - i) * pow(t, 2 - i);
      } else {
        DTVector[i] = 0;
      }
    }

    for(j = 0; j < 3; j++) {
      s = 0;
      for(k = 0; k < 4; k++) {
        s += DTVector[k] * M3[k][j];
      }
      tg[j] = s;
    }
  }

  // Gives the point on the cubic spline corresponding to t/10(using the lookup table).
  void bsplinePointI(int t) {
    // Q(u) = TVectorTable[u] * BSplineMatrix * BSplineCPoints

    float s;
    int j, k;

    for(j = 0; j < 3; j++) {
      s = 0;
      for(k = 0; k < 4; k++) {
        s += TVectorTable[t][k] * M3[k][j];
      }
      pt[j] = s;
    }
  }

  // Calulates the tangent vector of the spline at t/10.
  void bsplineTangentI(int t) {
    // Q(u) = DTVectorTable[u] * BSplineMatrix * BSplineCPoints

    float s;
    int j, k;

    for(j = 0; j < 3; j++) {
      s = 0;
      for(k = 0; k < 4; k++) {
        s += DTVectorTable[t][k] * M3[k][j];
      }
      tg[j] = s;
    }
  }    
}
