package processing.app;

import java.io.File;

import processing.app.ui.SplashWindow;
import processing.app.ui.Toolkit;


public class BaseSplash {
  static public void main(String[] args) {
    try {
      final boolean hidpi = Toolkit.highResImages();
      final String filename = "lib/about-" + (hidpi ? 2 : 1) + "x.png";
      File splashFile = Platform.getContentFile(filename);
      SplashWindow.splash(splashFile.toURI().toURL(), hidpi);
      SplashWindow.invokeMain("processing.app.Base", args);
      SplashWindow.disposeSplash();
    } catch (Exception e) {
      e.printStackTrace();
      // !@#!@$$! umm
      //SplashWindow.invokeMain("processing.app.Base", args);
    }
  }
}