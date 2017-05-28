
/**
 * This class implements complex numbers in java
 * 
 * @autor Raphael Augusto
 * 
 * */
public class PComplex {
  
  /** Real part of complex number */
  private double a;
  
  /** Imaginary part of complex number */
  private double b;
  
  
  /**
   * Constructor of the PComplex class
   * 
   * @author Raphael Augusto
   * @param real Real part of complex number
   * @param imaginary Imaginary part of complex number
   * 
   * */
  public PComplex(double real, double imaginary) {
    a = real;
    b = imaginary;
  }

  
  /**
   * Constructor of the PComplex class, where the values 
   * of real and imaginary is equals to zero
   * 
   * @author Raphael Augusto
   * 
   * */
  public PComplex() {
    this(0, 0);
  }

    
  /**
   * Set the values of complex number
   * 
   * @author Raphael Augusto
   * @param real Real part of complex number
   * @param imaginary Imaginary part of complex number
   * 
   * */
  public PComplex set(double real, double imaginary) {
  	a = real;
  	b = imaginary;
  	return this;
  }
  
  
  /**
   * Get the value of real part of complex number
   *  
   * @autor Raphael Augusto
   * @return These function return the real part of 
   *         complex number
   * 
   * */
  public float realPart() {
  	return (float) a;
  }

  
  /**
   * Get the value of imaginary part of complex number
   *  
   * @autor Raphael Augusto
   * @return These function return the imaginary part of 
   *         complex number
   * 
   * */
  public float imaginaryPart() {
  	return (float) b;
  }

  
  /**
   * Set the value of real part of complex number
   *  
   * @autor Raphael Augusto
   * @param real Real part of complex number
   * 
   * */
  public PComplex realPart(double real) {
  	a = real;
  	return this;
  }

  
  /**
   * Set the value of imaginary part of complex number
   *  
   * @autor Raphael Augusto
   * @param imaginary Imaginary part of complex number
   * 
   * */
  public PComplex imaginaryPart(double imaginary) {
  	b = imaginary;
  	return this;
  }
  
  
  /**
   * These functions shall compute the complex absolute value 
   * (also called norm, modulus, or magnitude) of z.
   * 
   * @autor Raphael Augusto
   * @return These function return the modulus of complex number
   * 
   * */
  public float abs() {
  	return (float) Math.sqrt(a*a + b*b);
  }
  
  
  /**
   * Theses function shall compute the argument of complex number
   * 
   * @autor Raphael Augusto
   * @return These function return the modulus of complex number
   * 
   * */
  public float arg() {
  	return (float) Math.atan2(b, a);
  }
  
  
  /**
   *  These functions shall compute the complex conjugate of z,
   *  by reversing the sign of its imaginary part.
   * 
   * @autor Raphael Augusto
   * @return These functions return the complex conjugate value.
   * 
   * */
  public PComplex conj() {
  	return new PComplex(a, -b);
  }

  
  /**
   *  This function multiply a number by its conjugate
   * 
   * @autor Raphael Augusto
   * 
   * */
  public float multConj() {
  	return (float) (a*a + b*b);
  }
  
  
  /**
   *  These function add a complex number
   * 
   * @autor Raphael Augusto
   * @param c Complex number
   * 
   * */
  public PComplex add(PComplex c) {
  	a += c.a;
  	b += c.b;
  	return this;
  }
  
  
  /**
   *  These function subtract a complex number
   * 
   * @autor Raphael Augusto
   * @param c Complex number
   * 
   * */
  public PComplex sub(PComplex c) {
  	a -= c.a;
  	b -= c.b;
  	return this;
  }
  
  
  /**
   *  These function multiply a complex number
   * 
   * @autor Raphael Augusto
   * @param c Complex number
   * 
   * */
  public PComplex mult(PComplex c) {
  	double ta = a * c.a - b * c.b;
  	double tb = a * c.b + b * c.a;
  	a = ta;
  	b = tb;
  	return this;
  }

  
  /**
   *  These function multiply a complex number by scalar
   * 
   * @autor Raphael Augusto
   * @param k Scalar
   * 
   * */
  public PComplex mult(double k) {
  	a *= k;
  	b *= k;
  	return this;
  }
  
  
  /**
   *  These function divide a complex number 
	 *
   * @autor Raphael Augusto
   * @param c Complex number
   * 
   * */
  public PComplex div(PComplex c) {
  	mult(c.conj());
  	div(c.multConj());
  	return this;
  }

  
  /**
   *  These function divide a complex number by scalar
   * 
   * @autor Raphael Augusto
   * @param k Scalar
   * 
   * */
  public PComplex div(double k) {
  	mult(1.0 / k);
  	return this;
  }
  
 
  /**
   *  These function compute the exponential of a complex number
   * 
   * @autor Raphael Augusto
   * @param c Complex number
   * 
   * */
  public static PComplex exp(PComplex c) {
  	double e = Math.exp(c.a); 
  	return new PComplex(
  			e * Math.cos(c.b),
  			e * Math.sin(c.b)
  	);
  }
  
  
  /**
   *  These function compute the logarithm of a complex number
   * 
   * @autor Raphael Augusto
   * @param c Complex number
   * 
   * */
  public static PComplex ln(PComplex c) {
  	return new PComplex(
  			c.abs(),
  			c.arg()
  	);
  }
}

