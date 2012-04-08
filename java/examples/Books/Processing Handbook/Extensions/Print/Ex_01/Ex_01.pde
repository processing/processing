import processing.pdf.*; // Import PDF code

size(600, 600, PDF, "line.pdf"); // Set PDF as the renderer
background(255);
stroke(0);
line(200, 0, width/2, height); // Draw line to PDF
exit(); // Stop the program