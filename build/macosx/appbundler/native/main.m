/*
 * Copyright 2012, Oracle and/or its affiliates. All rights reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

#import <Cocoa/Cocoa.h>
#include <dlfcn.h>
#include <jni.h>
#include <pwd.h>

#define JAVA_LAUNCH_ERROR "JavaLaunchError"

#define JVM_RUNTIME_KEY "JVMRuntime"
#define WORKING_DIR "WorkingDirectory"
#define JVM_MAIN_CLASS_NAME_KEY "JVMMainClassName"
#define JVM_OPTIONS_KEY "JVMOptions"
#define JVM_DEFAULT_OPTIONS_KEY "JVMDefaultOptions"
#define JVM_ARGUMENTS_KEY "JVMArguments"
#define JVM_CLASSPATH_KEY "JVMClassPath"
#define JVM_VERSION_KEY "JVMVersion"
#define JRE_PREFERRED_KEY "JREPreferred"
#define JDK_PREFERRED_KEY "JDKPreferred"
#define JVM_DEBUG_KEY "JVMDebug"
#define IGNORE_PSN_KEY "IgnorePSN"

#define JVM_RUN_PRIVILEGED "JVMRunPrivileged"
#define JVM_RUN_JNLP "JVMJNLPLauncher"
#define JVM_RUN_JAR "JVMJARLauncher"


#define UNSPECIFIED_ERROR "An unknown error occurred."

#define APP_ROOT_PREFIX "$APP_ROOT"
#define JVM_RUNTIME "$JVM_RUNTIME"

#define JAVA_RUNTIME  "/Library/Internet Plug-Ins/JavaAppletPlugin.plugin/Contents/Home"
#define LIBJLI_DY_LIB "lib/jli/libjli.dylib"
#define DEPLOY_LIB    "lib/deploy.jar"

//*
    #define DLog(...) NSLog(@"%s %@", __PRETTY_FUNCTION__, [NSString stringWithFormat:__VA_ARGS__])
/*/
    #define DLog(...) do { } while (0)
//*/

typedef int (JNICALL *JLI_Launch_t)(int argc, char ** argv,
                                    int jargc, const char** jargv,
                                    int appclassc, const char** appclassv,
                                    const char* fullversion,
                                    const char* dotversion,
                                    const char* pname,
                                    const char* lname,
                                    jboolean javaargs,
                                    jboolean cpwildcard,
                                    jboolean javaw,
                                    jint ergo);

static char** progargv = NULL;
static int progargc = 0;
static int launchCount = 0;

const char * tmpFile();
int launch(char *, int, char **);

NSString * findJavaDylib (NSString *, bool, bool, bool, bool);
NSString * findJREDylib (int, bool, bool);
NSString * findJDKDylib (int, bool, bool);
int extractMajorVersion (NSString *);
NSString * convertRelativeFilePath(NSString *);
NSString * addDirectoryToSystemArguments(NSUInteger, NSSearchPathDomainMask, NSString *, NSMutableArray *);
void addModifierFlagToSystemArguments(NSEventModifierFlags, NSString *, NSEventModifierFlags, NSMutableArray *);

int main(int argc, char *argv[]) {
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];

    int result;
    @try {
    	if ((argc > 1) && (launchCount == 0)) {
    		progargc = argc - 1;
    		progargv = &argv[1];
    	}

        launch(argv[0], progargc, progargv);
        result = 0;
    } @catch (NSException *exception) {
        NSAlert *alert = [[NSAlert alloc] init];
        [alert setAlertStyle:NSCriticalAlertStyle];
        [alert setMessageText:[exception reason]];
        [alert runModal];

        result = 1;
    }

    [pool drain];

    return result;
}

