Packages from Eclipse 4.5.2:
http://download.eclipse.org/eclipse/downloads/

File listing at:
http://download.eclipse.org/eclipse/updates/4.5/R-4.5.2-201602121500/plugins/?d

The jdtCompilerAdapter.jar is extracted from org.eclipse.jdt.core.jar to provide
the JDTCompilerAdapter class, which is the Ant task for the JDT compiler.
The jdi.jar and jdimodel.jar files are unpacked from org.eclipse.jdt.debug.jar.

This Mode does not use ecj.jar from the original Java mode, because its files are contained in the JARs below.

For 3.0 alpha 11, the signature files were removed from oorg.eclipse.jdt.core.jar to fix a signing conflict with Android Mode and Ant.
https://github.com/processing/processing/pull/3324

. . .

http://download.eclipse.org/eclipse/updates/4.5/R-4.5.2-201602121500/plugins/com.ibm.icu_54.1.1.v201501272100.jar

http://download.eclipse.org/eclipse/updates/4.5/R-4.5.2-201602121500/plugins/org.eclipse.core.contenttype_3.5.0.v20150421-2214.jar

http://download.eclipse.org/eclipse/updates/4.5/R-4.5.2-201602121500/plugins/org.eclipse.core.jobs_3.7.0.v20150330-2103.jar

http://download.eclipse.org/eclipse/updates/4.5/R-4.5.2-201602121500/plugins/org.eclipse.core.resources_3.10.1.v20150725-1910.jar

http://download.eclipse.org/eclipse/updates/4.5/R-4.5.2-201602121500/plugins/org.eclipse.core.runtime_3.11.1.v20150903-1804.jar

http://download.eclipse.org/eclipse/updates/4.5/R-4.5.2-201602121500/plugins/org.eclipse.equinox.common_3.7.0.v20150402-1709.jar

http://download.eclipse.org/eclipse/updates/4.5/R-4.5.2-201602121500/plugins/org.eclipse.equinox.preferences_3.5.300.v20150408-1437.jar

http://download.eclipse.org/eclipse/updates/4.5/R-4.5.2-201602121500/plugins/org.eclipse.jdt.core_3.11.2.v20160128-0629.jar

http://download.eclipse.org/eclipse/updates/4.5/R-4.5.2-201602121500/plugins/org.eclipse.osgi_3.10.102.v20160118-1700.jar

http://download.eclipse.org/eclipse/updates/4.5/R-4.5.2-201602121500/plugins/org.eclipse.text_3.5.400.v20150505-1044.jar

. . .

Updated 19 January 2015 to fix Java 8 support. The previous versions gave an "Annotation processing got disabled, since it requires a 1.6 compliant JVM" error.

Updated 21 March 2016 to newest version (from R-4.4.1-201409250400 to R-4.5.2-201602121500) as part of updating Java Mode

Finally, the archive site contains additional jars for the 4.5.2 release, including the JDT Core Batch Compiler (ECJ):
http://archive.eclipse.org/eclipse/downloads/drops4/R-4.5.2-201602121500/

. . .

Updated the org.eclipse libs. For eclipse projects, please see https://www.eclipse.org/legal/epl-2.0/ for licensing.
