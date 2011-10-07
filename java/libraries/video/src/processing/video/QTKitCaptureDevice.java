package processing.video;

import com.sun.jna.Library;
import com.sun.jna.Native;

/**
 * This JNA interface provides access to the environment variable-related functions in the C library.
 * How to use:
 * CLibrary clib = CLibrary.INSTANCE;         
 * String s = clib.getenv("DYLD_LIBRARY_PATH");
 */    
public interface QTKitCaptureDevice extends Library {
  QTKitCaptureDevice INSTANCE = (QTKitCaptureDevice)Native.loadLibrary("c", QTKitCaptureDevice.class);
  
  int setenv(String name, String value, int overwrite);
  String getenv(String name);
  int unsetenv(String name);
  int putenv(String string);
}




