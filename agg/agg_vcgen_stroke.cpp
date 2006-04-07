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
// Stroke generator
//
//----------------------------------------------------------------------------
#include <math.h>
#include "agg_vcgen_stroke.h"
#include "agg_shorten_path.h"

namespace agg
{

    //------------------------------------------------------------------------
    vcgen_stroke::vcgen_stroke() :
        m_src_vertices(),
        m_out_vertices(),
        m_width(0.5),
        m_miter_limit(4.0),
        m_inner_miter_limit(1.01),
        m_approx_scale(1.0),
        m_shorten(0.0),
        m_line_cap(butt_cap),
        m_line_join(miter_join),
        m_inner_join(inner_miter),
        m_closed(0),
        m_status(initial),
        m_src_vertex(0),
        m_out_vertex(0)
    {
    }


    //------------------------------------------------------------------------
    void vcgen_stroke::miter_limit_theta(double t)
    { 
        m_miter_limit = 1.0 / sin(t * 0.5) ;
    }


    //------------------------------------------------------------------------
    void vcgen_stroke::remove_all()
    {
        m_src_vertices.remove_all();
        m_closed = 0;
        m_status = initial;
    }


    //------------------------------------------------------------------------
    void vcgen_stroke::add_vertex(double x, double y, unsigned cmd)
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
                m_closed = get_close_flag(cmd);
            }
        }
    }


    //------------------------------------------------------------------------
    static inline void calc_butt_cap(double* cap,
                                     const vertex_dist& v0, 
                                     const vertex_dist& v1, 
                                     double len,
                                     double width)
    {
        double dx = (v1.y - v0.y) * width / len;
        double dy = (v1.x - v0.x) * width / len;
        cap[0] = v0.x - dx; 
        cap[1] = v0.y + dy;
        cap[2] = v0.x + dx;
        cap[3] = v0.y - dy;
    }

    //------------------------------------------------------------------------
    void vcgen_stroke::rewind(unsigned)
    {
        if(m_status == initial)
        {
            m_src_vertices.close(m_closed != 0);
            shorten_path(m_src_vertices, m_shorten, m_closed);
            if(m_src_vertices.size() < 3) m_closed = 0;
        }
        m_status = ready;
        m_src_vertex = 0;
        m_out_vertex = 0;
    }


    //------------------------------------------------------------------------
    unsigned vcgen_stroke::vertex(double* x, double* y)
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
                m_status = m_closed ? outline1 : cap1;
                cmd = path_cmd_move_to;
                m_src_vertex = 0;
                m_out_vertex = 0;
                break;

            case cap1:
                stroke_calc_cap(m_out_vertices,
                                m_src_vertices[0], 
                                m_src_vertices[1], 
                                m_src_vertices[0].dist,
                                m_line_cap,
                                m_width,
                                m_approx_scale);
                m_src_vertex = 1;
                m_prev_status = outline1;
                m_status = out_vertices;
                m_out_vertex = 0;
                break;

            case cap2:
                stroke_calc_cap(m_out_vertices,
                                m_src_vertices[m_src_vertices.size() - 1], 
                                m_src_vertices[m_src_vertices.size() - 2], 
                                m_src_vertices[m_src_vertices.size() - 2].dist,
                                m_line_cap,
                                m_width,
                                m_approx_scale);
                m_prev_status = outline2;
                m_status = out_vertices;
                m_out_vertex = 0;
                break;

            case outline1:
                if(m_closed)
                {
                    if(m_src_vertex >= m_src_vertices.size())
                    {
                        m_prev_status = close_first;
                        m_status = end_poly1;
                        break;
                    }
                }
                else
                {
                    if(m_src_vertex >= m_src_vertices.size() - 1)
                    {
                        m_status = cap2;
                        break;
                    }
                }
                stroke_calc_join(m_out_vertices, 
                                 m_src_vertices.prev(m_src_vertex), 
                                 m_src_vertices.curr(m_src_vertex), 
                                 m_src_vertices.next(m_src_vertex), 
                                 m_src_vertices.prev(m_src_vertex).dist,
                                 m_src_vertices.curr(m_src_vertex).dist,
                                 m_width, 
                                 m_line_join,
                                 m_inner_join,
                                 m_miter_limit,
                                 m_inner_miter_limit,
                                 m_approx_scale);
                ++m_src_vertex;
                m_prev_status = m_status;
                m_status = out_vertices;
                m_out_vertex = 0;
                break;

            case close_first:
                m_status = outline2;
                cmd = path_cmd_move_to;

            case outline2:
                if(m_src_vertex <= unsigned(m_closed == 0))
                {
                    m_status = end_poly2;
                    m_prev_status = stop;
                    break;
                }

                --m_src_vertex;
                stroke_calc_join(m_out_vertices,
                                 m_src_vertices.next(m_src_vertex), 
                                 m_src_vertices.curr(m_src_vertex), 
                                 m_src_vertices.prev(m_src_vertex), 
                                 m_src_vertices.curr(m_src_vertex).dist, 
                                 m_src_vertices.prev(m_src_vertex).dist,
                                 m_width, 
                                 m_line_join,
                                 m_inner_join,
                                 m_miter_limit,
                                 m_inner_miter_limit,
                                 m_approx_scale);

                m_prev_status = m_status;
                m_status = out_vertices;
                m_out_vertex = 0;
                break;

            case out_vertices:
                if(m_out_vertex >= m_out_vertices.size())
                {
                    m_status = m_prev_status;
                }
                else
                {
                    const point_type& c = m_out_vertices[m_out_vertex++];
                    *x = c.x;
                    *y = c.y;
                    return cmd;
                }
                break;

            case end_poly1:
                m_status = m_prev_status;
                return path_cmd_end_poly | path_flags_close | path_flags_ccw;

            case end_poly2:
                m_status = m_prev_status;
                return path_cmd_end_poly | path_flags_close | path_flags_cw;

            case stop:
                cmd = path_cmd_stop;
                break;
            }
        }
        return cmd;
    }

}
