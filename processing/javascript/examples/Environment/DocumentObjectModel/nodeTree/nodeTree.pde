/**
 *    Renders a simple graph from this documents DOM node tree.
 */
 
 Node tree;
 float tx, ty;
 float growth = 0.1;
 
 void setup ()
 {
     size( 500, 500 );
     
     tx = width/2;
     ty = height-20;
 }
 
 void draw ()
 {
     background(255);
     
     if ( tree != null )
     {
         drawNode( tree, tx, ty, -HALF_PI, 200 );
         
         growth += 0.005;
         growth = constrain(growth, 0, 1);
     }
     
     textFont(createFont("Arial",10));
 }
 
 void drawNode ( Node n, float x, float y, float rad, int level )
 {
     fill(0);
     noStroke();
     ellipse(x,y,5,5);
     
     drawNodeLabel( n.nodeName, x+5, y );
     
     level *= 0.75;
     
     if ( n.childNodes != null && n.childNodes.length > 0 )
     {
         float l = n.childNodes.length;
         float d = 90.0*growth/l;
         float d2 = ((l-1)/2)*d;
         
         for ( int i = 0; i < l; i++ )
         {
             float r = radians(d * i - d2) + rad;
             float nx = x + cos(r)*level*growth;
             float ny = y + sin(r)*level*growth;
             
             stroke(0);
             line(x,y,nx,ny);
             
             drawNode( n.childNodes[i], nx, ny, r, level );
         }
     }
 }
 
 void  drawNodeLabel ( String label, float x, float y )
 {
     float tw = textWidth(label);
     
     fill(200);
     ellipse( x+10, y, 14, 14 );
     ellipse( x+10+tw, y, 14, 14 );
     rect( x+10, y-7, tw-2, 14 );
     
     fill(0);
     text( label, x+10, y+3 );
 }
 
 // called by JavaScript, sends the root DOM Node
 void setTree ( Node root )
 {
     tree = root;
 }
 
 
 // explain Node to Processing, the attributes described here are part of the DOM Node:
 // http://www.w3.org/TR/DOM-Level-2-Core/core.html#ID-1950641247
 interface Node
 {
     Node[] childNodes;
     String nodeName;
     String nodeType;
     Node parentNode;
 }