int launch(char *commandName, int progargc, char *progargv[]) {

    // Preparation for jnlp launcher arguments
    const char *const_jargs = NULL;
    const char *const_appclasspath = NULL;

    // Get the main bundle
    NSBundle *mainBundle = [NSBundle mainBundle];

    // Get the main bundle's info dictionary
    NSDictionary *infoDictionary = [mainBundle infoDictionary];

    // Test for debugging (but only on the second runthrough)
    bool isDebugging = [[infoDictionary objectForKey:@JVM_DEBUG_KEY] boolValue];

    if (isDebugging) {
        DLog(@"\n\n\n\nLoading Application '%@'", [infoDictionary objectForKey:@"CFBundleName"]);
    }

    // Set the working directory based on config, defaulting to the user's home directory
    NSString *workingDir = [infoDictionary objectForKey:@WORKING_DIR];
    if (workingDir != nil) {
        workingDir = [workingDir stringByReplacingOccurrencesOfString:@APP_ROOT_PREFIX withString:[mainBundle bundlePath]];
    } else {
        workingDir = [[NSFileManager defaultManager] currentDirectoryPath];
        workingDir = NSHomeDirectory(); // REVIEW: Check which if these ones is realy the users home directory ...
    }
    if (isDebugging) {
    	DLog(@"Working Directory: '%@'", convertRelativeFilePath(workingDir));
    }

    chdir([workingDir UTF8String]);

    // execute privileged
    NSString *privileged = [infoDictionary objectForKey:@JVM_RUN_PRIVILEGED];
    if ( privileged != nil && getuid() != 0 ) {
        NSDictionary *error = [NSDictionary new];

        NSString *script =  [NSString stringWithFormat:@"do shell script \"\\\"%@\\\" > /dev/null 2>&1 &\" with administrator privileges", [NSString stringWithCString:commandName encoding:NSASCIIStringEncoding]];

        if (isDebugging) {
            DLog(@"script: %@", script);
        }

        NSAppleScript *appleScript = [[NSAppleScript new] initWithSource:script];
        if ([appleScript executeAndReturnError:&error]) {
            // This means we successfully elevated the application and can stop in here.
            return 0;
        }
    }

    // Locate the JLI_Launch() function
    NSString *runtime = [infoDictionary objectForKey:@JVM_RUNTIME_KEY];
    NSString *runtimePath = [[mainBundle builtInPlugInsPath] stringByAppendingPathComponent:runtime];

    NSString *jvmRequired = [infoDictionary objectForKey:@JVM_VERSION_KEY];
    bool exactVersionMatch = false;
    bool jrePreferred = [[infoDictionary objectForKey:@JRE_PREFERRED_KEY] boolValue];
    bool jdkPreferred = [[infoDictionary objectForKey:@JDK_PREFERRED_KEY] boolValue];

    if (jrePreferred && jdkPreferred) {
        DLog(@"Specifying both JRE- and JDK-preferred means neither is preferred");
        jrePreferred = false;
        jdkPreferred = false;
    }

    // check for jnlp launcher name
    // This basically circumvents the security problems introduced with 10.8.4 that JNLP Files must be signed to execute them without CTRL+CLick -> Open
    // See: How to sign (dynamic) JNLP files for OSX 10.8.4 and Gatekeeper http://stackoverflow.com/questions/16958130/how-to-sign-dynamic-jnlp-files-for-osx-10-8-4-and-gatekeeper
    // There is no solution to properly sign a dynamic jnlp file to date. Both Apple and Oracle have open rdars/tickets on this.
    // The following mechanism encapsulates a JNLP file/template. It makes a temporary copy when executing. This ensures that the JNLP file can be updates from the server at runtime.
    // YES, this may insert additional security threats, but it is still the only way to avoid permission problems.
    // It is highly recommended that the resulting .app container is being signed with a certificate from Apple - otherwise you will not need this mechanism.
    // Moved up here to check if we want to launch a JNLP. If so: make sure the version is below 9
    NSString *jnlplauncher = [infoDictionary objectForKey:@JVM_RUN_JNLP];
    if ( jnlplauncher != nil ) {
        int required = 8;
        if ( jvmRequired != nil ) {
            required = extractMajorVersion (jvmRequired);
            if (required > 8) { required = 8; }
        }

        exactVersionMatch = true;
        jvmRequired = [NSString stringWithFormat:@"1.%i", required];
        DLog(@"Will Require a JVM version '%i' due to JNLP restrictions", required);
    }

    NSString *javaDylib;

    // If a runtime is set, we really want it. If it is not there, we will fail later on.
    if (runtime != nil) {
        NSString *dylibRelPath = @"Contents/Home/jre";
        javaDylib = [[runtimePath stringByAppendingPathComponent:dylibRelPath] stringByAppendingPathComponent:@LIBJLI_DY_LIB];
        BOOL isDir;
        NSFileManager *fm = [[NSFileManager alloc] init];
        BOOL javaDylibFileExists = [fm fileExistsAtPath:javaDylib isDirectory:&isDir];
        if (!javaDylibFileExists || isDir) {
            dylibRelPath = @"Contents/Home";
            javaDylib = [[runtimePath stringByAppendingPathComponent:dylibRelPath] stringByAppendingPathComponent:@LIBJLI_DY_LIB];
            javaDylibFileExists = [fm fileExistsAtPath:javaDylib isDirectory:&isDir];
                if (!javaDylibFileExists || isDir) {
                    javaDylib = NULL;
                }
        }
        if (isDebugging) {
            DLog(@"Java Runtime (%@) Relative Path: '%@' (dylib: %@)", runtime, runtimePath, javaDylib);
        }
    }
    else {
        // Search for the runtimePath, then make it a libjli.dylib path.
        runtimePath = findJavaDylib (jvmRequired, jrePreferred, jdkPreferred, isDebugging, exactVersionMatch);
        javaDylib = [runtimePath stringByAppendingPathComponent:@LIBJLI_DY_LIB];

        if (isDebugging) {
            DLog(@"Java Runtime Dylib Path: '%@'", convertRelativeFilePath(javaDylib));
        }
    }

    const char *libjliPath = NULL;
    if (javaDylib != nil)
    {
        libjliPath = [javaDylib fileSystemRepresentation];
    }

    DLog(@"Launchpath: %s", libjliPath);

    void *libJLI = dlopen(libjliPath, RTLD_LAZY);

    JLI_Launch_t jli_LaunchFxnPtr = NULL;
    if (libJLI != NULL) {
        jli_LaunchFxnPtr = dlsym(libJLI, "JLI_Launch");
    }

    if (jli_LaunchFxnPtr == NULL) {
        NSString *msg;

        if (runtime == nil && jvmRequired != nil) {
            int required = extractMajorVersion (jvmRequired);

            if (required < 7) { required = 7; }

            if (jdkPreferred) {
                NSString *msga = NSLocalizedString(@"JDKxLoadFullError", @UNSPECIFIED_ERROR);
                msg = [NSString stringWithFormat:msga, required];
            }
            else {
                NSString *msga = NSLocalizedString(@"JRExLoadFullError", @UNSPECIFIED_ERROR);
                msg = [NSString stringWithFormat:msga, required];
            }
        }
        else {
            msg = NSLocalizedString(@"JRELoadError", @UNSPECIFIED_ERROR);
        }

        DLog(@"Error launching JVM Runtime (%@) Relative Path: '%@' (dylib: %@)\n  error: %@",
              runtime, runtimePath, javaDylib, msg);

        [[NSException exceptionWithName:@JAVA_LAUNCH_ERROR
                reason:msg userInfo:nil] raise];
    }

    // Set the class path
    NSFileManager *defaultFileManager = [NSFileManager defaultManager];
    NSString *mainBundlePath = [mainBundle bundlePath];

    // make sure the bundle path does not contain a colon, as that messes up the java.class.path,
    // because colons are used a path separators and cannot be escaped.

    // funny enough, Finder does not let you create folder with colons in their names,
    // but when you create a folder with a slash, e.g. "audio/video", it is accepted
    // and turned into... you guessed it, a colon:
    // "audio:video"
    if ([mainBundlePath rangeOfString:@":"].location != NSNotFound) {
        [[NSException exceptionWithName:@JAVA_LAUNCH_ERROR
                                 reason:NSLocalizedString(@"BundlePathContainsColon", @UNSPECIFIED_ERROR)
                               userInfo:nil] raise];
    }
    if (isDebugging) {
        NSLog(@"Main Bundle Path: '%@'", mainBundlePath);
    }

    // Set the class path
    NSString *javaPath = [mainBundlePath stringByAppendingString:@"/Contents/Java"];
    NSMutableArray *systemArguments = [[NSMutableArray alloc] init];
    NSMutableString *classPath = [NSMutableString stringWithString:@"-Djava.class.path="];

    // Set the library path
    NSString *libraryPath = [NSString stringWithFormat:@"-Djava.library.path=%@/Contents/MacOS", mainBundlePath];
    [systemArguments addObject:libraryPath];

    // Get the VM options
    NSMutableArray *options = [[infoDictionary objectForKey:@JVM_OPTIONS_KEY] mutableCopy];
    if (options == nil) {
        options = [NSMutableArray array];
    }

    // Get the VM default options
    NSArray *defaultOptions = [NSArray array];
    NSDictionary *defaultOptionsDict = [infoDictionary objectForKey:@JVM_DEFAULT_OPTIONS_KEY];
    if (defaultOptionsDict != nil) {
        NSMutableDictionary *defaults = [NSMutableDictionary dictionaryWithDictionary: defaultOptionsDict];
        // Replace default options with user specific options, if available
        NSUserDefaults *userDefaults = [NSUserDefaults standardUserDefaults];
        // Create special key that should be used by Java's java.util.Preferences impl
        // Requires us to use "/" + bundleIdentifier.replace('.', '/') + "/JVMOptions/" as node on the Java side
        // Beware: bundleIdentifiers shorter than 3 segments are placed in a different file!
        // See java/util/prefs/MacOSXPreferences.java of OpenJDK for details
        NSString *bundleDictionaryKey = [mainBundle bundleIdentifier];
        bundleDictionaryKey = [bundleDictionaryKey stringByReplacingOccurrencesOfString:@"." withString:@"/"];
        bundleDictionaryKey = [NSString stringWithFormat: @"/%@/", bundleDictionaryKey];

        NSDictionary *bundleDictionary = [userDefaults dictionaryForKey: bundleDictionaryKey];
        if (bundleDictionary != nil) {
            NSDictionary *jvmOptionsDictionary = [bundleDictionary objectForKey: @"JVMOptions/"];
            for (NSString *key in jvmOptionsDictionary) {
                NSString *value = [jvmOptionsDictionary objectForKey:key];
                [defaults setObject: value forKey: key];
            }
        }
        defaultOptions = [defaults allValues];
    }

    // Get the application arguments
    NSMutableArray *arguments = [[infoDictionary objectForKey:@JVM_ARGUMENTS_KEY] mutableCopy];
    if (arguments == nil) {
        arguments = [NSMutableArray array];
    }

    // Check for a defined JAR File below the Contents/Java folder
    // If set, use this instead of a classpath setting
    NSString *jarlauncher = [infoDictionary objectForKey:@JVM_RUN_JAR];

    // Get the main class name
    NSString *mainClassName = [infoDictionary objectForKey:@JVM_MAIN_CLASS_NAME_KEY];

    if ( jnlplauncher != nil ) {

        const_appclasspath = [[runtimePath stringByAppendingPathComponent:@DEPLOY_LIB] fileSystemRepresentation];

        // JNLP Launcher found, need to modify quite a bit now
        [options addObject:@"-classpath"];
        [options addObject:[NSString stringWithFormat:@"%s", const_appclasspath]];

        // unset the original classpath
        classPath = nil;

        // Main Class is javaws
        mainClassName=@"com.sun.javaws.Main";

        // Optional stuff that javaws would do as well
        [options addObject:@"-Dsun.awt.warmup=true"];
        [options addObject:@"-Xverify:remote"];
        [options addObject:@"-Djnlpx.remove=true"];
        [options addObject:@"-DtrustProxy=true"];

        [options addObject:[NSString stringWithFormat:@"-Djava.security.policy=file:%@/lib/security/javaws.policy", runtimePath]];
        [options addObject:[NSString stringWithFormat:@"-Xbootclasspath/a:%@/lib/javaws.jar:%@/lib/deploy.jar:%@/lib/plugin.jar", runtimePath, runtimePath, runtimePath]];

        // Argument that javaws does also
        // [arguments addObject:@"-noWebStart"];

        // Copy the jnlp to a temporary location
        NSError *copyerror = nil;
        NSString *tempFileName = [NSString stringWithCString:tmpFile() encoding:NSASCIIStringEncoding];
        // File now exists.
        [defaultFileManager removeItemAtPath:tempFileName error:NULL];

        // Check if this is absolute or relative (else)
        NSString *jnlpPath = [mainBundlePath stringByAppendingPathComponent:jnlplauncher];
        if ( ![defaultFileManager fileExistsAtPath:jnlpPath] ) {
            jnlpPath = [javaPath stringByAppendingPathComponent:jnlplauncher];
        }

        [defaultFileManager copyItemAtURL:[NSURL fileURLWithPath:jnlpPath] toURL:[NSURL fileURLWithPath:tempFileName] error:&copyerror];
        if ( copyerror != nil ) {
            NSLog(@"Error: %@", copyerror);
            [[NSException exceptionWithName:@"Error while copying JNLP File"
                                     reason:@"File copy error"
                                   userInfo:copyerror.userInfo] raise];
        }

        // Add the jnlp as argument so that javaws.Main can read and delete it
        [arguments addObject:tempFileName];

    } else
    // Either mainClassName or jarLauncher has to be set since this is not a jnlpLauncher
    if ( mainClassName == nil && jarlauncher == nil ) {
        [[NSException exceptionWithName:@JAVA_LAUNCH_ERROR
            reason:NSLocalizedString(@"MainClassNameRequired", @UNSPECIFIED_ERROR)
            userInfo:nil] raise];
    }

    if (isDebugging) {
        DLog(@"Main Class Name: '%@'", mainClassName);
    }

    // If a jar file is defined as launcher, disacard the javaPath
    if ( jarlauncher != nil ) {
        [classPath appendFormat:@":%@/%@", javaPath, jarlauncher];
    } else {

        NSArray *cp = [infoDictionary objectForKey:@JVM_CLASSPATH_KEY];
        if (cp == nil) {

            // Implicit classpath, so use the contents of the "Java" folder to build an explicit classpath

            [classPath appendFormat:@"%@/Classes", javaPath];
            NSFileManager *defaultFileManager = [NSFileManager defaultManager];
            NSArray *javaDirectoryContents = [defaultFileManager contentsOfDirectoryAtPath:javaPath error:nil];
            if (javaDirectoryContents == nil) {
                [[NSException exceptionWithName:@JAVA_LAUNCH_ERROR
                                         reason:NSLocalizedString(@"JavaDirectoryNotFound", @UNSPECIFIED_ERROR)
                                       userInfo:nil] raise];
            }

            for (NSString *file in javaDirectoryContents) {
                if ([file hasSuffix:@".jar"]) {
                    [classPath appendFormat:@":%@/%@", javaPath, file];
                }
            }

        } else {

            // Explicit ClassPath

            int k = 0;
            for (NSString *file in cp) {
                if (k++ > 0) [classPath appendString:@":"]; // add separator if needed
                file = [file stringByReplacingOccurrencesOfString:@APP_ROOT_PREFIX withString:[mainBundle bundlePath]];
                [classPath appendString:file];
            }
        }
    }

    if ( classPath != nil ) {
        [systemArguments addObject:classPath];
    }

    // Set OSX special folders
    NSString * libraryDirectory = addDirectoryToSystemArguments(NSLibraryDirectory, NSUserDomainMask, @"LibraryDirectory", systemArguments);
    addDirectoryToSystemArguments(NSDocumentDirectory, NSUserDomainMask, @"DocumentsDirectory", systemArguments);
    addDirectoryToSystemArguments(NSApplicationSupportDirectory, NSUserDomainMask, @"ApplicationSupportDirectory", systemArguments);
    addDirectoryToSystemArguments(NSCachesDirectory, NSUserDomainMask, @"CachesDirectory", systemArguments);
    addDirectoryToSystemArguments(NSApplicationDirectory, NSUserDomainMask, @"ApplicationDirectory", systemArguments);
    addDirectoryToSystemArguments(NSAutosavedInformationDirectory, NSUserDomainMask, @"AutosavedInformationDirectory", systemArguments);
    addDirectoryToSystemArguments(NSDesktopDirectory, NSUserDomainMask, @"DesktopDirectory", systemArguments);
    addDirectoryToSystemArguments(NSDownloadsDirectory, NSUserDomainMask, @"DownloadsDirectory", systemArguments);
    addDirectoryToSystemArguments(NSMoviesDirectory, NSUserDomainMask, @"MoviesDirectory", systemArguments);
    addDirectoryToSystemArguments(NSMusicDirectory, NSUserDomainMask, @"MusicDirectory", systemArguments);
    addDirectoryToSystemArguments(NSPicturesDirectory, NSUserDomainMask, @"PicturesDirectory", systemArguments);
    addDirectoryToSystemArguments(NSSharedPublicDirectory, NSUserDomainMask, @"SharedPublicDirectory", systemArguments);

    addDirectoryToSystemArguments(NSLibraryDirectory, NSLocalDomainMask, @"SystemLibraryDirectory", systemArguments);
    addDirectoryToSystemArguments(NSApplicationSupportDirectory, NSLocalDomainMask, @"SystemApplicationSupportDirectory", systemArguments);
    addDirectoryToSystemArguments(NSCachesDirectory, NSLocalDomainMask, @"SystemCachesDirectory", systemArguments);
    addDirectoryToSystemArguments(NSApplicationDirectory, NSLocalDomainMask, @"SystemApplicationDirectory", systemArguments);
    addDirectoryToSystemArguments(NSUserDirectory, NSLocalDomainMask, @"SystemUserDirectory", systemArguments);

    // get the user's home directory, independent of the sandbox container
    int bufsize;
    if ((bufsize = sysconf(_SC_GETPW_R_SIZE_MAX)) != -1) {
        char buffer[bufsize];
        struct passwd pwd, *result = NULL;
        if (getpwuid_r(getuid(), &pwd, buffer, bufsize, &result) == 0 && result) {
            [systemArguments addObject:[NSString stringWithFormat:@"-DUserHome=%s", pwd.pw_dir]];
        }
    }

    //Sandbox
    NSString *containersDirectory = [libraryDirectory stringByAppendingPathComponent:@"Containers"];
    NSString *sandboxEnabled = @"false";
    BOOL isDir;
    NSFileManager *fm = [[NSFileManager alloc] init];
    BOOL containersDirExists = [fm fileExistsAtPath:containersDirectory isDirectory:&isDir];
    if (containersDirExists && isDir) {
        sandboxEnabled = @"true";
    }
    NSString *sandboxEnabledVar = [NSString stringWithFormat:@"-DSandboxEnabled=%@", sandboxEnabled];
    [systemArguments addObject:sandboxEnabledVar];


    // Mojave Dark Mode enabled?
    NSString *osxMode = [[NSUserDefaults standardUserDefaults] stringForKey:@"AppleInterfaceStyle"];
    BOOL isDarkMode = (osxMode != nil && [osxMode isEqualToString:@"Dark"]);

    NSString *darkModeEnabledVar = [NSString stringWithFormat:@"-DDarkMode=%s",
                                                    (isDarkMode ? "true" : "false")];
    [systemArguments addObject:darkModeEnabledVar];

	// Check for modifier keys on app launch

	// Since [NSEvent modifierFlags] is only available since OS X 10.6., only add properties if supported.
	if ([NSEvent respondsToSelector:@selector(modifierFlags)]) {
		NSEventModifierFlags launchModifierFlags = [NSEvent modifierFlags] & NSEventModifierFlagDeviceIndependentFlagsMask;

		[systemArguments addObject:[NSString stringWithFormat:@"-DLaunchModifierFlags=%lu", (unsigned long)launchModifierFlags]];

		addModifierFlagToSystemArguments(NSEventModifierFlagCapsLock, @"LaunchModifierFlagCapsLock", launchModifierFlags, systemArguments);
		addModifierFlagToSystemArguments(NSEventModifierFlagShift, @"LaunchModifierFlagShift", launchModifierFlags, systemArguments);
		addModifierFlagToSystemArguments(NSEventModifierFlagControl, @"LaunchModifierFlagControl", launchModifierFlags, systemArguments);
		addModifierFlagToSystemArguments(NSEventModifierFlagOption, @"LaunchModifierFlagOption", launchModifierFlags, systemArguments);
		addModifierFlagToSystemArguments(NSEventModifierFlagCommand, @"LaunchModifierFlagCommand", launchModifierFlags, systemArguments);
		addModifierFlagToSystemArguments(NSEventModifierFlagNumericPad, @"LaunchModifierFlagNumericPad", launchModifierFlags, systemArguments);
		addModifierFlagToSystemArguments(NSEventModifierFlagHelp, @"LaunchModifierFlagHelp", launchModifierFlags, systemArguments);
		addModifierFlagToSystemArguments(NSEventModifierFlagFunction, @"LaunchModifierFlagFunction", launchModifierFlags, systemArguments);
	}



    // Remove -psn argument
    int newProgargc = progargc;
    char *newProgargv[newProgargc];
    for (int i = 0; i < progargc; i++) {
        newProgargv[i] = progargv[i];
    }

    bool ignorePSN = [[infoDictionary objectForKey:@IGNORE_PSN_KEY] boolValue];
    if (ignorePSN) {
        NSString *psnRegexp = @"^-psn_\\d_\\d+$";
        NSPredicate *psnTest = [NSPredicate predicateWithFormat:@"SELF MATCHES %@", psnRegexp];

        int shift = 0;
        int i = 0;
        while (i < newProgargc) {
            NSString *s = [NSString stringWithFormat:@"%s", newProgargv[i]];
            if ([psnTest evaluateWithObject: s]){
                shift++;
                newProgargc--;
            }
            newProgargv[i] = newProgargv[i+shift];
            i++;
        }
    }

    // replace $APP_ROOT in environment variables
    NSDictionary* environment = [[NSProcessInfo processInfo] environment];
    for (NSString* key in environment) {
        NSString* value = [environment objectForKey:key];
        NSString* newValue = [value stringByReplacingOccurrencesOfString:@APP_ROOT_PREFIX withString:[mainBundle bundlePath]];
        if (! [newValue isEqualToString:value]) {
            setenv([key UTF8String], [newValue UTF8String], 1);
        }
    }

    // Initialize the arguments to JLI_Launch()
    // +5 due to the special directories and the sandbox enabled property
    int argc = 1 + [systemArguments count] + [options count] + [defaultOptions count] + 1 + [arguments count] + newProgargc;
    char *argv[argc];

    int i = 0;
    argv[i++] = commandName;
    for (NSString *systemArgument in systemArguments) {
    	argv[i++] = strdup([systemArgument UTF8String]);
    }

    for (NSString *option in options) {
        option = [option stringByReplacingOccurrencesOfString:@APP_ROOT_PREFIX withString:[mainBundle bundlePath]];
        option = [option stringByReplacingOccurrencesOfString:@JVM_RUNTIME withString:runtimePath];
        argv[i++] = strdup([option UTF8String]);
        if (isDebugging) { DLog(@"Option: %@",option); }
    }

    for (NSString *defaultOption in defaultOptions) {
        defaultOption = [defaultOption stringByReplacingOccurrencesOfString:@APP_ROOT_PREFIX withString:[mainBundle bundlePath]];
        argv[i++] = strdup([defaultOption UTF8String]);
        if (isDebugging) { DLog(@"DefaultOption: %@",defaultOption); }
    }

    argv[i++] = strdup([mainClassName UTF8String]);

    for (NSString *argument in arguments) {
        argument = [argument stringByReplacingOccurrencesOfString:@APP_ROOT_PREFIX withString:[mainBundle bundlePath]];
        argv[i++] = strdup([argument UTF8String]);
    }

	int ctr = 0;
	for (ctr = 0; ctr < newProgargc; ctr++) {
		argv[i++] = newProgargv[ctr];
	}

    // Print the full command line for debugging purposes...
    if (isDebugging) {
        DLog(@"Command line passed to application:");
        int j=0;
        for(j=0; j<i; j++) {
            DLog(@"Arg %d: '%s'", j, argv[j]);
        }
    }

	launchCount++;

    // Invoke JLI_Launch()
    return jli_LaunchFxnPtr(argc, argv,
                            sizeof(&const_jargs) / sizeof(char *), &const_jargs,
                            sizeof(&const_appclasspath) / sizeof(char *), &const_appclasspath,
                            "",
                            "",
                            "java",
                            "java",
                            (const_jargs != NULL) ? JNI_TRUE : JNI_FALSE,
                            FALSE,
                            FALSE,
                            0);
}

