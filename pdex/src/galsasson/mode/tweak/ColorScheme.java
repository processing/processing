package galsasson.mode.tweak;

import java.awt.Color;

public class ColorScheme {
	private static ColorScheme instance = null;
	public Color redStrokeColor;
	public Color progressFillColor;
	public Color progressEmptyColor;
	public Color markerColor;
	public Color whitePaneColor;
	
	private ColorScheme()
	{
		redStrokeColor = new Color(160, 20, 20);	// dark red
		progressEmptyColor = new Color(180, 180, 180, 200);
		progressFillColor = new Color(0, 0, 0, 200);
		markerColor = new Color(228, 200, 91, 127);
		whitePaneColor = new Color(255, 255, 255, 120);
	}
	
	public static ColorScheme getInstance() {
		if (instance == null) {
			instance = new ColorScheme();
		}
		return instance;
	}

}


