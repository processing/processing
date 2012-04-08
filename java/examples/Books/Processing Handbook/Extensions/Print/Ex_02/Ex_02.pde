import processing.pdf.*; // Import PDF code

size(600, 600);
beginRecord(PDF, "line.pdf"); // Start writing to PDF
background(255);
stroke(0, 20);
strokeWeight(20);
line(200, 0, 400, height); // Draw line to screen and to PDF
endRecord(); // Stop writing to PDF