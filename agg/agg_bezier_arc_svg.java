import processing.core.*;


/**
 * Compute an SVG-style bezier arc.
 *
 * Computes an elliptical arc from (x1, y1) to (x2, y2). The size and
    * orientation of the ellipse are defined by two radii (rx, ry)
    * and an x-axis-rotation, which indicates how the ellipse as a whole
    * is rotated relative to the current coordinate system. The center
    * (cx, cy) of the ellipse is calculated automatically to satisfy the
    * constraints imposed by the other parameters.
    * large-arc-flag and sweep-flag contribute to the automatic calculations
    * and help determine how the arc is drawn.
    */
public class agg_bezier_arc_svg {

  public agg_bezier_arc_svg() : m_arc(), m_radii_ok(false) {}

  bezier_arc_svg(float x1, float y1,
                 float rx, float ry,
                 float angle,
                 bool large_arc_flag,
                 bool sweep_flag,
                 float x2, float y2) {
    m_arc = new agg_bezier_arc();
    m_radii_ok = false;

    init(x1, y1, rx, ry, angle, large_arc_flag, sweep_flag, x2, y2);
  }

        //--------------------------------------------------------------------
        void init(float x1, float y1,
                  float rx, float ry,
                  float angle,
                  bool large_arc_flag,
                  bool sweep_flag,
                  float x2, float y2);


  public boolean radii_ok() {
    return m_radii_ok;
  }


  public void rewind() {
    m_arc.rewind(0);
  }


  //--------------------------------------------------------------------
  int vertex(float x[], float y[], int offset) {  // un
    return m_arc.vertex(x, y, offset);
  }

        // Supplemantary functions. num_vertices() actually returns doubled
        // number of vertices. That is, for 1 vertex it returns 2.
        //--------------------------------------------------------------------
        unsigned  num_vertices() const { return m_arc.num_vertices(); }
        const float* vertices() const { return m_arc.vertices();     }
        float*       vertices()       { return m_arc.vertices();     }

      private agg_bezier_arc m_arc;
  private boolean m_radii_ok;
    }


    //--------------------------------------------------------------------
    void bezier_arc_svg::init(float x0, float y0,
                              float rx, float ry,
                              float angle,
                              bool large_arc_flag,
                              bool sweep_flag,
                              float x2, float y2)
    {
        m_radii_ok = true;

        if(rx < 0.0) rx = -rx;
        if(ry < 0.0) ry = -rx;

        // Calculate the middle point between
        // the current and the final points
        //------------------------
        float dx2 = (x0 - x2) / 2.0;
        float dy2 = (y0 - y2) / 2.0;

        float cos_a = cos(angle);
        float sin_a = sin(angle);

        // Calculate (x1, y1)
        //------------------------
        float x1 =  cos_a * dx2 + sin_a * dy2;
        float y1 = -sin_a * dx2 + cos_a * dy2;

        // Ensure radii are large enough
        //------------------------
        float prx = rx * rx;
        float pry = ry * ry;
        float px1 = x1 * x1;
        float py1 = y1 * y1;

        // Check that radii are large enough
        //------------------------
        float radii_check = px1/prx + py1/pry;
        if(radii_check > 1.0)
        {
            rx = sqrt(radii_check) * rx;
            ry = sqrt(radii_check) * ry;
            prx = rx * rx;
            pry = ry * ry;
            if(radii_check > 10.0) m_radii_ok = false;
        }

        // Calculate (cx1, cy1)
        //------------------------
        float sign = (large_arc_flag == sweep_flag) ? -1.0 : 1.0;
        float sq   = (prx*pry - prx*py1 - pry*px1) / (prx*py1 + pry*px1);
        float coef = sign * sqrt((sq < 0) ? 0 : sq);
        float cx1  = coef *  ((rx * y1) / ry);
        float cy1  = coef * -((ry * x1) / rx);

        //
        // Calculate (cx, cy) from (cx1, cy1)
        //------------------------
        float sx2 = (x0 + x2) / 2.0;
        float sy2 = (y0 + y2) / 2.0;
        float cx = sx2 + (cos_a * cx1 - sin_a * cy1);
        float cy = sy2 + (sin_a * cx1 + cos_a * cy1);

        // Calculate the start_angle (angle1) and the sweep_angle (dangle)
        //------------------------
        float ux =  (x1 - cx1) / rx;
        float uy =  (y1 - cy1) / ry;
        float vx = (-x1 - cx1) / rx;
        float vy = (-y1 - cy1) / ry;
        float p, n;

        // Calculate the angle start
        //------------------------
        n = sqrt(ux*ux + uy*uy);
        p = ux; // (1 * ux) + (0 * uy)
        sign = (uy < 0) ? -1.0 : 1.0;
        float v = p / n;
        if(v < -1.0) v = -1.0;
        if(v >  1.0) v =  1.0;
        float start_angle = sign * acos(v);

        // Calculate the sweep angle
        //------------------------
        n = sqrt((ux*ux + uy*uy) * (vx*vx + vy*vy));
        p = ux * vx + uy * vy;
        sign = (ux * vy - uy * vx < 0) ? -1.0 : 1.0;
        v = p / n;
        if(v < -1.0) v = -1.0;
        if(v >  1.0) v =  1.0;
        float sweep_angle = sign * acos(v);
        if(!sweep_flag && sweep_angle > 0)
        {
            sweep_angle -= pi * 2.0;
        }
        else
        if (sweep_flag && sweep_angle < 0)
        {
            sweep_angle += pi * 2.0;
        }

        // We can now build and transform the resulting arc
        //------------------------
        m_arc.init(0.0, 0.0, rx, ry, start_angle, sweep_angle);
        trans_affine mtx = trans_affine_rotation(angle);
        mtx *= trans_affine_translation(cx, cy);

        for(unsigned i = 2; i < m_arc.num_vertices()-2; i += 2)
        {
            mtx.transform(m_arc.vertices() + i, m_arc.vertices() + i + 1);
        }

        // We must make sure that the starting and ending points
        // exactly coincide with the initial (x0,y0) and (x2,y2)
        m_arc.vertices()[0] = x0;
        m_arc.vertices()[1] = y0;
        if(m_arc.num_vertices() > 2)
        {
            m_arc.vertices()[m_arc.num_vertices() - 2] = x2;
            m_arc.vertices()[m_arc.num_vertices() - 1] = y2;
        }
    }


}
