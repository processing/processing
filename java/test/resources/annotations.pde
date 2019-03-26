import java.io.Serializable;

void setup() {
    size(200,200);
}

@Deprecated
public void banana() {
    println("hey");
}

@SuppressWarnings({"serial", "rawtypes"})
class Banana implements Serializable {

}

@SuppressWarnings("serial")
class Apple implements Serializable {

}

@javax.annotation.Generated(value = {"com.mrfeinberg.ImmortalAroma" 
}, 
    comments="Shazam!", 
    date="2001-07-04T12:08:56.235-0700")
class Pear {}