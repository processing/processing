package processing.app;

import java.io.File;


public class SketchReference {
  String name;
  File pde;


  public SketchReference(String name, File pde) {
    this.name = name;
    this.pde = pde;
  }


  public String getPath() {
    return pde.getAbsolutePath();
  }


  public String toString() {
    return name;
  }
}