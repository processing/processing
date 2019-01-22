package processing.core.util.image.save;


import java.io.FileNotFoundException;
import java.io.IOException;

public interface ImageSaveStrategy {

  boolean save(int[] pixels, int pixelWidth, int pixelHeight, int format,
               String filename) throws IOException;

}
