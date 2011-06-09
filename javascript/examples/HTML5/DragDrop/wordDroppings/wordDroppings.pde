/**
 *    Drag one of the links below into the sketch window!      <br/>
 *    (Opera currently not supported, sorry!)
 */
 
color normalColor;
color dragColor;
color currentColor;

int nx = -10, ny = -10;
int cx = -10, cy = -10;

ArrayList trail;

String message;

void setup ()
{
    size( 300, 200 );
    
    normalColor = color(100);
    dragColor = color(200);
    currentColor = normalColor;
    
    trail = new ArrayList();
}

void draw ()
{
    background(100);
    
    noFill();
    stroke(currentColor);
    strokeWeight(5);
    rect(2.5,2.5,width-5,height-5);
    
    strokeWeight(1);
    if ( trail.size() > 2 )
    {
        for ( int i = 1; i < trail.size(); i++ )
        {
            stroke(map(i,1,trail.size(),100,0));
            line( trail.get(i-1)[0], trail.get(i-1)[1], 
                  trail.get(i)[0],   trail.get(i)[1] );
        }
        trail.remove(trail.get(0));
    }
    
    noStroke();
    fill(255);
    cx += (nx-cx) / 10.0;
    cy += (ny-cy) / 10.0;
    ellipse( cx, cy, 7, 7 );
    text( message, cx+7.5, cy+2.5 );
}

void dragEnter ()
{
    currentColor = dragColor;
    trail = new ArrayList();
}

void dragOver ( int dragX, int dragY )
{
    trail.add(new int[]{dragX,dragY});
}

void dragLeave ()
{
    currentColor = normalColor;
}

void dragDrop ( String dropMessage, int dropX, int dropY )
{
    message = dropMessage;
    nx = dropX;
    ny = dropY;
    
    currentColor = normalColor;
}
