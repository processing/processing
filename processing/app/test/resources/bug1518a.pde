import java.util.ArrayList;
import java.util.List;

void setup()
{
List<String> list = new ArrayList<String>();
list.add("foo");
list.add("bar");
list.add("baz");

binarySearch(list, "bar");
}

static <T> int binarySearch(List<? extends Comparable<? super T>> list, T
key) {
return 0;
}