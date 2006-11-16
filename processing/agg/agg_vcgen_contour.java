//----------------------------------------------------------------------------
// Anti-Grain Geometry - Version 2.3
// Copyright (C) 2002-2005 Maxim Shemanarev (http://www.antigrain.com)
//
// Permission to copy, use, modify, sell and distribute this software
// is granted provided this copyright notice appears in all copies.
// This software is provided "as is" without express or implied
// warranty, and with no claim as to its suitability for any purpose.
//
//----------------------------------------------------------------------------
// Contact: mcseem@antigrain.com
//          mcseemagg@yahoo.com
//          http://www.antigrain.com
//----------------------------------------------------------------------------
//
// Contour generator
//
//----------------------------------------------------------------------------

package agg;


public class agg_vcgen_contour {

  // enum status_e  // arbitrary values
  static final int initial = 1;
  static final int ready = 2;
  static final int outline = 3;
  static final int out_vertices = 4;
  static final int end_poly = 5;
  static final int stop = 6;

  //typedef vertex_sequence<vertex_dist, 6> vertex_storage;
  //typedef pod_deque<point_type, 6>        coord_storage;

  public void line_join(line_join_e lj) {
    m_line_join = lj;
  }

  public void inner_join(inner_join_e ij) {
    m_inner_join = ij;
  }

  public void width(float w) {
    m_width = w * 0.5f;
  }

  public void miter_limit(float ml) {
    m_miter_limit = ml;
  }

  public void miter_limit_theta(float t) {
    m_miter_limit = 1.0f / (float)Math.sin(t * 0.5f) ;
  }

  public void inner_miter_limit(float ml) {
    m_inner_miter_limit = ml;
  }

  public void approximation_scale(float as) {
    m_approx_scale = as;
  }

  public void auto_detect_orientation(bool v) {
    m_auto_detect = v;
  }


  public int line_join() const { return m_line_join; }  // line_join_e
  public int inner_join() const { return m_inner_join; }  // inner_join_e
  public float width() const { return m_width * 2.0f; }
  public float miter_limit() const { return m_miter_limit; }
  public float inner_miter_limit() const { return m_inner_miter_limit; }
  public float approximation_scale() const { return m_approx_scale; }
  public boolean auto_detect_orientation() const { return m_auto_detect; }


  public void remove_all() {
    m_src_vertices.remove_all();
    m_closed = 0;
    m_orientation = 0;
    m_abs_width = fabs(m_width);
    m_signed_width = m_width;
    m_status = initial;
  }


  public void add_vertex(float x, float y, int cmd) {
    m_status = initial;
    if (agg_basics.is_move_to(cmd)) {
      m_src_vertices.modify_last(vertex_dist(x, y));
    } else {
      if (is_vertex(cmd)) {
        m_src_vertices.add(vertex_dist(x, y));
      } else {
        if (is_end_poly(cmd)) {
          m_closed = get_close_flag(cmd);
          if(m_orientation == path_flags_none) {
            m_orientation = get_orientation(cmd);
          }
        }
      }
    }
  }

  //vcgen_contour(const vcgen_contour&);
  //const vcgen_contour& operator = (const vcgen_contour&);

  private vertex_storage m_src_vertices;
  private coord_storage  m_out_vertices;
  private float m_width;
  private int m_line_join;  // line_join_e
  private int m_inner_join;  // inner_join_e
  private float m_approx_scale;
  private float m_abs_width;
  private float m_signed_width;
  private float m_miter_limit;
  private float m_inner_miter_limit;
  private int m_status;  // status_e
  private int m_src_vertex;  // un
  private int m_out_vertex;  // un
  private int m_closed;  // un
  private int m_orientation;  // un
  private boolean m_auto_detect;


  public vcgen_contour() {
    m_src_vertices = new vertex_storage();
    m_out_vertices = new coord_storage();
    m_width = 1.0f;
    m_line_join = bevel_join;
    m_inner_join = inner_miter;
    m_approx_scale = 1.0f;
    m_abs_width = 1.0f;
    m_signed_width = 1.0f;
    m_miter_limit = 4.0f;
    m_inner_miter_limit = 1.0f + 1.0f/64.0f;
    m_status = initial;
    m_src_vertex = 0;
    m_closed = 0;
    m_orientation = 0;
    m_auto_detect = false;
  }


  //void rewind(unsigned path_id);
  public void rewind() {
    if (m_status == initial) {
      m_src_vertices.close(true);
      m_signed_width = m_width;
      if (m_auto_detect) {
        if (!is_oriented(m_orientation)) {
          m_orientation = (calc_polygon_area(m_src_vertices) > 0.0f) ?
            path_flags_ccw :
            path_flags_cw;
        }
      }
      if(is_oriented(m_orientation)) {
        m_signed_width = is_ccw(m_orientation) ? m_width : -m_width;
      }
    }
    m_status = ready;
    m_src_vertex = 0;
  }


  public int vertex(float x[], float y[], int offset) {  // un
    int cmd = path_cmd_line_to;
    while (!is_stop(cmd)) {
      switch(m_status) {
      case initial:
        rewind(0);

      case ready:
        //if(m_src_vertices.size() < 2 + unsigned(m_closed != 0)) {
        if (m_src_vertices.size() < 2 + ((m_closed != 0) ? 1 : 0)) {
          cmd = path_cmd_stop;
          break;
        }
        m_status = outline;
        cmd = path_cmd_move_to;
        m_src_vertex = 0;
        m_out_vertex = 0;

      case outline:
        if(m_src_vertex >= m_src_vertices.size()) {
          m_status = end_poly;
          break;
        }
        stroke_calc_join(m_out_vertices,
                         m_src_vertices.prev(m_src_vertex),
                         m_src_vertices.curr(m_src_vertex),
                         m_src_vertices.next(m_src_vertex),
                         m_src_vertices.prev(m_src_vertex).dist,
                         m_src_vertices.curr(m_src_vertex).dist,
                         m_signed_width,
                         m_line_join,
                         m_inner_join,
                         m_miter_limit,
                         m_inner_miter_limit,
                         m_approx_scale);
        ++m_src_vertex;
        m_status = out_vertices;
        m_out_vertex = 0;

      case out_vertices:
        if(m_out_vertex >= m_out_vertices.size()) {
          m_status = outline;
        } else {
          const point_type& c = m_out_vertices[m_out_vertex++];
          x[offset] = c.x;
          y[offset] = c.y;
          return cmd;
        }
        break;

      case end_poly:
        if (!m_closed) return path_cmd_stop;
        m_status = stop;
        return path_cmd_end_poly | path_flags_close | path_flags_ccw;

      case stop:
        return path_cmd_stop;
      }
    }
    return cmd;
  }
}
