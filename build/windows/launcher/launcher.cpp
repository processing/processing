// -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*-

// if this code looks shitty, that's because it is. people are likely to have
// the durndest things in their CLASSPATH and QTJAVA environment variables. 
// mostly because installers often mangle them without the user knowing. 
// so who knows where and when the quotes will show up. the code below is 
// based on a couple years of trial and error with processing releases.

// The size of all of the strings was made sort of ambiguously large, since
// 1) nothing is hurt by allocating an extra few bytes temporarily and
// 2) if the user has a long path, and it gets copied five times over for the
// classpath, the program runs the risk of crashing. Bad bad.

#define ARGS_FILE_PATH "\\lib\\args.txt"

#include <windows.h>
#include <stdio.h>
#include <stdlib.h>


void removeLineEndings(char *what);

int STDCALL
WinMain (HINSTANCE hInst, HINSTANCE hPrev, LPSTR lpCmd, int nShow)
{
  // all these malloc statements... things may need to be larger.

  // what was passed to this application
  char *incoming_cmdline = (char *)malloc(strlen(lpCmd) * sizeof(char));
  strcpy (incoming_cmdline, lpCmd);

  // append the classpath and launcher.Application
  char *loaddir = (char *)malloc(MAX_PATH * sizeof(char));
  *loaddir = 0;

  GetModuleFileName(NULL, loaddir, MAX_PATH);
  // remove the application name
  *(strrchr(loaddir, '\\')) = '\0';

  //

  // open the file that contains the main class name and java args

  char *argsfilepath = (char*) 
    malloc(strlen(loaddir) * sizeof(char) + 
           strlen(ARGS_FILE_PATH) * sizeof(char) + 1);
  strcpy(argsfilepath, loaddir);
  strcat(argsfilepath, ARGS_FILE_PATH);

  char javaArgs[512];
  char javaMainClass[512];
  char jarList[512];
  char *app_classpath = (char *)malloc(10 * strlen(loaddir) + 4096);

  FILE *argsfile = fopen(argsfilepath, "r");
  if (argsfile == NULL) {  // won't exist for processing.exe
    strcpy(javaArgs, "-Xms128m -Xmx128m");
    strcpy(javaMainClass, "processing.app.Base");
    sprintf(app_classpath, 
            "%s\\lib;"
            "%s\\lib\\build;"
            "%s\\lib\\pde.jar;"
            "%s\\lib\\core.jar;"
            "%s\\lib\\mrj.jar;"
            "%s\\lib\\oro.jar;"
            "%s\\lib\\registry.jar;"
            "%s\\lib\\antlr.jar;",
            loaddir, loaddir, loaddir, loaddir, 
            loaddir, loaddir, loaddir, loaddir);
  } else {
    fgets(javaArgs, 511, argsfile);
    removeLineEndings(javaArgs);
    fgets(javaMainClass, 511, argsfile);
    removeLineEndings(javaMainClass);
    fgets(jarList, 511, argsfile);
    removeLineEndings(jarList);

    //MessageBox(NULL, javaArgs, "args",  MB_OK);
    //MessageBox(NULL, javaMainClass, "class", MB_OK);
    //MessageBox(NULL, jarList, "jarlist", MB_OK);

    app_classpath[0] = 0;
    char *jar = (char*) strtok(jarList, ",");
    while (jar != NULL) {
      //MessageBox(NULL, jar, "entry 1", MB_OK);      
      char entry[1024];
      sprintf(entry, "%s\\lib\\%s;", loaddir, jar);
      //MessageBox(NULL, entry, "entry 2", MB_OK);      
      strcat(app_classpath, entry);
      jar = (char*) strtok(NULL, ",");
    }
    //MessageBox(NULL, app_classpath, "app cp", MB_OK);
    fclose(argsfile);
  }

  //  

  char *cp = (char *)malloc(10 * strlen(loaddir) + 4096);

  // test to see if running with a java runtime nearby or not
  char *testpath = (char *)malloc(MAX_PATH * sizeof(char));
  *testpath = 0;
  strcpy(testpath, loaddir);
  strcat(testpath, "\\java\\bin\\java.exe");
  FILE *fp = fopen(testpath, "rb");
  int local_jre_installed = (fp != NULL);
  //char *rt_jar = (fp == NULL) ? "" : "java\\lib\\rt.jar;";
  if (fp != NULL) fclose(fp); // argh! this was probably causing trouble

  //const char *envClasspath = getenv("CLASSPATH");
  char *env_classpath = (char *)malloc(16384 * sizeof(char));

  // ignoring CLASSPATH for now, because it's not needed
  // and causes more trouble than it's worth [0060]
  env_classpath[0] = 0;

  /*
  // keep this code around since may be re-enabled later
  if (getenv("CLASSPATH") != NULL) {
    strcpy(env_classpath, getenv("CLASSPATH"));
    if (env_classpath[0] == '\"') {
      // starting quote in classpath.. yech
      env_classpath++;  // shitty.. i know..

      int len = strlen(env_classpath);
      if (env_classpath[len-1] == '\"') {
        env_classpath[len-1] = 0;
      } else {
        // a starting quote but no ending quote.. ugh
        // maybe throw an error
      }
    }
    int last = strlen(env_classpath);
    env_classpath[last++] = ';';
    env_classpath[last] = 0;
  } else {
    env_classpath[0] = 0;
  }
  */

  char *qtjava_path = (char *)malloc(16384 * sizeof(char));
  qtjava_path[0] = 0;

  if (getenv("QTJAVA") != NULL) {
    char *qtjava_temp = (char *)malloc(16384 * sizeof(char));
    strcpy(qtjava_temp, getenv("QTJAVA"));
    if (qtjava_temp[0] == '\"') {  // has quotes
      // remove quotes by subsetting string by two
      strncpy(qtjava_path, &qtjava_temp[1], strlen(qtjava_temp) - 2);
    } else {
      strcpy(qtjava_path, getenv("QTJAVA"));
    }
    FILE *fp = fopen(qtjava_path, "rb");
    if (fp != NULL) {
      fclose(fp);  // found it, all set
      strcat(qtjava_path, ";"); // add path separator
      //MessageBox(NULL, "found 1", "msg", MB_OK);
    } else {
      qtjava_path[0] = 0; // not a valid path
    }
  }

  if (qtjava_path[0] == 0) {  // not set yet
    //if (getenv("WINDIR") == NULL) {
    // uh-oh.. serious problem.. gonna have to report this
    // but hopefully WINDIR is set on win98 too
    //} else {

    strcpy(qtjava_path, getenv("WINDIR"));
    strcat(qtjava_path, "\\SYSTEM32\\QTJava.zip");

    FILE *fp = fopen(qtjava_path, "rb");
    if (fp != NULL) {
      fclose(fp);  // found it, all set
      strcat(qtjava_path, ";"); // add path separator
      //MessageBox(NULL, "found 2", "msg", MB_OK);
    } else {
      qtjava_path[0] = 0; // not a valid path      
    }
  }

  if (qtjava_path[0] == 0) {
    strcpy(qtjava_path, getenv("WINDIR"));
    strcat(qtjava_path, "\\SYSTEM\\QTJava.zip");
    
    fp = fopen(qtjava_path, "rb");
    if (fp != NULL) {
      fclose(fp);  // found it, all set
      strcat(qtjava_path, ";"); // add path separator
      //MessageBox(NULL, "found 3", "msg", MB_OK);
    } else {
      // doesn't seem to be installed, which is a problem.
      // but the error will be reported by the pde
      qtjava_path[0] = 0;
    }
  }

  // don't put quotes around contents of cp, even though %s might have 
  // spaces in it. don't put quotes in it, because it's setting the 
  // environment variable for CLASSPATH, not being included on the 
  // command line. so setting the env var it's ok to have spaces, 
  // and the quotes prevent javax.comm.properties from being found.

  strcpy(cp, app_classpath);
  if (local_jre_installed) {
    char localjre[512];
    printf(localjre, "%s\\java\\lib\\rt.jar;", loaddir);
    strcat(cp, localjre);
  }
  strcat(cp, qtjava_path);
  strcat(cp, env_classpath);

  if (!SetEnvironmentVariable("CLASSPATH", cp)) {
    MessageBox(NULL, "Could not set CLASSPATH environment variable",
               "Processing Error", MB_OK);
    return 0;
  }
  
  // need to add the local jre to the path for 'java mode' in the env
  if (local_jre_installed) {
    char *env_path = (char *)malloc(strlen(getenv("PATH")) * sizeof(char));
    strcpy(env_path, getenv("PATH"));
    char *paf = (char *)malloc((strlen(env_path) + strlen(loaddir) + 32) * sizeof(char));
    sprintf(paf, "%s\\java\\bin;%s", loaddir, env_path);

    if (!SetEnvironmentVariable("PATH", paf)) {
      MessageBox(NULL, "Could not set PATH environment variable",
                 "Processing Error", MB_OK);
      return 0;
    }
  }

  // what gets put together to pass to jre
  char *outgoing_cmdline = (char *)malloc(16384 * sizeof(char));

  // prepend the args for -mx and -ms
  strcpy(outgoing_cmdline, javaArgs);
  strcat(outgoing_cmdline, " ");

  // add the name of the class to execute and a space before the next arg
  strcat(outgoing_cmdline, javaMainClass);
  strcat(outgoing_cmdline, " ");

  // append additional incoming stuff (document names), if any
  strcat(outgoing_cmdline, incoming_cmdline);

  //MessageBox(NULL, outgoing_cmdline, "cmdline", MB_OK);

  char *executable = (char *)malloc((strlen(loaddir) + 256) * sizeof(char));
  // loaddir is the name path to the current application

  //if (localJreInstalled) {
  if (local_jre_installed) {
    strcpy(executable, loaddir);
    // copy in the path for javaw, relative to launcher.exe
    strcat(executable, "\\java\\bin\\javaw.exe");
  } else {
    strcpy(executable, "javaw.exe");
  }

  SHELLEXECUTEINFO ShExecInfo;

  // set up the execution info
  ShExecInfo.cbSize = sizeof(SHELLEXECUTEINFO);
  ShExecInfo.fMask = 0;
  ShExecInfo.hwnd = 0;
  ShExecInfo.lpVerb = "open";
  ShExecInfo.lpFile = executable;
  ShExecInfo.lpParameters = outgoing_cmdline;
  ShExecInfo.lpDirectory = loaddir;
  ShExecInfo.nShow = SW_SHOWNORMAL;
  ShExecInfo.hInstApp = NULL;

  if (!ShellExecuteEx(&ShExecInfo)) {
    MessageBox(NULL, "Error calling ShellExecuteEx()", 
               "Processing Error", MB_OK);
    return 0;
  }

  if (reinterpret_cast<int>(ShExecInfo.hInstApp) <= 32) {

    // some type of error occurred
    switch (reinterpret_cast<int>(ShExecInfo.hInstApp)) {
    case ERROR_FILE_NOT_FOUND:
    case ERROR_PATH_NOT_FOUND:
	    MessageBox(NULL, "A required file could not be found. \n"
                 "You may need to install a Java runtime\n"
                 "or re-install Processing.",
                 "Processing Error", MB_OK);
	    break;
    case 0:
    case SE_ERR_OOM:
	    MessageBox(NULL, "Not enough memory or resources to run at"
                 " this time.", "Processing Error", MB_OK);
	    
	    break;
    default:
	    MessageBox(NULL, "There is a problem with your installation.\n"
                 "If the problem persists, re-install the program.", 
                 "Processing Error", MB_OK);
	    break;
    }
  }

  return 0;
}


void removeLineEndings(char *what) {
  int index = strlen(what) - 1;
  while (index >= 0) {
    if ((what[index] == 10) || (what[index] == 13)) {
      what[index] = 0;
      --index;
    } else {
      return;
    }
  }
}
