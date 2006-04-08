package agg;


public class agg_basics {

  /*
    typedef unsigned char cover_type;    //----cover_type
    enum cover_scale_e
    {
        cover_shift = 8,                 //----cover_shift
        cover_size  = 1 << cover_shift,  //----cover_size
        cover_mask  = cover_size - 1,    //----cover_mask
        cover_none  = 0,                 //----cover_none
        cover_full  = cover_mask         //----cover_full
    };
  */
  public static final int cover_shift = 8;
  public static final int cover_size  = 1 << cover_shift;
  public static final int cover_mask  = cover_size - 1;
  public static final int cover_none  = 0;
  public static final int cover_full  = cover_mask;

  /*
  const double pi = 3.14159265358979323846;

  inline double deg2rad(double deg)
  {
    return deg * pi / 180.0;
  }

  inline double rad2deg(double rad)
  {
    return rad * 180.0 / pi;
  }
  */

  public static final int path_cmd_stop     = 0;
  public static final int path_cmd_move_to  = 1;
  public static final int path_cmd_line_to  = 2;
  public static final int path_cmd_curve3   = 3;
  public static final int path_cmd_curve4   = 4;
  public static final int path_cmd_curveN   = 5;
  public static final int path_cmd_catrom   = 6;
  public static final int path_cmd_ubspline = 7;
  public static final int path_cmd_end_poly = 0x0F;
  public static final int path_cmd_mask     = 0x0F;

  public static final int path_flags_none  = 0;
  public static final int path_flags_ccw   = 0x10;
  public static final int path_flags_cw    = 0x20;
  public static final int path_flags_close = 0x40;
  public static final int path_flags_mask  = 0xF0;


  final static public boolean is_vertex(unsigned c) {
    return c >= path_cmd_move_to && c < path_cmd_end_poly;
  }


  final static public boolean is_drawing(unsigned c) {
    return c >= path_cmd_line_to && c < path_cmd_end_poly;
  }


  final static public boolean is_stop(unsigned c) {
    return c == path_cmd_stop;
  }


  final static public boolean is_move_to(unsigned c) {
    return c == path_cmd_move_to;
  }


  final static public boolean is_line_to(unsigned c) {
    return c == path_cmd_line_to;
  }


  final static public boolean is_curve(unsigned c) {
    return c == path_cmd_curve3 || c == path_cmd_curve4;
  }


  final static public boolean is_curve3(unsigned c) {
    return c == path_cmd_curve3;
  }


  final static public boolean is_curve4(unsigned c) {
    return c == path_cmd_curve4;
  }


  final static public boolean is_end_poly(unsigned c) {
    return (c & path_cmd_mask) == path_cmd_end_poly;
  }


  final static public boolean is_close(int c) {  // un
    return (c & ~(path_flags_cw | path_flags_ccw)) ==
      (path_cmd_end_poly | path_flags_close);
  }


  final static public boolean is_next_poly(int c) {  // un
    return is_stop(c) || is_move_to(c) || is_end_poly(c);
  }


  final static public boolean is_cw(int c) {  // un
    return (c & path_flags_cw) != 0;
  }


  final static public boolean is_ccw(int c) {  // un
    return (c & path_flags_ccw) != 0;
  }


  final static public boolean is_oriented(int c) {  // un
    return (c & (path_flags_cw | path_flags_ccw)) != 0;
  }


  final static public boolean is_closed(int c) {  // un
    return (c & path_flags_close) != 0;
  }


  final static public int get_close_flag(int c) {  // un
    return c & path_flags_close;
  }


  final static public int clear_orientation(int c) {  // un
    return c & ~(path_flags_cw | path_flags_ccw);
  }


  final static public int get_orientation(int c) {  // un
    return c & (path_flags_cw | path_flags_ccw);
  }


  final static public int set_orientation(int c, int o) {  // un
    return clear_orientation(c) | o;
  }
}

