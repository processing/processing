package processing.core.util.image.save;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;


public class ImageSaveUtil {

  public static OutputStream createForFile(String filename) throws FileNotFoundException {
    return new BufferedOutputStream(new FileOutputStream(filename), 32768);
  }

}
