// java 5 torture test

import static java.lang.Math.tanh;
import java.util.concurrent.Callable;
import java.util.List;
import java.util.Comparator;
import java.util.Map;
import java.util.Collection;
import java.util.Arrays;
import java.util.HashSet;


private static Comparator<String> rotarapmoc = new Comparator<String>() {
  public int compare(final String o1, final String o2)
  {
    return o1.charAt(o1.length() - 1) - o2.charAt(o2.length() - 1);
  }
};

final <T> void printClass(T t) {
   println(t.getClass()); 
}
final List<String> sortem(final String... strings) {
   Arrays.sort(strings,  rotarapmoc);
   return Arrays.asList(strings);
}

final Map<String, Collection<Integer>>
charlesDeGaulle = new HashMap<String, Collection<Integer>>();

void setup() {
  charlesDeGaulle.put("banana", new HashSet<Integer>());
  charlesDeGaulle.get("banana").add(0);
  System.out.println(sortem("aztec", "maya", "spanish", "portuguese"));
  printClass(12.d);
}
