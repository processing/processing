/**
 *  A very simplistic Twitter reader.<br />
 *
 *  <form>
 *    <input id="form-input" type="text" value="#processingjs" />
 *    <input id="form-submit" type="submit" value="search" />
 *  </form>
 */
 
 HashMap<Integer,Tweet> tweets;
 PImage img;
 
 int scrollY = 0;
 int totalTweetsHeight = 0;
 
 Tweet selectedTweet = null;
 
 void setup ()
 {
     size( 300, 200 );
     
     textFont(createFont("Helvetica", 10));
 }
 
 void draw ()
 {
     background( (tweets != null && tweets.size() > 0) ? 200 : 100 );
     
     if ( tweets != null && tweets.size() > 0 )
     {
         translate( 0,-scrollY );
     
         int y = 2;
         for ( Tweet t : tweets.values() )
         {
             fill( selectedTweet == t ? 0xDDDDFF : 220 );
             noStroke();
             rect( 2, y, width-4, 50 );
             
             if ( t.profile_image != null )
             {
                image( t.profile_image, 2, y ); 
             }
             else
             {
                 fill(100);
                 rect( 2, y, 50, 50 );
             }
             
             fill(50);
             text( "@" + t.from_user, 54, y+10 );
             text( t.text, 54, y+14, width-56, 30 );
             
             y += 52;
         }
     }
     else
     {
         translate( width/2, height/2 );
         noStroke();
         for ( float a = 0, r = 0; a < 360; a+=24 )
         {
             fill( map( int(a+frameCount)%360, 0, 360, 0, 100 ) );
             r = radians(a);
             ellipse( sin(r)*20, cos(r)*20, 5, 5 );
         }
     }
 }
 
 int pressedY; int pScrollY; boolean wasDragged = false;
 void mousePressed ()
 {
     pressedY = mouseY;
     pScrollY = scrollY;
     wasDragged = false;
 }
 
 void mouseMoved ()
 {
     selectTweet();
 }
 
 void mouseDragged ()
 {
     selectTweet();
     
     if ( tweets != null && tweets.size() > 0 )
     {
         int newScrollY = pScrollY + (mouseY-pressedY);
         if ( newScrollY > 0 && newScrollY < totalTweetsHeight - height )
         {
             scrollY = newScrollY;
         }
         fill(255);
         noStroke();
         rect( width-4, map(scrollY,0,totalTweetsHeight - height, 0, height-10), 4, 10 );
     }
     wasDragged = true;
 }
 
 void mouseReleased ()
 {
     selectTweet();
     
     if ( !wasDragged && selectedTweet != null )
     {
         link( "http://twitter.com/#!/" + selectedTweet.from_user + 
               "/status/" + selectedTweet.id_str, 
               "_new" );
     }
 }
 
 void selectTweet ()
 {
     int tweetY = int((scrollY + mouseY) / 52);
     if ( tweetY >= 0 && tweetY < tweets.size() )
     {
         selectedTweet = tweets.values()[tweetY];
     }
 }
 
 void resetTweets ()
 {
     tweets = null;
     selectedTweet = null;
     tweets = new HashMap<Integer,Tweet>();
     totalTweetsHeight = 0;
     scrollY = 0;
 }
 
 void newTweet ( Tweet t )
 {
     t.profile_image = loadImage(t.profile_image_url);
     tweets.put( t.id, t );
     totalTweetsHeight = (2 + tweets.size() * 52);
 }
 
 /* explain Processing how the incoming data looks like */
 interface Tweet
 {
     String created_at;
     String from_user;
     int from_user_id;
     /*geo*/
     int id;
     String id_str; /* sometimes differs from "id" */
     String iso_language_code;
     int to_user_id;
     String profile_image_url;
     String source;
     String text;
 }
