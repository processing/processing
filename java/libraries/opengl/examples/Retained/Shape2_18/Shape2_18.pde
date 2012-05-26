size(100, 100, P2D);
smooth();

PShape obj = createShape();
obj.noFill();
obj.stroke(0);
obj.strokeWeight(1);
obj.vertex(15, 40); // V1 (see p.76)
obj.bezierVertex(5, 0, 80, 0, 50, 55); // C1, C2, V2
obj.vertex(30, 45); // V3
obj.vertex(25, 75); // V4
obj.bezierVertex(50, 70, 75, 90, 80, 70); // C3, C4, V5
obj.end();  

shape(obj);
