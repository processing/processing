size(100, 100, P3D);

beginShape(POLYGON);
fill(0, 255, 0, 255);
stroke(0, 0, 255, 255);
strokeWeight(10);
strokeJoin(ROUND);
vertex(10, 10);
vertex(10, 90);
vertex(90, 90);
vertex(90, 10);  
breakShape();
vertex(40, 40);
vertex(70, 40);
vertex(70, 70);
vertex(40, 70);
endShape(CLOSE);

