import processing.core.*;

package agg;


public class agg_arrowhead {
  float m_head_d1;
  float m_head_d2;
  float m_head_d3;
  float m_head_d4;
  float m_tail_d1;
  float m_tail_d2;
  float m_tail_d3;
  float m_tail_d4;
  boolean m_head_flag;
  boolean m_tail_flag;
  float m_coord[] = new float[16];
  int m_cmd[] = new int[8];  // un
  int m_curr_id;  // un
  int m_curr_coord;  // un


  public agg_arrowhead() {
    m_head_d1 = 1.0f;
    m_head_d2 = 1.0f;
    m_head_d3 = 1.0f;
    m_head_d4 = 0.0f;
    m_tail_d1 = 1.0f;
    m_tail_d2 = 1.0f;
    m_tail_d3 = 1.0f;
    m_tail_d4 = 0.0f;
    m_head_flag = false;
    m_tail_flag = false;
    m_curr_id = 0f;
    m_curr_coord = 0;
  }


  public void head(float d1, float d2, float d3, float d4) {
    m_head_d1 = d1;
    m_head_d2 = d2;
    m_head_d3 = d3;
    m_head_d4 = d4;
    m_head_flag = true;
  }


  public void head() {
    m_head_flag = true;
  }


  public void no_head() {
    m_head_flag = false;
  }


  public void tail(float d1, float d2, float d3, float d4){
    m_tail_d1 = d1;
    m_tail_d2 = d2;
    m_tail_d3 = d3;
    m_tail_d4 = d4;
    m_tail_flag = true;
  }


  public void tail() {
    m_tail_flag = true;
  }


  public void no_tail() {
    m_tail_flag = false;
  }


  public void arrowhead::rewind(in path_id) {  // un
    m_curr_id = path_id;
    m_curr_coord = 0;

    if (path_id == 0) {
      if (!m_tail_flag) {
        m_cmd[0] = path_cmd_stop;
        return;
      }
      m_coord[0]  =  m_tail_d1;             m_coord[1]  =  0.0;
      m_coord[2]  =  m_tail_d1 - m_tail_d4; m_coord[3]  =  m_tail_d3;
      m_coord[4]  = -m_tail_d2 - m_tail_d4; m_coord[5]  =  m_tail_d3;
      m_coord[6]  = -m_tail_d2;             m_coord[7]  =  0.0;
      m_coord[8]  = -m_tail_d2 - m_tail_d4; m_coord[9]  = -m_tail_d3;
      m_coord[10] =  m_tail_d1 - m_tail_d4; m_coord[11] = -m_tail_d3;

      m_cmd[0] = path_cmd_move_to;
      m_cmd[1] = path_cmd_line_to;
      m_cmd[2] = path_cmd_line_to;
      m_cmd[3] = path_cmd_line_to;
      m_cmd[4] = path_cmd_line_to;
      m_cmd[5] = path_cmd_line_to;
      m_cmd[7] = path_cmd_end_poly | path_flags_close | path_flags_ccw;
      m_cmd[6] = path_cmd_stop;
      return;
    }

    if (path_id == 1) {
      if (!m_head_flag) {
        m_cmd[0] = path_cmd_stop;
        return;
      }
      m_coord[0]  = -m_head_d1;            m_coord[1]  = 0.0;
      m_coord[2]  = m_head_d2 + m_head_d4; m_coord[3]  = -m_head_d3;
      m_coord[4]  = m_head_d2;             m_coord[5]  = 0.0;
      m_coord[6]  = m_head_d2 + m_head_d4; m_coord[7]  = m_head_d3;

      m_cmd[0] = path_cmd_move_to;
      m_cmd[1] = path_cmd_line_to;
      m_cmd[2] = path_cmd_line_to;
      m_cmd[3] = path_cmd_line_to;
      m_cmd[4] = path_cmd_end_poly | path_flags_close | path_flags_ccw;
      m_cmd[5] = path_cmd_stop;
      return;
    }
  }


  public int vertex(float x[], float y[], int offset) {  // un
    if (m_curr_id < 2) {
      int curr_idx = m_curr_coord * 2;  // un
      x[offset] = m_coord[curr_idx];
      y[offset] = m_coord[curr_idx + 1];
      return m_cmd[m_curr_coord++];
    }
    return path_cmd_stop;
  }
}
