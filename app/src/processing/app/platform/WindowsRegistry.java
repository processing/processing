package processing.app.platform;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.TreeSet;

import com.sun.jna.platform.win32.Advapi32;
import com.sun.jna.platform.win32.WinBase;
import com.sun.jna.platform.win32.WinError;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.platform.win32.WinReg;
import com.sun.jna.platform.win32.WinReg.HKEY;
import com.sun.jna.platform.win32.WinReg.HKEYByReference;
import com.sun.jna.ptr.IntByReference;


/**
 * Methods for accessing the Windows Registry. Only String and DWORD values
 * supported at the moment.
 * <p>
 * Not sure of where this code came from originally, but it was hacked on
 * 20 July 2013 to make updates for use with JNA 3.5.2's platform classes.
 */
public class WindowsRegistry {
  static public enum REGISTRY_ROOT_KEY {
    CLASSES_ROOT, CURRENT_USER, LOCAL_MACHINE, USERS
  };
  //private final static HashMap<REGISTRY_ROOT_KEY, Integer> rootKeyMap = new HashMap<REGISTRY_ROOT_KEY, Integer>();
  private final static HashMap<REGISTRY_ROOT_KEY, WinReg.HKEY> rootKeyMap =
    new HashMap<REGISTRY_ROOT_KEY, WinReg.HKEY>();

  static {
    rootKeyMap.put(REGISTRY_ROOT_KEY.CLASSES_ROOT, WinReg.HKEY_CLASSES_ROOT);
    rootKeyMap.put(REGISTRY_ROOT_KEY.CURRENT_USER, WinReg.HKEY_CURRENT_USER);
    rootKeyMap.put(REGISTRY_ROOT_KEY.LOCAL_MACHINE, WinReg.HKEY_LOCAL_MACHINE);
    rootKeyMap.put(REGISTRY_ROOT_KEY.USERS, WinReg.HKEY_USERS);
  }


  /**
   * Gets one of the root keys.
   *
   * @param key key type
   * @return root key
   */
  private static HKEY getRegistryRootKey(REGISTRY_ROOT_KEY key) {
    Advapi32 advapi32;
    //IntByReference pHandle;
    HKEYByReference pHandle;
    //int handle = 0;
    HKEY handle = null;

    advapi32 = Advapi32.INSTANCE;
//    pHandle = new IntByReference();
    pHandle = new WinReg.HKEYByReference();

    if (advapi32.RegOpenKeyEx(rootKeyMap.get(key), null, 0, 0, pHandle) == WinError.ERROR_SUCCESS) {
      handle = pHandle.getValue();
    }
    return handle;
  }


  /**
   * Opens a key.
   *
   * @param rootKey root key
   * @param subKeyName name of the key
   * @param access access mode
   * @return handle to the key or 0
   */
  private static HKEY openKey(REGISTRY_ROOT_KEY rootKey, String subKeyName, int access) {
    //Advapi32 advapi32;
    //IntByReference pHandle;
    //int rootKeyHandle;

    Advapi32 advapi32 = Advapi32.INSTANCE;
    HKEY rootKeyHandle = getRegistryRootKey(rootKey);
    //pHandle = new IntByReference();
    HKEYByReference pHandle = new HKEYByReference();

    if (advapi32.RegOpenKeyEx(rootKeyHandle, subKeyName, 0, access, pHandle) == WinError.ERROR_SUCCESS) {
      return pHandle.getValue();
    } else {
      return null;
    }
  }


  /**
   * Converts a Windows buffer to a Java String.
   *
   * @param buf buffer
   * @throws java.io.UnsupportedEncodingException on error
   * @return String
   */
  private static String convertBufferToString(byte[] buf) throws UnsupportedEncodingException {
    return new String(buf, 0, buf.length - 2, "UTF-16LE");
  }


  /**
   * Converts a Windows buffer to an int.
   *
   * @param buf buffer
   * @return int
   */
  private static int convertBufferToInt(byte[] buf) {
    return ((buf[0] & 0xff) +
            ((buf[1] & 0xff) << 8) +
            ((buf[2] & 0xff) << 16) +
            ((buf[3] & 0xff) << 24));
  }


