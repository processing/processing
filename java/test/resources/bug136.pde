import java.util.Collections;
java.util.List alist = Collections.synchronizedList(new ArrayList());

void setup() {
size(400, 200);
alist.add("hello");
}

void draw() {
rect(width/4, height/4, width/2, height/2);
synchronized(alist) {
alist.get(0);
}
}