import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.Comparator;

Comparator<String> comparator = new Comparator<String>()
{
public int compare(final String value0, final String value1)
{
return value0.compareTo(value1);
}
};

void setup()
{
List<String> list = new ArrayList<String>();
list.add("foo");
list.add("bar");
list.add("baz");

Collections.sort(list, comparator);
}