  /**
   * Read a String value.
   *
   * @param rootKey root key
   * @param subKeyName key name
   * @param name value name
   * @throws java.io.UnsupportedEncodingException on error
   * @return String or null
   */
  static public String getStringValue(REGISTRY_ROOT_KEY rootKey, String subKeyName, String name) throws UnsupportedEncodingException {
    //Advapi32 advapi32;
    //IntByReference pType, lpcbData;
    byte[] lpData = new byte[1];
    //int handle = 0;
    String ret = null;

    Advapi32 advapi32 = Advapi32.INSTANCE;
    IntByReference pType = new IntByReference();
    IntByReference lpcbData = new IntByReference();
    HKEY handle = openKey(rootKey, subKeyName, WinNT.KEY_READ);

    //if (handle != 0) {
    if (handle != null) {
      //if (advapi32.RegQueryValueEx(handle, name, null, pType, lpData, lpcbData) == WinError.ERROR_MORE_DATA) {
      if (advapi32.RegQueryValueEx(handle, name, 0, pType, lpData, lpcbData) == WinError.ERROR_MORE_DATA) {
        lpData = new byte[lpcbData.getValue()];

        //if (advapi32.RegQueryValueEx(handle, name, null, pType, lpData, lpcbData) == WinError.ERROR_SUCCESS) {
        if (advapi32.RegQueryValueEx(handle, name, 0, pType, lpData, lpcbData) == WinError.ERROR_SUCCESS) {
          ret = convertBufferToString(lpData);
        }
      }
      advapi32.RegCloseKey(handle);
    }
    return ret;
  }


  /**
   * Read an int value.
   *
   *
   * @return int or 0
   * @param rootKey root key
   * @param subKeyName key name
   * @param name value name
   */
  static public int getIntValue(REGISTRY_ROOT_KEY rootKey, String subKeyName, String name) {
    Advapi32 advapi32;
    IntByReference pType;
    IntByReference lpcbData;
    byte[] lpData = new byte[1];
    //int handle = 0;
    HKEY handle = null;
    int ret = 0;

    advapi32 = Advapi32.INSTANCE;
    pType = new IntByReference();
    lpcbData = new IntByReference();
    handle = openKey(rootKey, subKeyName, WinNT.KEY_READ);

    //if(handle != 0) {
    if (handle != null) {
      //if (advapi32.RegQueryValueEx(handle, name, null, pType, lpData, lpcbData) == WinError.ERROR_MORE_DATA) {
      if (advapi32.RegQueryValueEx(handle, name, 0, pType, lpData, lpcbData) == WinError.ERROR_MORE_DATA) {
        lpData = new byte[lpcbData.getValue()];

        //if(advapi32.RegQueryValueEx(handle, name, null, pType, lpData, lpcbData) == WinError.ERROR_SUCCESS) {
        if (advapi32.RegQueryValueEx(handle, name, 0, pType, lpData, lpcbData) == WinError.ERROR_SUCCESS) {
          ret = convertBufferToInt(lpData);
        }
      }
      advapi32.RegCloseKey(handle);
    }
    return ret;
  }


  /**
   * Delete a value.
   *
   * @param rootKey root key
   * @param subKeyName key name
   * @param name value name
   * @return true on success
   */
  static public boolean deleteValue(REGISTRY_ROOT_KEY rootKey, String subKeyName, String name) {
    Advapi32 advapi32 = Advapi32.INSTANCE;
    //int handle;
    boolean ret = true;

    HKEY handle = openKey(rootKey, subKeyName, WinNT.KEY_READ | WinNT.KEY_WRITE);

    //if(handle != 0) {
    if (handle != null) {
      if (advapi32.RegDeleteValue(handle, name) == WinError.ERROR_SUCCESS) {
        ret = true;
      }
      advapi32.RegCloseKey(handle);
    }
    return ret;
  }


