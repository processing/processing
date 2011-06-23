void timeLineEffects ()
{
     float t = video.duration/10;
     float n = video.currentTime;
     
     fill( 255 );
     
     if ( n < t )
     {
         float w = map(n,0,t,100,500);
         ellipse( width/2,height/2,w,w);
     }
     else if ( n < 2*t )
     {
         float w = map(n,t,2*t,0,300);
         
         fill(255);
         ellipse( width/2-100,height/2-100,w*0.75,w*0.75);
         fill(0);
         ellipse( width/2+50,height/2,w,w);
         fill(255);
         ellipse( width/2-100,height/2+150,w*1.5,w*1.5);
     }
     else if ( n < 3*t )
     {
         float w = map(n,2*t,3*t,0,1);
         
         for ( int i = 0 ; i < 9; i++ )
         {
             rect((i%3)*width/3,int(i/3)*height/3,width/3*w,height/3*w);
         }
     }
     else if ( n < 4*t )
     {
         float w = map(n,3*t,4*t,-1,0);
         
         fill( 0 );
         for ( int i = 0 ; i < 25; i++ )
         {
             rect(((i%5)+1)*width/5,(int(i/5)+1)*height/5,width/5*w,height/5*w);
         }
     }
     else if ( n < 5*t )
     {
         float w = map(n,4*t,5*t,0,height/9);
         
         for ( int i = 0 ; i < 10; i++ )
         {
             rect(0,i*(height/9)-w, width, height/18);
         }
     }
     else if ( n < 7*t )
     {
         float w = map(n,6*t,7*t,0,TWO_PI);
         for ( int i = 0 ; i < 22; i++ )
         {
             fill( cos(w) > 0 ? 255 : 0 );
             rect(i*(width/20), 0, width/40, height);
         }
     }
     else if ( n < 8*t )
     {
         int mx = int(map(n,t*7,t*8,1,20));
         
         for ( int i = 0; i < mx; i++ )
             ellipse( random(width), random(height), 5*mx, 5*mx );
     }
     else if ( n < 9*t )
     {
         int mxx = 1+int(map(n,t*8,t*9,1,10))*2;
         mx = mxx*mxx;
         
         for ( int i = 0 ; i < mx; i+=2 )
         {
             rect( (i%mxx)*width/mxx, int(i/mxx)*height/mxx, width/mxx, height/mxx );
         }
     }
     else if ( n < 10*t )
     {
         float w = map(n,9*t,10*t,700,100);
         ellipse( width/2,height/2,w,w);
     }
}
