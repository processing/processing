
// add Prototype.js library
document.write("<script src=\"prototype-min.js\" type=\"text/javascript\"></script>");
// add Box2D.js library
document.write("<script src=\"box2d-min.js\" type=\"text/javascript\"></script>");

// event handler called once document has loaded ..
window.onload = function () {
    tryFindSketch();
}

// try to get the sketch instance from Processing.js
function tryFindSketch () {
    var sketch = Processing.getInstanceById(getProcessingSketchId());
    if ( sketch == undefined )
        return setTimeout( tryFindSketch, 200 ); // retry
    
    var inter = new Box2DJSInterface(sketch);
    sketch.setBox2DInterface(inter);
}

/**
 *  This is just a tiny simple wrapper to get you started ...
 *  ... or spawn an idea.
 *
 *  Based on Ando Yasushi example code.
 */
var Box2DJSInterface = (function() {
    function Box2DJSInterface () {
        if ( arguments.length <= 0 || typeof arguments[0] !== 'object' ) {
            alert('You need to pass a Processing instance in here!');
            return undefined;
        }

        var world = createWorld();
        createGround(world);
        createBox(world, null, -10, 125, 10, 250 );
        createBox(world, null, 510, 125, 10, 250 );
            
        var sketch = arguments[0];

        this.update = function () {
            world.Step( 1/60.0, 1.0 );
        }

        this.draw = function () {
            this.drawJoints();
            this.drawShapes();
        }

        this.drawJoints = function () {
            for (var j = world.m_jointList; j; j = j.m_next) {
                var b1 = j.m_body1;
                var b2 = j.m_body2;
                var x1 = b1.m_position;
                var x2 = b2.m_position;
                var p1 = j.GetAnchor1();
                var p2 = j.GetAnchor2();

                switch (j.m_type) {
                case b2Joint.e_distanceJoint:
                    sketch.drawJoint(p1.x, p1.y, p2.x, p2.y);
                    break;

                case b2Joint.e_pulleyJoint:
                    // TODO
                    break;

                default:
                    if (b1 == world.m_groundBody) {
                        sketch.drawJoint([p1.x, p1.y, x2.x, x2.y]);
                    }
                    else if (b2 == world.m_groundBody) {
                        sketch.drawJoint([p1.x, p1.y, x1.x, x1.y]);
                    }
                    else {
                        sketch.drawJoint([x1.x, x1.y, p1.x, p1.y, x2.x, x2.y, p2.x, p2.y]);
                    }
                    break;
                }
            }
        }

        this.drawShapes = function () {
            for (var b = world.m_bodyList; b; b = b.m_next) {
                for (var s = b.GetShapeList(); s != null; s = s.GetNext()) {
                    switch (s.m_type) {
                    case b2Shape.e_circleShape:
                            var pos = s.m_position;
                            var r = s.m_radius;
                            sketch.drawCircle(s.GetUserData(),pos.x,pos.y,r);
                        break;
                    case b2Shape.e_polyShape:
                            var tV = b2Math.AddVV( s.m_position, 
                                                   b2Math.b2MulMV( s.m_R, s.m_vertices[0] ) );
                            var points = [tV.x, tV.y];
                            for (var i = 0; i < s.m_vertexCount; i++) {
                                var v = b2Math.AddVV( s.m_position, b2Math.b2MulMV( s.m_R, s.m_vertices[i] ) );
                                points[points.length] = v.x;
                                points[points.length] = v.y;
                            }
                            points[points.length] = tV.x;
                            points[points.length] = tV.y;
                            sketch.drawPolygon(s.GetUserData(),points);
                        break;
                    }
                }
            }
        }
        
        this.createBall = function ( c, x, y, r ) {
            createBall( world, c, x, y, r );
        }
        
        this.createBox = function ( c, x, y, w, h ) {
            createBox( world, c, x, y, w, h, false );
        }
    }
    
    var createWorld = function () {
        var worldAABB = new b2AABB();
        worldAABB.minVertex.Set(-1000, -1000);
        worldAABB.maxVertex.Set(1000, 1000);
        var gravity = new b2Vec2(0, 300);
        var doSleep = true;
        var world = new b2World(worldAABB, gravity, doSleep);
        return world;
    }

    var createGround = function (world) {
        var groundSd = new b2BoxDef();
        groundSd.extents.Set(1000, 50);
        groundSd.restitution = 0.2;
        var groundBd = new b2BodyDef();
        groundBd.AddShape(groundSd);
        groundBd.position.Set(-500, 350);
        return world.CreateBody(groundBd);
    }

    var createBall = function (world, c, x, y, r) {
        var ballSd = new b2CircleDef();
        ballSd.density = 2.0;
        ballSd.radius = r;
        ballSd.restitution = 1.0;
        ballSd.friction = 2.0;
        ballSd.userData = c;
        var ballBd = new b2BodyDef();
        ballBd.AddShape(ballSd);
        ballBd.position.Set(x, y);
        return world.CreateBody(ballBd);
    }

    var createBox = function (world, c, x, y, width, height, fixed) {
        if (typeof(fixed) == 'undefined') fixed = true;
        var boxSd = new b2BoxDef();
        boxSd.userData = c;
        if (!fixed) boxSd.density = 1.0;
        boxSd.extents.Set(width, height);
        var boxBd = new b2BodyDef();
        boxBd.AddShape(boxSd);
        boxBd.position.Set(x, y);
        return world.CreateBody(boxBd);
    }
    
    return Box2DJSInterface;
})();

