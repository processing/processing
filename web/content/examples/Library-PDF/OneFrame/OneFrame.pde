/**
 * Mouse Press
 * 
 * Saves one PDF with the contents of the display window.
 * Because this example uses beginRecord, the image is shown
 * to the display window and is saved to the file. 
 * 
 * Created 14 June 2006
 * 
*/


import processing.pdf.*;

size(600, 600);

beginRecord(PDF, "line.pdf"); 

background(255);
stroke(0, 20);
strokeWeight(20.0);
line(200, 0, 400, height);

endRecord();


