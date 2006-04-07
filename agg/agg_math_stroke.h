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
// Stroke math
//
//----------------------------------------------------------------------------

#ifndef AGG_STROKE_MATH_INCLUDED
#define AGG_STROKE_MATH_INCLUDED

#include "agg_math.h"
#include "agg_vertex_sequence.h"

namespace agg
{
    //-------------------------------------------------------------line_cap_e
    enum line_cap_e
    {
        butt_cap,
        square_cap,
        round_cap
    };

    //------------------------------------------------------------line_join_e
    enum line_join_e
    {
        miter_join         = 0,
        miter_join_revert  = 1,
        miter_join_round   = 4,
        round_join         = 2,
        bevel_join         = 3
    };

    //-----------------------------------------------------------inner_join_e
    enum inner_join_e
    {
        inner_bevel,
        inner_miter,
        inner_jag,
        inner_round
    };

    // Minimal angle to calculate round joins, less than 0.1 degree.
    const double stroke_theta = 0.001;       //----stroke_theta

    //--------------------------------------------------------stroke_calc_arc
    template<class VertexConsumer>
    void stroke_calc_arc(VertexConsumer& out_vertices,
                         double x,   double y, 
                         double dx1, double dy1, 
                         double dx2, double dy2,
                         double width,
                         double approximation_scale)
    {
        typedef typename VertexConsumer::value_type coord_type;

        double a1 = atan2(dy1, dx1);
        double a2 = atan2(dy2, dx2);
        double da = a1 - a2;

        //  Possible optimization. Not important at all; consumes time but happens rarely
        //if(fabs(da) < stroke_theta)
        //{
        //    out_vertices.add(coord_type((x + x + dx1 + dx2) * 0.5, 
        //                                (y + y + dy1 + dy2) * 0.5));
        //    return;
        //}

        bool ccw = da > 0.0 && da < pi;

        if(width < 0) width = -width;
        da = acos(width / (width + 0.125 / approximation_scale)) * 2;

        out_vertices.add(coord_type(x + dx1, y + dy1));
        if(!ccw)
        {
            if(a1 > a2) a2 += 2 * pi;
            a2 -= da / 4;
            a1 += da;
            while(a1 < a2)
            {
                out_vertices.add(coord_type(x + cos(a1) * width, y + sin(a1) * width));
                a1 += da;
            }
        }
        else
        {
            if(a1 < a2) a2 -= 2 * pi;
            a2 += da / 4;
            a1 -= da;
            while(a1 > a2)
            {
                out_vertices.add(coord_type(x + cos(a1) * width, y + sin(a1) * width));
                a1 -= da;
            }
        }
        out_vertices.add(coord_type(x + dx2, y + dy2));
    }



    //-------------------------------------------------------stroke_calc_miter
    template<class VertexConsumer>
    void stroke_calc_miter(VertexConsumer& out_vertices,
                           const vertex_dist& v0, 
                           const vertex_dist& v1, 
                           const vertex_dist& v2,
                           double dx1, double dy1, 
                           double dx2, double dy2,
                           double width,
                           line_join_e line_join,
                           double miter_limit,
                           double approximation_scale)
    {
        typedef typename VertexConsumer::value_type coord_type;

        double xi = v1.x;
        double yi = v1.y;
        bool miter_limit_exceeded = true; // Assume the worst

        if(calc_intersection(v0.x + dx1, v0.y - dy1,
                             v1.x + dx1, v1.y - dy1,
                             v1.x + dx2, v1.y - dy2,
                             v2.x + dx2, v2.y - dy2,
                             &xi, &yi))
        {
            // Calculation of the intersection succeeded
            //---------------------
            double d1 = calc_distance(v1.x, v1.y, xi, yi);
            double lim = width * miter_limit;
            if(d1 <= lim)
            {
                // Inside the miter limit
                //---------------------
                out_vertices.add(coord_type(xi, yi));
                miter_limit_exceeded = false;
            }
        }
        else
        {
            // Calculation of the intersection failed, most probably
            // the three points lie one straight line. 
            // First check if v0 and v2 lie on the opposite sides of vector: 
            // (v1.x, v1.y) -> (v1.x+dx1, v1.y-dy1), that is, the perpendicular
            // to the line determined by vertices v0 and v1.
            // This condition determines whether the next line segments continues
            // the previous one or goes back.
            //----------------
            double x2 = v1.x + dx1;
            double y2 = v1.y - dy1;
            if(((x2 - v0.x)*dy1 - (v0.y - y2)*dx1 < 0.0) !=
               ((x2 - v2.x)*dy1 - (v2.y - y2)*dx1 < 0.0))
            {
                // This case means that the next segment continues 
                // the previous one (straight line)
                //-----------------
                out_vertices.add(coord_type(v1.x + dx1, v1.y - dy1));
                miter_limit_exceeded = false;
            }
        }

        if(miter_limit_exceeded)
        {
            // Miter limit exceeded
            //------------------------
            switch(line_join)
            {
            case miter_join_revert:
                // For the compatibility with SVG, PDF, etc, 
                // we use a simple bevel join instead of
                // "smart" bevel
                //-------------------
                out_vertices.add(coord_type(v1.x + dx1, v1.y - dy1));
                out_vertices.add(coord_type(v1.x + dx2, v1.y - dy2));
                break;

            case miter_join_round:
                stroke_calc_arc(out_vertices, 
                                v1.x, v1.y, dx1, -dy1, dx2, -dy2, 
                                width, approximation_scale);
                break;

            default:
                // If no miter-revert, calculate new dx1, dy1, dx2, dy2
                //----------------
                out_vertices.add(coord_type(v1.x + dx1 + dy1 * miter_limit, 
                                            v1.y - dy1 + dx1 * miter_limit));
                out_vertices.add(coord_type(v1.x + dx2 - dy2 * miter_limit, 
                                            v1.y - dy2 - dx2 * miter_limit));
                break;
            }
        }
    }






