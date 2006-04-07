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

#include <math.h>
#include "agg_vcgen_contour.h"

namespace agg
{

    //------------------------------------------------------------------------
    vcgen_contour::vcgen_contour() :
        m_src_vertices(),
        m_out_vertices(),
        m_width(1.0),
        m_line_join(bevel_join),
        m_inner_join(inner_miter),
        m_approx_scale(1.0),
        m_abs_width(1.0),
        m_signed_width(1.0),
        m_miter_limit(4.0),
        m_inner_miter_limit(1.0 + 1.0/64.0),
        m_status(initial),
        m_src_vertex(0),
        m_closed(0),
        m_orientation(0),
        m_auto_detect(false)
    {
    }


    //------------------------------------------------------------------------
    void vcgen_contour::remove_all()
    {
        m_src_vertices.remove_all();
        m_closed = 0;
        m_orientation = 0;
        m_abs_width = fabs(m_width);
        m_signed_width = m_width;
        m_status = initial;
    }


    //------------------------------------------------------------------------
    void vcgen_contour::miter_limit_theta(double t)
    { 
        m_miter_limit = 1.0 / sin(t * 0.5) ;
    }


    //------------------------------------------------------------------------
    void vcgen_contour::add_vertex(double x, double y, unsigned cmd)
    {
        m_status = initial;
        if(is_move_to(cmd))
        {
            m_src_vertices.modify_last(vertex_dist(x, y));
        }
        else
        {
            if(is_vertex(cmd))
            {
                m_src_vertices.add(vertex_dist(x, y));
            }
            else
            {
                if(is_end_poly(cmd))
                {
                    m_closed = get_close_flag(cmd);
                    if(m_orientation == path_flags_none) 
                    {
                        m_orientation = get_orientation(cmd);
                    }
                }
            }
        }
    }


    //------------------------------------------------------------------------
    void vcgen_contour::rewind(unsigned)
    {
        if(m_status == initial)
        {
            m_src_vertices.close(true);
            m_signed_width = m_width;
            if(m_auto_detect)
            {
                if(!is_oriented(m_orientation))
                {
                    m_orientation = (calc_polygon_area(m_src_vertices) > 0.0) ? 
                                    path_flags_ccw : 
                                    path_flags_cw;
                }
            }
            if(is_oriented(m_orientation))
            {
                m_signed_width = is_ccw(m_orientation) ? m_width : -m_width;
            }
        }
        m_status = ready;
        m_src_vertex = 0;
    }


    //------------------------------------------------------------------------
    unsigned vcgen_contour::vertex(double* x, double* y)
    {
        unsigned cmd = path_cmd_line_to;
        while(!is_stop(cmd))
        {
            switch(m_status)
            {
            case initial:
                rewind(0);

            case ready:
                if(m_src_vertices.size() < 2 + unsigned(m_closed != 0))
                {
                    cmd = path_cmd_stop;
                    break;
                }
                m_status = outline;
                cmd = path_cmd_move_to;
                m_src_vertex = 0;
                m_out_vertex = 0;

            case outline:
                if(m_src_vertex >= m_src_vertices.size())
                {
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
                if(m_out_vertex >= m_out_vertices.size())
                {
                    m_status = outline;
                }
                else
                {
                    const point_type& c = m_out_vertices[m_out_vertex++];
                    *x = c.x;
                    *y = c.y;
                    return cmd;
                }
                break;

            case end_poly:
                if(!m_closed) return path_cmd_stop;
                m_status = stop;
                return path_cmd_end_poly | path_flags_close | path_flags_ccw;

            case stop:
                return path_cmd_stop;
            }
        }
        return cmd;
    }

}
