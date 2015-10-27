@echo off

.\java\bin\java -cp "lib/pde.jar;core/library/core.jar;lib/jna.jar;lib/antlr.jar;lib/ant.jar;lib/ant-launcher.jar;lib/org-netbeans-swing-outline.jar;lib/com.ibm.icu_4.4.2.v20110823.jar;lib/jdi.jar;lib/jdimodel.jar;lib/org.eclipse.osgi_3.8.1.v20120830-144521.jar;tools/MovieMaker/tool/MovieMaker.jar"  processing.app.tools.MovieMaker %*
