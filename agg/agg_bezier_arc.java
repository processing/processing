import processing.core.*;


public class agg_bezier_arc {

  private int m_vertex;  // un
  private m_num_vertices; // un
  private float m_vertices[] = new float[26];
  private int m_cmd;  // un

  /**
   * This epsilon is used to prevent us from adding degenerate curves
   * (converging to a single point).
   * The value isn't very critical. Function arc_to_bezier() has a limit
   * of the sweep_angle. If fabs(sweep_angle) exceeds pi/2 the curve
   * becomes inaccurate. But slight exceeding is quite appropriate.
   */
  static final float bezier_arc_angle_epsilon = 0.01f;


  public agg_bezier_arc() {
    m_vertex = 26;
    m_num_vertices = 0;
    m_cmd = path_cmd_line_to;
  }


  public agg_bezier_arc(float x,  float y,
                    float rx, float ry,
                    float start_angle,
                    float sweep_angle) {
    init(x, y, rx, ry, start_angle, sweep_angle);
  }


  public void init(float x,  float y,
                   float rx, float ry,
                   float start_angle,
                   float sweep_angle) {
    start_angle = start_angle % PConstants.TWO_PI;
    if (sweep_angle >=  2.0 * pi) sweep_angle =  PConstants.TWO_PI;
    if (sweep_angle <= -2.0 * pi) sweep_angle = -PConstants.TWO_PI;

    if (Math.abs(sweep_angle) < 1e-10) {
      m_num_vertices = 4;
      m_cmd = path_cmd_line_to;
      m_vertices[0] = x + rx * (float)Math.cos(start_angle);
      m_vertices[1] = y + ry * (float)Math.sin(start_angle);
      m_vertices[2] = x + rx * (float)Math.cos(start_angle + sweep_angle);
      m_vertices[3] = y + ry * (float)Math.sin(start_angle + sweep_angle);
      return;
    }

    float total_sweep = 0.0f;
    float local_sweep = 0.0f;
    float prev_sweep;
    m_num_vertices = 2;
    m_cmd = path_cmd_curve4;
    bool done = false;
    do {
      if (sweep_angle < 0.0f) {
        prev_sweep  = total_sweep;
        local_sweep = -pi * 0.5f;
        total_sweep -= pi * 0.5f;
        if (total_sweep <= sweep_angle + bezier_arc_angle_epsilon) {
          local_sweep = sweep_angle - prev_sweep;
          done = true;
        }
      } else {
        prev_sweep  = total_sweep;
        local_sweep =  pi * 0.5f;
        total_sweep += pi * 0.5f;
        if (total_sweep >= sweep_angle - bezier_arc_angle_epsilon) {
          local_sweep = sweep_angle - prev_sweep;
          done = true;
        }
      }

      arc_to_bezier(x, y, rx, ry,
                    start_angle,
                    local_sweep,
                    m_vertices + m_num_vertices - 2);

      m_num_vertices += 6;
      start_angle += local_sweep;
    }
    while (!done && m_num_vertices < 26);
  }


  public void rewind() {
    m_vertex = 0;
  }


  public int vertex(float x[], float y[], int offset) {  // un
    if (m_vertex >= m_num_vertices) return path_cmd_stop;
    x[offset] = m_vertices[m_vertex];
    y[offset] = m_vertices[m_vertex + 1];
    m_vertex += 2;
    return (m_vertex == 2) ? path_cmd_move_to : m_cmd;
  }


  /**
   * Supplemantary functions. num_vertices() actually returns doubled
   * number of vertices. That is, for 1 vertex it returns 2.
   */
  public int num_vertices() {
    return m_num_vertices;
  }


  public void float[] vertices() {
    return m_vertices;
  }


  public float[] vertices() {
    return m_vertices;
  }


  static public void arc_to_bezier(float cx, float cy, float rx, float ry,
                                   float start_angle, float sweep_angle,
                                   float* curve) {
    float x0 = (float) Math.cos(sweep_angle / 2.0f);
    float y0 = (float) Math.sin(sweep_angle / 2.0f);
    float tx = (1.0f - x0) * 4.0f / 3.0f;
    float ty = y0 - tx * x0 / y0;
    float px[] = new float[4];
    float py[] = new float[4];
    px[0] =  x0;
    py[0] = -y0;
    px[1] =  x0 + tx;
    py[1] = -ty;
    px[2] =  x0 + tx;
    py[2] =  ty;
    px[3] =  x0;
    py[3] =  y0;

    float sn = (float) Math.sin(start_angle + sweep_angle / 2.0f);
    float cs = (float) Math.cos(start_angle + sweep_angle / 2.0f);

    for(int i = 0; i < 4; i++) {
      curve[i * 2]     = cx + rx * (px[i] * cs - py[i] * sn);
      curve[i * 2 + 1] = cy + ry * (px[i] * sn + py[i] * cs);
    }
  }
}