/*
 * Convenient Method to create a temporary JNLP file(name)
 * This file will be deleted by the JLI_Launch when the program ends.
 */
const char * tmpFile() {
    NSString *tempFileTemplate = [NSTemporaryDirectory()
                                  stringByAppendingPathComponent:@"jnlpFile.XXXXXX.jnlp"];

    const char *tempFileTemplateCString = [tempFileTemplate fileSystemRepresentation];

    char *tempFileNameCString = (char *)malloc(strlen(tempFileTemplateCString) + 1);
    strcpy(tempFileNameCString, tempFileTemplateCString);
    int fileDescriptor = mkstemps(tempFileNameCString, 5);

    // no need to keep it open
    close(fileDescriptor);

    if (fileDescriptor == -1) {
        DLog(@"Error while creating tmp file");
        return nil;
    }

    NSString *tempFileName = [[NSFileManager defaultManager]
                              stringWithFileSystemRepresentation:tempFileNameCString
                              length:strlen(tempFileNameCString)];

    free(tempFileNameCString);

    return [tempFileName fileSystemRepresentation];
}

/**
 *  Searches for a JRE or JDK dylib of the specified version or later.
 *  First checks the "usual" JRE location, and failing that looks for a JDK.
 *  The version required should be a string of form "1.X". If no version is
 *  specified or the version is pre-1.7, then a Java 1.7 is sought.
 */
