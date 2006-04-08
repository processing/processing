import processing.core.*;

package agg;


public class agg_arc {

  float m_x;
  float m_y;
  float m_rx;
  float m_ry;
  float m_angle;
  float m_start;
  float m_end;
  float m_scale;
  float m_da;
  boolean m_ccw;
  boolean m_initialized;
  int m_path_cmd;  // un


  public agg_arc() {
    m_scale = 1.0;
    m_initialized = false;
  }


  /*
  public arc(float x,  float y,
             float rx, float ry,
             float a1, float a2) {
    init(x, y, rx, ry, a1, a2, true);
  }
  */


  public agg_arc(float x,  float y,
                 float rx, float ry,
                 float a1, float a2,
                 boolean ccw) {
    init(x, y, rx, ry, a1, a2, ccw);
  }


  /*
  public void init(float x,  float y,
                   float rx, float ry,
                   float a1, float a2) {
    init(x, y, rx, ry, a1, a2, true);
  }
  */


  public void init(float x,  float y,
                   float rx, float ry,
                   float a1, float a2,
                   boolean ccw) {
    m_x = x;
    m_y = y;
    m_rx = rx;
    m_ry = ry;
    m_scale = 1.0f;
    normalize(a1, a2, ccw);
  }


  public void approximation_scale(float s) {
    m_scale = s;
    if (m_initialized) {
      normalize(m_start, m_end, m_ccw);
    }
  }


  public void rewind() {
    m_path_cmd = path_cmd_move_to;
    m_angle = m_start;
  }


  int vertex(float x[], float y[], int offset) {  // un
    if (is_stop(m_path_cmd)) return path_cmd_stop;

    if ((m_angle < m_end - m_da/4) != m_ccw) {
      x[offset] = m_x + cos(m_end) * m_rx;
      y[offset] = m_y + sin(m_end) * m_ry;
      m_path_cmd = path_cmd_stop;
      return path_cmd_line_to;
    }

    x[offset] = m_x + (float)Math.cos(m_angle) * m_rx;
    y[offset] = m_y + (float)Math.sin(m_angle) * m_ry;

    m_angle += m_da;

    int pf = m_path_cmd;  // un
    m_path_cmd = path_cmd_line_to;
    return pf;
  }


  protected void normalize(float a1, float a2, boolean ccw) {
    float ra = (Math.abs(m_rx) + Math.abs(m_ry)) / 2;
    m_da = (float) Math.acos(ra / (ra + 0.125f / m_scale)) * 2;
    if (ccw) {
      while (a2 < a1) a2 += PConstants.PI * 2.0f;
    } else {
      while (a1 < a2) a1 += PConstants.PI * 2.0f;
      m_da = -m_da;
    }
    m_ccw   = ccw;
    m_start = a1;
    m_end   = a2;
    m_initialized = true;
  }
}
