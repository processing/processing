// The same rectangle is drawn, but only the second is
// affected by translate() because it is drawn after
rect(0, 5, 70, 30);
translate(10, 30); // Shifts 10 pixels right and 30 down
rect(0, 5, 70, 30);