NSString * findJavaDylib (
        NSString *jvmRequired,
        bool jrePreferred,
        bool jdkPreferred,
        bool isDebugging,
        bool exactMatch)
{
    DLog (@"Searching for a JRE.");
    int required = extractMajorVersion(jvmRequired);

    if (required < 7)
    {
        if (isDebugging) { DLog (@"Required JVM must be at least ver. 7."); }
        required = 7;
    }

    if (isDebugging) {
        DLog (@"Searching for a Java %d", required);
    }

    //  First, if a JRE is acceptible, try to find one with required Java version.
    //  If found, return address for dylib that should be in the JRE package.
    if (jdkPreferred) {
        if (isDebugging) {
            DLog (@"A JDK is preferred; will not search for a JRE.");
        }
    }
    else {
        NSString * dylib = findJREDylib (required, isDebugging, exactMatch);

        if (dylib != nil) { return dylib; }

        if (isDebugging) { DLog (@"No matching JRE found."); }
    }

    // If JRE not found or if JDK preferred, look for an acceptable JDK
    // (probably in /Library/Java/JavaVirtualMachines if so). If found,
    // return return address of dylib in the JRE within the JDK.
    if (jrePreferred) {
        if (isDebugging) {
            DLog (@"A JRE is preferred; will not search for a JDK.");
        }
    }
    else {
        NSString * dylib = findJDKDylib (required, isDebugging, exactMatch);

        if (dylib != nil) { return dylib; }

        if (isDebugging) { DLog (@"No matching JDK found."); }
    }

    DLog (@"No matching JRE or JDK found.");

    return nil;
}

