smooth();
noFill();
beginShape();
vertex(15, 40); // V1 (see p.76)
bezierVertex(5, 0, 80, 0, 50, 55); // C1, C2, V2
vertex(30, 45); // V3
vertex(25, 75); // V4
bezierVertex(50, 70, 75, 90, 80, 70); // C3, C4, V5
endShape();