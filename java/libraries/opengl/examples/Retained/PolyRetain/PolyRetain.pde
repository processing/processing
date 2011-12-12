size(100, 100, P3D);

PShape3D obj = (PShape3D)createShape(POLYGON);
obj.fill(0, 255, 0, 255);
obj.stroke(0, 0, 255, 255);
obj.strokeWeight(10);
obj.strokeJoin(ROUND);
obj.vertex(10, 10);
obj.vertex(10, 90);
obj.vertex(90, 90);
obj.vertex(90, 10);  
obj.breakShape();
obj.vertex(40, 40);
obj.vertex(70, 40);
obj.vertex(70, 70);
obj.vertex(40, 70);
obj.end(CLOSE);

shape(obj);
