/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2012-2013 The Processing Foundation
  Copyright (c) 2008-2012 Ben Fry and Casey Reas

  This program is free software; you can redistribute it and/or
  modify it under the terms of the GNU General Public License
  version 2, as published by the Free Software Foundation.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program; if not, write to the Free Software Foundation,
  Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package processing.app.platform;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.platform.win32.Kernel32Util;
import com.sun.jna.platform.win32.Shell32;
import com.sun.jna.platform.win32.ShlObj;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinError;
import com.sun.jna.platform.win32.WinNT.HRESULT;

import processing.app.Base;
import processing.app.Messages;
import processing.app.Preferences;
import processing.app.platform.DefaultPlatform;
import processing.app.platform.WindowsRegistry.REGISTRY_ROOT_KEY;
import processing.core.PApplet;


/**
 * Platform-specific glue for Windows.
 */
public class WindowsPlatform extends DefaultPlatform {

  static final String APP_NAME = "Processing";
  static final String REG_OPEN_COMMAND =
    System.getProperty("user.dir").replace('/', '\\') +
    "\\" + APP_NAME.toLowerCase() + ".exe \"%1\"";
  static final String REG_DOC = APP_NAME + ".Document";


  public void initBase(Base base) {
    super.initBase(base);
    checkAssociations();
    //checkQuickTime();
    checkPath();

    /*
    File f = new File(System.getProperty("user.dir"), "recycle-test.txt");
    //File f = new File("C:\\recycle-test.txt");
    System.out.println(f.getAbsolutePath());
    java.io.PrintWriter writer = PApplet.createWriter(f);
    writer.println("blah");
    writer.flush();
    writer.close();
    try {
      deleteFile(f);
    } catch (IOException e) {
      e.printStackTrace();
    }
    */

    //findJDK();
    /*
    new Thread(new Runnable() {
      public void run() {
        try {
          Thread.sleep(2000);
        } catch (InterruptedException ie) { }
        findJDK();
      }
    }).start();
    */
  }


// HKEY_LOCAL_MACHINE\SOFTWARE\JavaSoft\Java Development Kit\CurrentVersion -> 1.6 (String)
// HKEY_LOCAL_MACHINE\SOFTWARE\JavaSoft\Java Development Kit\CurrentVersion\1.6\JavaHome -> c:\jdk-1.6.0_05

  /*
  static public void findJDK() {
    try {
      String jcpo = System.getProperty("java.home");
      String jv = System.getProperty("java.version");
      System.out.println("home and version = " + jcpo + " and " + jv);

      // the last parameter will be anything appearing on the right-hand
      // side of regedit.
      final String JDK_KEY = "SOFTWARE\\JavaSoft\\Java Development Kit";
      String currentVersion =
        Registry.getStringValue(REGISTRY_ROOT_KEY.LOCAL_MACHINE,
                                JDK_KEY,
                                "CurrentVersion");
      System.out.println("current version is " + currentVersion);
      if (currentVersion != null) {
        String javaHome =
          Registry.getStringValue(REGISTRY_ROOT_KEY.LOCAL_MACHINE,
                                  JDK_KEY + "\\" + currentVersion,
                                  "JavaHome");
        System.out.println("home is where the " + javaHome + " is");
        if (javaHome != null) {
          String jcp = System.getProperty("java.class.path");
          String toolsJar = javaHome + "\\lib\\tools.jar";
          System.setProperty("java.class.path",
                             jcp + File.pathSeparator + toolsJar);
          System.out.println("set jcp to " +
                             System.getProperty("java.class.path"));
        }
      }
    } catch (UnsupportedEncodingException uee) {
      uee.printStackTrace();
    }
  }
  */


