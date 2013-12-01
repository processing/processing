/**
 * Get Meta Data
 * by Damien Di Fede.
 *  
 * This sketch demonstrates how to use the <code>getMetaData</code> 
 * method of <code>AudioPlayer</code>. This method is also available 
 * for <code>AudioSnippet</code> and <code>AudioSample</code>. 
 * You should use this method when you want to retrieve metadata 
 * about a file that you have loaded, like ID3 tags from an mp3 file. 
 * If you load WAV file or other non-tagged file, most of the metadata 
 * will be empty, but you will still have information like the filename 
 * and the length.
 * <p>
 * For more information about Minim and additional features, 
 * visit http://code.compartmental.net/minim/ 
 */

import ddf.minim.*;

Minim minim;
AudioPlayer groove;
AudioMetaData meta;

void setup()
{
  size(512, 256, P2D);
  
  minim = new Minim(this);
  groove = minim.loadFile("groove.mp3");
  meta = groove.getMetaData();
  
  textFont(createFont("Serif", 12));
}

int ys = 25;
int yi = 15;

void draw()
{
  background(0);
  int y = ys;
  text("File Name: " + meta.fileName(), 5, y);
  text("Length (in milliseconds): " + meta.length(), 5, y+=yi);
  text("Title: " + meta.title(), 5, y+=yi);
  text("Author: " + meta.author(), 5, y+=yi); 
  text("Album: " + meta.album(), 5, y+=yi);
  text("Date: " + meta.date(), 5, y+=yi);
  text("Comment: " + meta.comment(), 5, y+=yi);
  text("Track: " + meta.track(), 5, y+=yi);
  text("Genre: " + meta.genre(), 5, y+=yi);
  text("Copyright: " + meta.copyright(), 5, y+=yi);
  text("Disc: " + meta.disc(), 5, y+=yi);
  text("Composer: " + meta.composer(), 5, y+=yi);
  text("Orchestra: " + meta.orchestra(), 5, y+=yi);
  text("Publisher: " + meta.publisher(), 5, y+=yi);
  text("Encoded: " + meta.encoded(), 5, y+=yi);
}