/**
 *  Searches for a JRE dylib of the specified version or later.
 */
NSString * findJREDylib (
        int jvmRequired,
        bool isDebugging,
        bool exactMatch)
{
    // Try the "java -version" shell command and see if we get a response and
    // if so whether the version  is acceptable.
    // If found, return address for dylib that should be in the JRE package.
    // Note that for unknown but ancient reasons, the result is output to stderr
    // rather than to stdout.
    @try
    {
        NSTask *task = [[NSTask alloc] init];
        [task setLaunchPath:[@JAVA_RUNTIME stringByAppendingPathComponent:@"bin/java"]];

        NSArray *args = [NSArray arrayWithObjects: @"-version", nil];
        [task setArguments:args];

        NSPipe *stdout = [NSPipe pipe];
        [task setStandardOutput:stdout];

        NSPipe *stderr = [NSPipe pipe];
        [task setStandardError:stderr];

        [task setStandardInput:[NSPipe pipe]];

        NSFileHandle *outHandle = [stdout fileHandleForReading];
        NSFileHandle *errHandle = [stderr fileHandleForReading];

        [task launch];
        [task waitUntilExit];
        [task release];

        NSData *data1 = [outHandle readDataToEndOfFile];
        NSData *data2 = [errHandle readDataToEndOfFile];

        NSString *outRead = [[NSString alloc] initWithData:data1
                                                  encoding:NSUTF8StringEncoding];
        NSString *errRead = [[NSString alloc] initWithData:data2
                                                  encoding:NSUTF8StringEncoding];

    //  Found something in errRead. Parse it for a Java version string and
    //  try to extract a major version number.
        if (errRead != nil) {
            int version = 0;

            // The result of the version command is 'java version "1.x"' or 'java version "9"'
            NSRange vrange = [errRead rangeOfString:@"java version \""];

            if (vrange.location != NSNotFound) {
                NSString *vstring = [errRead substringFromIndex:(vrange.location + 14)];

                vrange  = [vstring rangeOfString:@"\""];
                vstring = [vstring substringToIndex:vrange.location];

                version = extractMajorVersion(vstring);

                if (isDebugging) {
                    DLog (@"Found a Java %@ JRE", vstring);
                    DLog (@"Looks like major version %d", extractMajorVersion(vstring));
                }
            }

            if ( (version >= jvmRequired && !exactMatch) || (version == jvmRequired && exactMatch) ) {
                if (isDebugging) {
                    DLog (@"JRE version qualifies");
                }
                return @JAVA_RUNTIME;
            }
        }
    }
    @catch (NSException *exception)
    {
        DLog (@"JRE search exception: '%@'", [exception reason]);
    }

    return nil;
}