  /**
   * Make sure that .pde files are associated with processing.exe.
   */
  protected void checkAssociations() {
    try {
      if (Preferences.getBoolean("platform.auto_file_type_associations")) {
        // Check the key that should be set by a previous run of Processing
        String knownCommand =
          WindowsRegistry.getStringValue(REGISTRY_ROOT_KEY.CURRENT_USER,
                                  "Software\\Classes\\" + REG_DOC + "\\shell\\open\\command", "");
        // If the association hasn't been set, or it's not correct, set it.
        if (knownCommand == null || !knownCommand.equals(REG_OPEN_COMMAND)) {
          setAssociations();
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }


  /**
   * Associate .pde files with this version of Processing. After 2.0.1,
   * this was changed to only set the values for the current user, so that
   * it would no longer silently fail on systems that have UAC turned on.
   */
  protected void setAssociations() throws UnsupportedEncodingException {
    // http://support.microsoft.com/kb/184082
    // http://msdn.microsoft.com/en-us/library/cc144175%28v=VS.85%29.aspx
    // http://msdn.microsoft.com/en-us/library/cc144104%28v=VS.85%29.aspx
    // http://msdn.microsoft.com/en-us/library/cc144067%28v=VS.85%29.aspx
    // msdn.microsoft.com/en-us/library/windows/desktop/ms724475(v=vs.85).aspx

//    HKEY_CLASSES_ROOT
//    MyProgram.exe
//       shell
//          open
//             command
//                (Default) = C:\MyDir\MyProgram.exe "%1"

/*
    REGISTRY_ROOT_KEY rootKey = REGISTRY_ROOT_KEY.CLASSES_ROOT;
    if (Registry.createKey(rootKey,
                           "", ".pde") &&
        Registry.setStringValue(rootKey,
                                ".pde", "", DOC) &&

        Registry.createKey(rootKey, "", DOC) &&
        Registry.setStringValue(rootKey, DOC, "",
                                "Processing Source Code") &&

        Registry.createKey(rootKey,
                           DOC, "shell") &&
        Registry.createKey(rootKey,
                           DOC + "\\shell", "open") &&
        Registry.createKey(rootKey,
                           DOC + "\\shell\\open", "command") &&
        Registry.setStringValue(rootKey,
                                DOC + "\\shell\\open\\command", "",
                                openCommand)) {
*/

    // "To change the settings for the interactive user, store the changes
    // under HKEY_CURRENT_USER\Software\Classes rather than HKEY_CLASSES_ROOT."
    // msdn.microsoft.com/en-us/library/windows/desktop/ms724475(v=vs.85).aspx
    final REGISTRY_ROOT_KEY rootKey = REGISTRY_ROOT_KEY.CURRENT_USER;
    final String docPrefix = "Software\\Classes\\" + REG_DOC;

    // First create the .pde association
    if (WindowsRegistry.createKey(rootKey, "Software\\Classes", ".pde") &&
        WindowsRegistry.setStringValue(rootKey, "Software\\Classes\\.pde", "", REG_DOC) &&

        // Now give files with a .pde extension a name for the explorer
        WindowsRegistry.createKey(rootKey, "Software\\Classes", REG_DOC) &&
        WindowsRegistry.setStringValue(rootKey, docPrefix, "", APP_NAME + " Source Code") &&

        // Now associate the 'open' command with the current processing.exe
        WindowsRegistry.createKey(rootKey, docPrefix, "shell") &&
        WindowsRegistry.createKey(rootKey, docPrefix + "\\shell", "open") &&
        WindowsRegistry.createKey(rootKey, docPrefix + "\\shell\\open", "command") &&
        WindowsRegistry.setStringValue(rootKey, docPrefix + "\\shell\\open\\command", "", REG_OPEN_COMMAND)) {

      // everything ok
      // hooray!

    } else {
      Messages.log("Could not associate files, turning off auto-associate pref.");
      Preferences.setBoolean("platform.auto_file_type_associations", false);
    }
  }


  /**
   * Remove extra quotes, slashes, and garbage from the Windows PATH.
   */
  protected void checkPath() {
    String path = System.getProperty("java.library.path");
    String[] pieces = PApplet.split(path, File.pathSeparatorChar);
    String[] legit = new String[pieces.length];
    int legitCount = 0;
    for (String item : pieces) {
      if (item.startsWith("\"")) {
        item = item.substring(1);
      }
      if (item.endsWith("\"")) {
        item = item.substring(0, item.length() - 1);
      }
      if (item.endsWith(File.separator)) {
        item = item.substring(0, item.length() - File.separator.length());
      }
      File directory = new File(item);
      if (!directory.exists()) {
        continue;
      }
      if (item.trim().length() == 0) {
        continue;
      }
      legit[legitCount++] = item;
    }
    legit = PApplet.subset(legit, 0, legitCount);
    String newPath = PApplet.join(legit, File.pathSeparator);
    if (!newPath.equals(path)) {
      System.setProperty("java.library.path", newPath);
    }
  }


  // looking for Documents and Settings/blah/Application Data/Processing
  public File getSettingsFolder() throws Exception {
    String appData = getAppDataPath();
    if (appData != null) {
      return new File(appData, APP_NAME);
    }
    throw new IOException("Could not get the Application Data folder");
  }


  static private String getAppDataPath() throws Exception {
    // Trying to deal with JNA problems on Windows 10
    // https://github.com/processing/processing/issues/3800
    // Try to load JNA and set its temporary directory
    getLibC();

    // HKEY_CURRENT_USER\Software\Microsoft
    //   \Windows\CurrentVersion\Explorer\Shell Folders
    // Value Name: AppData
    // Value Type: REG_SZ
    // Value Data: path

    //String keyPath =
    //  "Software\\Microsoft\\Windows\\CurrentVersion" +
    // "\\Explorer\\Shell Folders";
    //String appDataPath =
    //  Registry.getStringValue(REGISTRY_ROOT_KEY.CURRENT_USER, keyPath, "AppData");

    // Fix for Issue 410
    // Java 1.6 doesn't provide a good workaround (http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6519127)
    // Using JNA and SHGetFolderPath instead.

    // this will be contain the path if SHGetFolderPath is successful
    char[] pszPath = new char[WinDef.MAX_PATH];
    HRESULT hResult =
      Shell32.INSTANCE.SHGetFolderPath(null, ShlObj.CSIDL_APPDATA,
                                       null, ShlObj.SHGFP_TYPE_CURRENT,
                                       pszPath);

    if (!hResult.equals(WinError.S_OK)) {
      System.err.println(Kernel32Util.formatMessageFromHR(hResult));
      throw new Exception("Problem city, population: your computer.");
    }

    String appDataPath = new String(pszPath);
    int len = appDataPath.indexOf("\0");
//    appDataPath = appDataPath.substring(0, len);
//    return new File(appDataPath, "Processing");
    return appDataPath.substring(0, len);
  }


  // looking for Documents and Settings/blah/My Documents/Processing
  public File getDefaultSketchbookFolder() throws Exception {
    String documentsPath = getDocumentsPath();
    if (documentsPath != null) {
      return new File(documentsPath, APP_NAME);
    }
    return null;
  }


  static private String getDocumentsPath() throws Exception {
    // http://support.microsoft.com/?kbid=221837&sd=RMVP
    // http://support.microsoft.com/kb/242557/en-us

    // The path to the My Documents folder is stored in the following
    // registry key, where path is the complete path to your storage location

    // HKEY_CURRENT_USER\Software\Microsoft\Windows\CurrentVersion\Explorer\Shell Folders
    // Value Name: Personal
    // Value Type: REG_SZ
    // Value Data: path

    // in some instances, this may be overridden by a policy, in which case check:
    // HKEY_CURRENT_USER\Software\Microsoft\Windows\CurrentVersion\Explorer\User Shell Folders

    //String keyPath =
    //  "Software\\Microsoft\\Windows\\CurrentVersion" +
    //  "\\Explorer\\Shell Folders";
    //String personalPath =
    //  Registry.getStringValue(REGISTRY_ROOT_KEY.CURRENT_USER, keyPath, "Personal");

    // "The "Shell Folders" key exists solely to permit four programs written
    //  in 1994 to continue running on the RTM version of Windows 95." -- Raymond Chen, MSDN

    char[] pszPath = new char[WinDef.MAX_PATH]; // this will be contain the path if SHGetFolderPath is successful
    HRESULT hResult = Shell32.INSTANCE.SHGetFolderPath(null, ShlObj.CSIDL_PERSONAL, null, ShlObj.SHGFP_TYPE_CURRENT, pszPath);

    if (!hResult.equals(WinError.S_OK)) {
      System.err.println(Kernel32Util.formatMessageFromHR(hResult));
      throw new Exception("Problem city, population: your computer.");
    }

    String personalPath = new String(pszPath);
    int len = personalPath.indexOf("\0");
//    personalPath = personalPath.substring(0, len);
//    return new File(personalPath, "Processing");
    return personalPath.substring(0, len);
  }


//  @Override
//  public boolean deleteFile(File file) {
//    try {
//      moveToTrash(new File[] { file });
//    } catch (IOException e) {
//      e.printStackTrace();
//      Base.log("Could not move " + file.getAbsolutePath() + " to the trash.", e);
//      return false;
//    }
//    return true;
//  }


//  /**
//   * Move files/folders to the trash. If this file is on another file system
//   * or on a shared network directory, it will simply be deleted without any
//   * additional confirmation. Take that.
//   * <p>
//   * Based on JNA source for com.sun.jna.platform.win32.W32FileUtils
//   *
//   * @param files array of File objects to be removed
//   * @return true if no error codes returned
//   * @throws IOException if something bad happened along the way
//   */
//  static private boolean moveToTrash(File[] files) throws IOException {
//    Shell32 shell = Shell32.INSTANCE;
//    SHFILEOPSTRUCT fileop = new SHFILEOPSTRUCT();
//    fileop.wFunc = ShellAPI.FO_DELETE;
//    String[] paths = new String[files.length];
//    for (int i = 0; i < paths.length; i++) {
//      paths[i] = files[i].getAbsolutePath();
//      System.out.println(paths[i]);
//    }
//    fileop.pFrom = new WString(fileop.encodePaths(paths));
//    fileop.fFlags = ShellAPI.FOF_ALLOWUNDO | ShellAPI.FOF_NO_UI;
//    int ret = shell.SHFileOperation(fileop);
//    if (ret != 0) {
//      throw new IOException("Move to trash failed: " +
//                            fileop.pFrom + ": error code " + ret);
////        throw new IOException("Move to trash failed: " + fileop.pFrom + ": " +
////                              Kernel32Util.formatMessageFromLastErrorCode(ret));
//    }
//    if (fileop.fAnyOperationsAborted) {
//      throw new IOException("Move to trash aborted");
//    }
//    return true;
//  }


//  /**
//   * Ported from ShellAPI.h in the Microsoft Windows SDK 6.0A.
//   * Modified (bastardized) version from the JNA "platform" classes.
//   * @author dblock[at]dblock.org
//   */
//  public interface ShellAPI extends StdCallLibrary {
//
//    int STRUCTURE_ALIGNMENT = com.sun.jna.Platform.is64Bit() ?
//      Structure.ALIGN_DEFAULT : Structure.ALIGN_NONE;
//
//    int FO_MOVE = 0x0001;
//    int FO_COPY = 0x0002;
//    int FO_DELETE = 0x0003;
//    int FO_RENAME = 0x0004;
//
//    int FOF_MULTIDESTFILES = 0x0001;
//    int FOF_CONFIRMMOUSE = 0x0002;
//    int FOF_SILENT = 0x0004; // don't display progress UI (confirm prompts may be displayed still)
//    int FOF_RENAMEONCOLLISION = 0x0008; // automatically rename the source files to avoid the collisions
//    int FOF_NOCONFIRMATION = 0x0010; // don't display confirmation UI, assume "yes" for cases that can be bypassed, "no" for those that can not
//    int FOF_WANTMAPPINGHANDLE = 0x0020; // Fill in SHFILEOPSTRUCT.hNameMappings
//    int FOF_ALLOWUNDO = 0x0040; // enable undo including Recycle behavior for IFileOperation::Delete()
//    int FOF_FILESONLY = 0x0080; // only operate on the files (non folders), both files and folders are assumed without this
//    int FOF_SIMPLEPROGRESS = 0x0100; // means don't show names of files
//    int FOF_NOCONFIRMMKDIR = 0x0200; // don't dispplay confirmatino UI before making any needed directories, assume "Yes" in these cases
//    int FOF_NOERRORUI = 0x0400; // don't put up error UI, other UI may be displayed, progress, confirmations
//    int FOF_NOCOPYSECURITYATTRIBS = 0x0800; // dont copy file security attributes (ACLs)
//    int FOF_NORECURSION = 0x1000; // don't recurse into directories for operations that would recurse
//    int FOF_NO_CONNECTED_ELEMENTS = 0x2000; // don't operate on connected elements ("xxx_files" folders that go with .htm files)
//    int FOF_WANTNUKEWARNING = 0x4000; // during delete operation, warn if nuking instead of recycling (partially overrides FOF_NOCONFIRMATION)
//    int FOF_NORECURSEREPARSE = 0x8000; // deprecated; the operations engine always does the right thing on FolderLink objects (symlinks, reparse points, folder shortcuts)
//    int FOF_NO_UI = (FOF_SILENT | FOF_NOCONFIRMATION | FOF_NOERRORUI | FOF_NOCONFIRMMKDIR); // don't display any UI at all
//
//    int PO_DELETE = 0x0013; // printer is being deleted
//    int PO_RENAME = 0x0014; // printer is being renamed
//    int PO_PORTCHANGE = 0x0020; // port this printer connected to is being changed
//    int PO_REN_PORT = 0x0034; // PO_RENAME and PO_PORTCHANGE at same time.
//  }


  /*
  public void openURL(String url) throws Exception {
    // this is not guaranteed to work, because who knows if the
    // path will always be c:\progra~1 et al. also if the user has
    // a different browser set as their default (which would
    // include me) it'd be annoying to be dropped into ie.
    //Runtime.getRuntime().exec("c:\\progra~1\\intern~1\\iexplore "
    // + currentDir

    // the following uses a shell execute to launch the .html file
    // note that under cygwin, the .html files have to be chmodded +x
    // after they're unpacked from the zip file. i don't know why,
    // and don't understand what this does in terms of windows
    // permissions. without the chmod, the command prompt says
    // "Access is denied" in both cygwin and the "dos" prompt.
    //Runtime.getRuntime().exec("cmd /c " + currentDir + "\\reference\\" +
    //                    referenceFile + ".html");
    if (url.startsWith("http://")) {
      // open dos prompt, give it 'start' command, which will
      // open the url properly. start by itself won't work since
      // it appears to need cmd
      Runtime.getRuntime().exec("cmd /c start " + url);
    } else {
      // just launching the .html file via the shell works
      // but make sure to chmod +x the .html files first
      // also place quotes around it in case there's a space
      // in the user.dir part of the url
      Runtime.getRuntime().exec("cmd /c \"" + url + "\"");
    }
  }


  public boolean openFolderAvailable() {
    return true;
  }


  public void openFolder(File file) throws Exception {
    String folder = file.getAbsolutePath();

    // doesn't work
    //Runtime.getRuntime().exec("cmd /c \"" + folder + "\"");

    // works fine on winxp, prolly win2k as well
    Runtime.getRuntime().exec("explorer \"" + folder + "\"");

    // not tested
    //Runtime.getRuntime().exec("start explorer \"" + folder + "\"");
  }
  */


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  // getenv/setenv code partially thanks to Richard Quirk from:
  // http://quirkygba.blogspot.com/2009/11/setting-environment-variables-in-java.html

  static WinLibC clib;


  static WinLibC getLibC() {
    if (clib == null) {
      try {
        clib = (WinLibC) Native.loadLibrary("msvcrt", WinLibC.class);
      } catch (UnsatisfiedLinkError ule) {
        Messages.showTrace("JNA Error",
                           "JNA could not be loaded. Please report here:\n" +
                           "http://github.com/processing/processing/issues/new", ule, true);

        /*
        // Might be a problem with file encoding, use a default directory
        // https://github.com/processing/processing/issues/3624
        File ctmp = new File("C:\\TEMP");  // kick it old school
        if (ctmp.exists() || ctmp.mkdirs()) {
          try {
            File jnaTmp = File.createTempFile("processing", "jna", ctmp);
            if (jnaTmp.mkdirs()) {
              jnaTmp.deleteOnExit();  // clean up when we're done
              System.setProperty("jna.tmpdir", jnaTmp.getAbsolutePath());
              try {
                clib = (WinLibC) Native.loadLibrary("msvcrt", WinLibC.class);
              } catch (UnsatisfiedLinkError ulf) {
                Messages.showTrace("No luck with JNA",
                                   "After several attempts, JNA could not be loaded. Please report:\n" +
                                   "http://github.com/processing/processing/issues/new", ulf, true);
              }
            }
          } catch (IOException e) {
            Messages.showTrace("Could not create temp directory",
                               "JNA could not be loaded properly. Please report:\n" +
                               "http://github.com/processing/processing/issues/new", e, true);
          }
        } else {
          Messages.showError("Could not create temp directory",
                             "JNA could not be loaded into C:\\TEMP. Please report:\n" +
                             "http://github.com/processing/processing/issues/new", null);
        }
        */
      }
    }
    return clib;
  }


  public interface WinLibC extends Library {
    //WinLibC INSTANCE = (WinLibC) Native.loadLibrary("msvcrt", WinLibC.class);
    //libc = Native.loadLibrary("msvcrt", WinLibC.class);
    public int _putenv(String name);
  }


  public void setenv(String variable, String value) {
    //WinLibC clib = WinLibC.INSTANCE;
    getLibC()._putenv(variable + "=" + value);
  }


  public String getenv(String variable) {
    return System.getenv(variable);
  }


  public int unsetenv(String variable) {
    //WinLibC clib = WinLibC.INSTANCE;
    //clib._putenv(variable + "=");
    //return 0;
    return getLibC()._putenv(variable + "=");
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .

  // JNA code for using SHGetFolderPath to fix Issue 410
  // https://code.google.com/p/processing/issues/detail?id=410
  // Based on answer provided by McDowell at
  // http://stackoverflow.com/questions/585534/what-is-the-best-way-to-find-the-users-home-directory-in-java/586917#586917

//  private static Map<String, Object> OPTIONS = new HashMap<String, Object>();
//
//  static {
//    OPTIONS.put(Library.OPTION_TYPE_MAPPER, W32APITypeMapper.UNICODE);
//    OPTIONS.put(Library.OPTION_FUNCTION_MAPPER, W32APIFunctionMapper.UNICODE);
//  }
//
//
//  static class HANDLE extends PointerType implements NativeMapped {
//    public HANDLE() { }
//  }
//
//  static class HWND extends HANDLE { }
//
//
//  public interface Shell32 extends Library {
//
//    public static final int MAX_PATH = 260;
//    public static final int SHGFP_TYPE_CURRENT = 0;
//    public static final int SHGFP_TYPE_DEFAULT = 1;
//    public static final int S_OK = 0;
//
//    // KNOWNFOLDERIDs are preferred to CSDIL values
//    // but Windows XP only supports CSDIL so thats what we have to use
//    public static final int CSIDL_APPDATA = 0x001a; // "Application Data"
//    public static final int CSIDL_PERSONAL = 0x0005;      // "My Documents"
//
//    static Shell32 INSTANCE = (Shell32) Native.loadLibrary("shell32", Shell32.class, OPTIONS);
//
//    /**
//     * see http://msdn.microsoft.com/en-us/library/bb762181(VS.85).aspx
//     *
//     * HRESULT SHGetFolderPath( HWND hwndOwner, int nFolder, HANDLE hToken,
//     * DWORD dwFlags, LPTSTR pszPath);
//     */
//    public int SHGetFolderPath(HWND hwndOwner, int nFolder, HANDLE hToken,
//                               int dwFlags, char[] pszPath);
//
//    /**
//     * This function can be used to copy, move, rename,
//     * or delete a file system object.
//     * @param fileop Address of an SHFILEOPSTRUCT structure that contains
//     * information this function needs to carry out the specified operation.
//     * @return Returns zero if successful, or nonzero otherwise.
//     */
//    public int SHFileOperation(SHFILEOPSTRUCT fileop);
//  }
}
