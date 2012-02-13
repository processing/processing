size(100, 100, P3D);

beginShape(POLYGON);
fill(0, 255, 0, 255);
stroke(0, 0, 255, 255);
strokeWeight(5);
strokeJoin(ROUND);
beginContour();
vertex(10, 10);
vertex(10, 90);
vertex(90, 90);
vertex(90, 10);  
endContour();
beginContour();
vertex(40, 40);
vertex(70, 40);
vertex(70, 70);
vertex(40, 70);
endContour();
endShape(CLOSE);

