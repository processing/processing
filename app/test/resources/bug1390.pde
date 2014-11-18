import java.lang.*;

enum Operation {
  @Deprecated ADD_10(10)                 { protected int apply(int x) { return x + y; } },
  MULT_5(5)                              { protected int apply(int x) { return x * y; } },
  @SuppressWarnings("serial") DIV_10(10) { protected int apply(int x) { return x / y; } },
  SUB_8(8)                               { protected int apply(int x) { return x - y; } };
  
  final int y;
  
  Operation(int y) {
  	this.y = y;
  }
  
  protected abstract int apply(int x);
}

Operation operation = Operation.ADD_10;

void setup() {
  int x = 10;
  println("Original:", x);
  for (Operation op : Operation.values()) {
      x = op.apply(x);
      println(op.toString(), x);
  }
  x = operation.apply(x);
  println(operation.toString(), x);
}
