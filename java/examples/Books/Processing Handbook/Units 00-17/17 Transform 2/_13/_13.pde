background(0);
smooth();
stroke(255, 120);
translate(66, 33); // Set initial offset
for (int i = 0; i < 18; i++) { // 18 repetitions
  strokeWeight(i); // Increase stroke weight
  rotate(PI/12); // Accumulate the rotation
  line(0, 0, 55, 0);
}