smooth();
noFill();
beginShape();
vertex(15, 30); // V1 (see p.76)
bezierVertex(20, -5, 70, 5, 40, 35); // C1, C2, V2
bezierVertex(5, 70, 45, 105, 70, 70); // C3, C4, V3
endShape();