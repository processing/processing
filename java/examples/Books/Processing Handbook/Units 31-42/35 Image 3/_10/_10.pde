for (int i = 0; i < 55; i++) {
  for (int j = 0; j < 55; j++) {
    color c = color((i + j) * 1.8);
    set(30 + i, 20 + j, c);
  }
}