  /**
   * Writes a String value.
   *
   * @param rootKey root key
   * @param subKeyName key name
   * @param name value name
   * @param value value
   * @throws java.io.UnsupportedEncodingException on error
   * @return true on success
   */
  static public boolean setStringValue(REGISTRY_ROOT_KEY rootKey, String subKeyName, String name, String value) throws UnsupportedEncodingException {
    //int handle;
    byte[] data;
    boolean ret = false;

    // appears to be Java 1.6 syntax, removing [fry]
    //data = Arrays.copyOf(value.getBytes("UTF-16LE"), value.length() * 2 + 2);
    data = new byte[value.length() * 2 + 2];
    byte[] src = value.getBytes("UTF-16LE");
    System.arraycopy(src, 0, data, 0, src.length);

    Advapi32 advapi32 = Advapi32.INSTANCE;
    HKEY handle = openKey(rootKey, subKeyName, WinNT.KEY_READ | WinNT.KEY_WRITE);

    //if(handle != 0) {
    if (handle != null) {
      if (advapi32.RegSetValueEx(handle, name, 0, WinNT.REG_SZ, data, data.length) == WinError.ERROR_SUCCESS) {
        ret = true;
      }
      advapi32.RegCloseKey(handle);
    }
    return ret;
  }


  /**
   * Writes an int value.
   *
   *
   * @return true on success
   * @param rootKey root key
   * @param subKeyName key name
   * @param name value name
   * @param value value
   */
  static public boolean setIntValue(REGISTRY_ROOT_KEY rootKey, String subKeyName, String name, int value) {
    Advapi32 advapi32;
    //int handle;
    byte[] data;
    boolean ret = false;

    data = new byte[4];
    data[0] = (byte)(value & 0xff);
    data[1] = (byte)((value >> 8) & 0xff);
    data[2] = (byte)((value >> 16) & 0xff);
    data[3] = (byte)((value >> 24) & 0xff);
    advapi32 = Advapi32.INSTANCE;
    HKEY handle = openKey(rootKey, subKeyName, WinNT.KEY_READ | WinNT.KEY_WRITE);

    //if(handle != 0) {
    if (handle != null) {
      if (advapi32.RegSetValueEx(handle, name, 0, WinNT.REG_DWORD, data, data.length) == WinError.ERROR_SUCCESS) {
        ret = true;
      }
      advapi32.RegCloseKey(handle);
    }
    return ret;
  }


  /**
   * Check for existence of a value.
   *
   * @param rootKey root key
   * @param subKeyName key name
   * @param name value name
   * @return true if exists
   */
  static public boolean valueExists(REGISTRY_ROOT_KEY rootKey, String subKeyName, String name) {
    //Advapi32 advapi32;
    IntByReference pType, lpcbData;
    byte[] lpData = new byte[1];
    //int handle = 0;
    boolean ret = false;

    Advapi32 advapi32 = Advapi32.INSTANCE;
    pType = new IntByReference();
    lpcbData = new IntByReference();
    HKEY handle = openKey(rootKey, subKeyName, WinNT.KEY_READ);

    //if(handle != 0) {
    if (handle != null) {
      //if (advapi32.RegQueryValueEx(handle, name, null, pType, lpData, lpcbData) != WinError.ERROR_FILE_NOT_FOUND) {
      if (advapi32.RegQueryValueEx(handle, name, 0, pType, lpData, lpcbData) != WinError.ERROR_FILE_NOT_FOUND) {
        ret = true;

      } else {
        ret = false;
      }
      advapi32.RegCloseKey(handle);
    }
    return ret;
  }


  /**
   * Create a new key.
   *
   * @param rootKey root key
   * @param parent name of parent key
   * @param name key name
   * @return true on success
   */
  static public boolean createKey(REGISTRY_ROOT_KEY rootKey, String parent, String name) {
    //Advapi32 advapi32;
    //IntByReference hkResult, dwDisposition;
    //int handle = 0;
    boolean ret = false;

    Advapi32 advapi32 = Advapi32.INSTANCE;
    //IntByReference hkResult = new IntByReference();
    HKEYByReference hkResult = new HKEYByReference();
    IntByReference dwDisposition = new IntByReference();
    HKEY handle = openKey(rootKey, parent, WinNT.KEY_READ);

    //if(handle != 0) {
    if (handle != null) {
      if (advapi32.RegCreateKeyEx(handle, name, 0, null, WinNT.REG_OPTION_NON_VOLATILE, WinNT.KEY_READ, null,
         hkResult, dwDisposition) == WinError.ERROR_SUCCESS) {
        ret = true;
        advapi32.RegCloseKey(hkResult.getValue());

      } else {
        ret = false;
      }
      advapi32.RegCloseKey(handle);
    }
    return ret;
  }


