Packages from Eclipse 4.4.1:
http://download.eclipse.org/eclipse/downloads/

File listing at:
http://download.eclipse.org/eclipse/updates/4.4/R-4.4.1-201409250400/plugins/?d

The jdtCompilerAdapter.jar is extracted from org.eclipse.jdt.core.jar to provide
the JDTCompilerAdapter class, which is the Ant task for the JDT compiler.
The jdi.jar and jdimodel.jar files are unpacked from org.eclipse.jdt.debug.jar.

This Mode does not use ecj.jar from the original Java mode, because its files are contained in the JARs below.

. . . 

http://download.eclipse.org/eclipse/updates/4.4/R-4.4.1-201409250400/plugins/com.ibm.icu_52.1.0.v201404241930.jar

http://download.eclipse.org/eclipse/updates/4.4/R-4.4.1-201409250400/plugins/org.eclipse.core.contenttype_3.4.200.v20140207-1251.jar

http://download.eclipse.org/eclipse/updates/4.4/R-4.4.1-201409250400/plugins/org.eclipse.core.jobs_3.6.0.v20140424-0053.jar

http://download.eclipse.org/eclipse/updates/4.4/R-4.4.1-201409250400/plugins/org.eclipse.core.resources_3.9.1.v20140825-1431.jar

http://download.eclipse.org/eclipse/updates/4.4/R-4.4.1-201409250400/plugins/org.eclipse.core.runtime_3.10.0.v20140318-2214.jar

http://download.eclipse.org/eclipse/updates/4.4/R-4.4.1-201409250400/plugins/org.eclipse.equinox.common_3.6.200.v20130402-1505.jar

http://download.eclipse.org/eclipse/updates/4.4/R-4.4.1-201409250400/plugins/org.eclipse.equinox.preferences_3.5.200.v20140224-1527.jar

http://download.eclipse.org/eclipse/updates/4.4/R-4.4.1-201409250400/plugins/org.eclipse.jdt.core_3.10.0.v20140902-0626.jar

http://download.eclipse.org/eclipse/updates/4.4/R-4.4.1-201409250400/plugins/org.eclipse.jdt.debug_3.8.101.v20140902-1548.jar

http://download.eclipse.org/eclipse/updates/4.4/R-4.4.1-201409250400/plugins/org.eclipse.osgi_3.10.1.v20140909-1633.jar

http://download.eclipse.org/eclipse/updates/4.4/R-4.4.1-201409250400/plugins/org.eclipse.text_3.5.300.v20130515-1451.jar

. . . 

Updated 19 January 2015 to fix Java 8 support. The previous versions gave an "Annotation processing got disabled, since it requires a 1.6 compliant JVM" error.

Finally, the archive site contains additional jars for the 4.4.1 release, including the JDT Core Batch Compiler (ECJ):
http://archive.eclipse.org/eclipse/downloads/drops4/R-4.4.1-201409250400/