//  Having failed to find a JRE in the usual location, see if a JDK is installed
//  (probably in /Library/Java/JavaVirtualMachines). If so, return address of
//  dylib in the JRE within the JDK.
/**
 *  Searches for a JDK dylib of the specified version or later.
 */
NSString * findJDKDylib (
        int jvmRequired,
        bool isDebugging,
        bool exactMatch)
{
    @try
    {
        NSTask *task = [[NSTask alloc] init];
        [task setLaunchPath:@"/usr/libexec/java_home"];

        NSArray *args = [NSArray arrayWithObjects: @"-v", [NSString stringWithFormat:@"1.%i%@", jvmRequired, exactMatch?@"":@"+"], nil];
        [task setArguments:args];

        NSPipe *stdout = [NSPipe pipe];
        [task setStandardOutput:stdout];

        NSPipe *stderr = [NSPipe pipe];
        [task setStandardError:stderr];

        [task setStandardInput:[NSPipe pipe]];

        NSFileHandle *outHandle = [stdout fileHandleForReading];
        NSFileHandle *errHandle = [stderr fileHandleForReading];

        [task launch];
        [task waitUntilExit];
        [task release];

        NSData *data1 = [outHandle readDataToEndOfFile];
        NSData *data2 = [errHandle readDataToEndOfFile];

        NSString *outRead = [[NSString alloc] initWithData:data1
                                                    encoding:NSUTF8StringEncoding];
        NSString *errRead = [[NSString alloc] initWithData:data2
                                                    encoding:NSUTF8StringEncoding];

    //  If matching JDK not found, outRead will include something like
    //  "Unable to find any JVMs matching version "1.X"."
        if ( errRead != nil
                && [errRead rangeOfString:@"Unable"].location != NSNotFound )
        {
            if (isDebugging) {  DLog (@"No matching JDK found."); }
            return nil;
        }

        int version = 0;

        NSRange vrange = [outRead rangeOfString:@"jdk1."];
        if (vrange.location == NSNotFound) {
            // try the changed version layout from version 9
            vrange = [outRead rangeOfString:@"jdk-"];
            vrange.location += 4;
        } else {
            // otherwise remove the leading jdk
            vrange.location += 3;
        }

        if (vrange.location != NSNotFound) {
            NSString *vstring = [outRead substringFromIndex:(vrange.location)];

            vrange  = [vstring rangeOfString:@"/"];
            vstring = [vstring substringToIndex:vrange.location];

            version = extractMajorVersion(vstring);

            if (isDebugging) {
                DLog (@"Found a Java %@ JDK", vstring);
                DLog (@"Looks like major version %d", extractMajorVersion(vstring));
            }
        }

        if ( (version >= jvmRequired && !exactMatch) || (version == jvmRequired && exactMatch) ) {
            if (isDebugging) {
                DLog (@"JDK version qualifies");
            }

            NSString *outread2 = [outRead stringByTrimmingCharactersInSet:[NSCharacterSet
                                                            whitespaceAndNewlineCharacterSet]];

        //  Return location where LIBJLI_DY_LIB is located. Note that the path was
        //  shortemed between JDK 8 and JDK 9.
            if (version > 8) {
                return outread2;
            }
            else {
                return [outread2 stringByAppendingPathComponent:@"jre"];
            }
        }
    }
    @catch (NSException *exception)
    {
        DLog (@"JDK search exception: '%@'", [exception reason]);
    }

    return nil;
}