    //--------------------------------------------------------stroke_calc_cap
    template<class VertexConsumer>
    void stroke_calc_cap(VertexConsumer& out_vertices,
                         const vertex_dist& v0, 
                         const vertex_dist& v1, 
                         double len,
                         line_cap_e line_cap,
                         double width,
                         double approximation_scale)
    {
        typedef typename VertexConsumer::value_type coord_type;

        out_vertices.remove_all();

        double dx1 = (v1.y - v0.y) / len;
        double dy1 = (v1.x - v0.x) / len;
        double dx2 = 0;
        double dy2 = 0;

        dx1 *= width;
        dy1 *= width;

        if(line_cap != round_cap)
        {
            if(line_cap == square_cap)
            {
                dx2 = dy1;
                dy2 = dx1;
            }
            out_vertices.add(coord_type(v0.x - dx1 - dx2, v0.y + dy1 - dy2));
            out_vertices.add(coord_type(v0.x + dx1 - dx2, v0.y - dy1 - dy2));
        }
        else
        {
            double a1 = atan2(dy1, -dx1);
            double a2 = a1 + pi;
            double da = acos(width / (width + 0.125 / approximation_scale)) * 2;
            out_vertices.add(coord_type(v0.x - dx1, v0.y + dy1));
            a1 += da;
            a2 -= da/4;
            while(a1 < a2)
            {
                out_vertices.add(coord_type(v0.x + cos(a1) * width, 
                                            v0.y + sin(a1) * width));
                a1 += da;
            }
            out_vertices.add(coord_type(v0.x + dx1, v0.y - dy1));
        }
    }



    //-------------------------------------------------------stroke_calc_join
    template<class VertexConsumer>
    void stroke_calc_join(VertexConsumer& out_vertices,
                          const vertex_dist& v0, 
                          const vertex_dist& v1, 
                          const vertex_dist& v2,
                          double len1, 
                          double len2,
                          double width, 
                          line_join_e line_join,
                          inner_join_e inner_join,
                          double miter_limit,
                          double inner_miter_limit,
                          double approximation_scale)
    {
        typedef typename VertexConsumer::value_type coord_type;

        double dx1, dy1, dx2, dy2;

        dx1 = width * (v1.y - v0.y) / len1;
        dy1 = width * (v1.x - v0.x) / len1;

        dx2 = width * (v2.y - v1.y) / len2;
        dy2 = width * (v2.x - v1.x) / len2;

        out_vertices.remove_all();

        if(calc_point_location(v0.x, v0.y, v1.x, v1.y, v2.x, v2.y) > 0)
        {
            // Inner join
            //---------------
            switch(inner_join)
            {
            default: // inner_bevel
                out_vertices.add(coord_type(v1.x + dx1, v1.y - dy1));
                out_vertices.add(coord_type(v1.x + dx2, v1.y - dy2));
                break;

            case inner_miter:
                stroke_calc_miter(out_vertices,
                                  v0, v1, v2, dx1, dy1, dx2, dy2, 
                                  width,                                   
                                  miter_join_revert, 
                                  inner_miter_limit,
                                  1.0);
                break;

            case inner_jag:
            case inner_round:
                {
                    double d = (dx1-dx2) * (dx1-dx2) + (dy1-dy2) * (dy1-dy2);
                    if(d < len1 * len1 && d < len2 * len2)
                    {
                        stroke_calc_miter(out_vertices,
                                          v0, v1, v2, dx1, dy1, dx2, dy2, 
                                          width,                                   
                                          miter_join_revert, 
                                          inner_miter_limit,
                                          1.0);
                    }
                    else
                    {
                        if(inner_join == inner_jag)
                        {
                            out_vertices.add(coord_type(v1.x + dx1, v1.y - dy1));
                            out_vertices.add(coord_type(v1.x,       v1.y      ));
                            out_vertices.add(coord_type(v1.x + dx2, v1.y - dy2));
                        }
                        else
                        {
                            out_vertices.add(coord_type(v1.x + dx1, v1.y - dy1));
                            out_vertices.add(coord_type(v1.x,       v1.y      ));
                            stroke_calc_arc(out_vertices, 
                                            v1.x, v1.y, dx2, -dy2, dx1, -dy1, 
                                            width, approximation_scale);
                            out_vertices.add(coord_type(v1.x,       v1.y      ));
                            out_vertices.add(coord_type(v1.x + dx2, v1.y - dy2));
                        }
                    }
                }
                break;
            }
        }
        else
        {
            // Outer join
            //---------------
            switch(line_join)
            {
            case miter_join:
            case miter_join_revert:
            case miter_join_round:
                stroke_calc_miter(out_vertices, 
                                  v0, v1, v2, dx1, dy1, dx2, dy2, 
                                  width,                                   
                                  line_join, 
                                  miter_limit,
                                  approximation_scale);
                break;

            case round_join:
                stroke_calc_arc(out_vertices, 
                                v1.x, v1.y, dx1, -dy1, dx2, -dy2, 
                                width, approximation_scale);
                break;

            default: // Bevel join
                out_vertices.add(coord_type(v1.x + dx1, v1.y - dy1));
                out_vertices.add(coord_type(v1.x + dx2, v1.y - dy2));
                break;
            }
        }
    }




}

#endif
