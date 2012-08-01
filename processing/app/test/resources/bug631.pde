firstLoop:
	for (int i = 0; i < 10; i++) {
		for (int j = 0; j < 10; j++) {
			if ((i+j) % 5 != 0) continue firstLoop;
			System.out.println(i + " " + j);
		}
	} 