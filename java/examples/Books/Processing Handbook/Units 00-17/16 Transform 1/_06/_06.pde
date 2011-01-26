pushMatrix();
translate(20, 0);
rect(0, 10, 70, 20); // Draws at (20, 30)
pushMatrix();
translate(30, 0);
rect(0, 30, 70, 20); // Draws at (50, 30)
popMatrix();
rect(0, 50, 70, 20); // Draws at (20, 50)
popMatrix();
rect(0, 70, 70, 20); // Draws at (0, 70)