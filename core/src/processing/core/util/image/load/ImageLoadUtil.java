package processing.core.util.image.load;

public class ImageLoadUtil {

  public static boolean checkExtensionRequiresAlpha(String extension) {
    return extension.equals("gif") ||
        extension.equals("png") ||
        extension.equals("unknown");
  }

}
