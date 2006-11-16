package agg;


public class rect {

  float x1, y1, x2, y2;


  public rect() {
  }


  public rect(float x1_, float y1_, float x2_, float y2_) {
    x1 = x1_;
    y1 = y1_;
    x2 = x2_;
    y2 = y2_;
  }


  public rect normalize() {
    if (x1 > x2) {
      float t = x1; x1 = x2; x2 = t;
    }
    if (y1 > y2) {
      float t = y1; y1 = y2; y2 = t;
    }
  }


  public boolean clip(rect r) {
    if (x2 > r.x2) x2 = r.x2;
    if (y2 > r.y2) y2 = r.y2;
    if (x1 < r.x1) x1 = r.x1;
    if (y1 < r.y1) y1 = r.y1;
    return x1 <= x2 && y1 <= y2;
  }


  public boolean is_valid() {
    return x1 <= x2 && y1 <= y2;
  }


  /*
    template<class T> struct rect_base
    {
        typedef rect_base<T> self_type;
        T x1;
        T y1;
        T x2;
        T y2;

        rect_base() {}
        rect_base(T x1_, T y1_, T x2_, T y2_) :
            x1(x1_), y1(y1_), x2(x2_), y2(y2_) {}

        const self_type& normalize()
        {
            T t;
            if(x1 > x2) { t = x1; x1 = x2; x2 = t; }
            if(y1 > y2) { t = y1; y1 = y2; y2 = t; }
            return *this;
        }

        bool clip(const self_type& r)
        {
            if(x2 > r.x2) x2 = r.x2;
            if(y2 > r.y2) y2 = r.y2;
            if(x1 < r.x1) x1 = r.x1;
            if(y1 < r.y1) y1 = r.y1;
            return x1 <= x2 && y1 <= y2;
        }

        bool is_valid() const
        {
            return x1 <= x2 && y1 <= y2;
        }
    };
  */


  static final public rect intersect_rectangles(rect r1, rect r2) {
    rect r = new rect(r1);

    if (r.x2 > r2.x2) r.x2 = r2.x2;
    if (r.y2 > r2.y2) r.y2 = r2.y2;
    if (r.x1 < r2.x1) r.x1 = r2.x1;
    if (r.y1 < r2.y1) r.y1 = r2.y1;
    return r;
  }


  /*
    template<class Rect>
    inline Rect intersect_rectangles(const Rect& r1, const Rect& r2)
    {
        Rect r = r1;

        // First process x2,y2 because the other order
        // results in Internal Compiler Error under
        // Microsoft Visual C++ .NET 2003 69462-335-0000007-18038 in
        // case of "Maximize Speed" optimization option.
        //-----------------
        if(r.x2 > r2.x2) r.x2 = r2.x2;
        if(r.y2 > r2.y2) r.y2 = r2.y2;
        if(r.x1 < r2.x1) r.x1 = r2.x1;
        if(r.y1 < r2.y1) r.y1 = r2.y1;
        return r;
    }
  */


  static final public rect unite_rectangles(rect r1, rect r2) {
    rect r = new rect(r1);
    if(r.x2 < r2.x2) r.x2 = r2.x2;
    if(r.y2 < r2.y2) r.y2 = r2.y2;
    if(r.x1 > r2.x1) r.x1 = r2.x1;
    if(r.y1 > r2.y1) r.y1 = r2.y1;
    return r;
  }

  /*
  typedef rect_base<int>    rect;   //----rect
  typedef rect_base<double> rect_d; //----rect_d
  */
}