  /**
   * Delete a key.
   *
   * @param rootKey root key
   * @param parent name of parent key
   * @param name key name
   * @return true on success
   */
  static public boolean deleteKey(REGISTRY_ROOT_KEY rootKey, String parent, String name) {
    //Advapi32 advapi32;
    //int handle = 0;
    boolean ret = false;

    Advapi32 advapi32 = Advapi32.INSTANCE;
    HKEY handle = openKey(rootKey, parent, WinNT.KEY_READ);

    //if(handle != 0) {
    if (handle != null) {
      if (advapi32.RegDeleteKey(handle, name) == WinError.ERROR_SUCCESS) {
        ret = true;

      } else {
        ret = false;
      }
      advapi32.RegCloseKey(handle);
    }
    return ret;
  }


  /**
   * Get all sub keys of a key.
   *
   * @param rootKey root key
   * @param parent key name
   * @return array with all sub key names
   */
  static public String[] getSubKeys(REGISTRY_ROOT_KEY rootKey, String parent) {
    //Advapi32 advapi32;
    //int handle = 0, dwIndex;
    char[] lpName;
    IntByReference lpcName;
    WinBase.FILETIME lpftLastWriteTime;
    TreeSet<String> subKeys = new TreeSet<String>();

    Advapi32 advapi32 = Advapi32.INSTANCE;
    HKEY handle = openKey(rootKey, parent, WinNT.KEY_READ);
    lpName = new char[256];
    lpcName = new IntByReference(256);
    lpftLastWriteTime = new WinBase.FILETIME();

    //if(handle != 0) {
    if (handle != null) {
      int dwIndex = 0;

      while(advapi32.RegEnumKeyEx(handle, dwIndex, lpName, lpcName, null,
            null, null, lpftLastWriteTime) == WinError.ERROR_SUCCESS) {
        subKeys.add(new String(lpName, 0, lpcName.getValue()));
        lpcName.setValue(256);
        dwIndex++;
      }
      advapi32.RegCloseKey(handle);
    }
    return subKeys.toArray(new String[] { });
  }


  /**
   * Get all values under a key.
   *
   * @param rootKey root key
   * @param key jey name
   * @throws java.io.UnsupportedEncodingException on error
   * @return TreeMap with name and value pairs
   */
  static public TreeMap<String, Object> getValues(REGISTRY_ROOT_KEY rootKey, String key) throws UnsupportedEncodingException {
    //Advapi32 advapi32;
    //int handle = 0, dwIndex, result = 0;
    char[] lpValueName;
    byte[] lpData;
    IntByReference lpcchValueName, lpType, lpcbData;
    String name;
    TreeMap<String, Object> values = new TreeMap<String, Object>(String.CASE_INSENSITIVE_ORDER);

    Advapi32 advapi32 = Advapi32.INSTANCE;
    HKEY handle = openKey(rootKey, key, WinNT.KEY_READ);
    lpValueName = new char[16384];
    lpcchValueName = new IntByReference(16384);
    lpType = new IntByReference();
    lpData = new byte[1];
    lpcbData = new IntByReference();

    //if(handle != 0) {
    if (handle != null) {
      int dwIndex = 0;
      int result = 0;

      do {
        lpcbData.setValue(0);
        result = advapi32.RegEnumValue(handle, dwIndex, lpValueName, lpcchValueName, null,
          lpType, lpData, lpcbData);

        if (result == WinError.ERROR_MORE_DATA) {
          lpData = new byte[lpcbData.getValue()];
          lpcchValueName =  new IntByReference(16384);
          result = advapi32.RegEnumValue(handle, dwIndex, lpValueName, lpcchValueName, null,
            lpType, lpData, lpcbData);

          if (result == WinError.ERROR_SUCCESS) {
            name = new String(lpValueName, 0, lpcchValueName.getValue());

            switch(lpType.getValue()) {
              case WinNT.REG_SZ:
                values.put(name, convertBufferToString(lpData));
                break;
              case WinNT.REG_DWORD:
                values.put(name, convertBufferToInt(lpData));
                break;
              default:
                break;
            }
          }
        }
        dwIndex++;
      } while (result == WinError.ERROR_SUCCESS);

      advapi32.RegCloseKey(handle);
    }
    return values;
  }
}