/**
 *  Extract the Java major version number from a string. We expect the input
 *  to look like either either "1.X", "1.X.Y_ZZ" or "X.Y.ZZ", and the
 *  returned result will be the integral value of X. Any failure to parse the
 *  string will return 0.
 */
int extractMajorVersion (NSString *vstring) {
  if (vstring == nil) { return 0; }

  //  Expecting either a java version of form 1.X, 1.X.Y_ZZ or jdk1.X.Y_ZZ.
  //  Strip everything from start and ending that aren't part of the version number
  NSCharacterSet* nonDigits = [[NSCharacterSet characterSetWithCharactersInString:@"0123456789."] invertedSet];
  vstring = [vstring stringByTrimmingCharactersInSet:nonDigits];

  if([vstring hasPrefix:@"1."]) {  // this is the version < 9 layout. Remove the leading "1."
    vstring = [vstring substringFromIndex:2];
  }

  // the next integer token should be the major version, so read everything up to the first decimal point, if any
  NSUInteger versionEndLoc = [vstring rangeOfString:@"."].location;
  if (versionEndLoc != NSNotFound) {
    vstring = [vstring substringToIndex:versionEndLoc];
  }

  return [vstring intValue];
}


NSString * convertRelativeFilePath(NSString * path) {
    return [path stringByStandardizingPath];
}

NSString * addDirectoryToSystemArguments(NSUInteger searchPath, NSSearchPathDomainMask domainMask,
		NSString *systemProperty, NSMutableArray *systemArguments) {
	NSArray *paths = NSSearchPathForDirectoriesInDomains(searchPath,domainMask, YES);
    if ([paths count] > 0) {
		NSString *basePath = [paths objectAtIndex:0];
		NSString *directory = [NSString stringWithFormat:@"-D%@=%@", systemProperty, basePath];
		[systemArguments addObject:directory];
		return basePath;
    }
    return nil;
}

void addModifierFlagToSystemArguments(NSEventModifierFlags mask, NSString *systemProperty, NSEventModifierFlags modifierFlags, NSMutableArray *systemArguments) {
	NSString *modifierFlagValue = (modifierFlags & mask) ? @"true" : @"false";
	NSString *modifierFlagVar = [NSString stringWithFormat:@"-D%@=%@", systemProperty, modifierFlagValue];
	[systemArguments addObject:modifierFlagVar];
}
