/**
 * Getting Started. 
 * Illustration by George Brower. 
 * 
 * The Candy SVG Import library is used for loading SVG (Scalable Vector Graphics)
 * files into Processing. This library was specifically tested under SVG files created 
 * from Adobe Illustrator. For now, we can't guarantee that it'll work for SVG's 
 * created from anything else. 
 * 
 * An SVG created under Illustrator must be created in one of two ways:
 *  - File > Save for Web (or control-alt-shift-s on a PC). Under settings, make sure 
 *    the CSS properties is set to "Presentation Attributes"
 *  - With Illustrator CS2, it is also possible to use "Save As" with "SVG" as the file 
 *    setting, but the CSS properties set to "Presentation Attributes" 
 * Saving it any other way will most likely break Candy.
 * 
 * For more information, visit:
 * http://www.processing.org/reference/libraries/candy/
 */

import processing.candy.*;
import processing.xml.*;

SVG bot;

void setup(){
  size(200, 200);
  smooth();
  // The file "bot1.svg" must be in the data folder
  // of the current sketch to load successfully
  bot = new SVG(this, "bot1.svg");
} 

void draw(){
  background(102);
  bot.draw(10, 10, 100, 100);  // Draw at coordinate [10, 10] at size 100 x 100
  bot.draw(70, 60);            // Draw at coordinate [70, 60] at the default size
  
  shape(bot, 10, 10, 100, 100);
}
