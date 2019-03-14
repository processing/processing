/*
Part of the Processing project - http://processing.org

Copyright (c) 2019 The Processing Foundation

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License version 2
as published by the Free Software Foundation.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software Foundation,
Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package processing.mode.java.pdex.util.runtime;


/**
 * Constants related to runtime component enumeration.
 */
public class RuntimeConst {

  /**
   * The modules comprising the Java standard modules.
   */
  public static final String[] STANDARD_MODULES = {
      "java.base.jmod",
      "java.compiler.jmod",
      "java.datatransfer.jmod",
      "java.desktop.jmod",
      "java.instrument.jmod",
      "java.logging.jmod",
      "java.management.jmod",
      "java.management.rmi.jmod",
      "java.naming.jmod",
      "java.net.http.jmod",
      "java.prefs.jmod",
      "java.rmi.jmod",
      "java.scripting.jmod",
      "java.se.jmod",
      "java.security.jgss.jmod",
      "java.security.sasl.jmod",
      "java.smartcardio.jmod",
      "java.sql.jmod",
      "java.sql.rowset.jmod",
      "java.transaction.xa.jmod",
      "java.xml.crypto.jmod",
      "java.xml.jmod",
      "jdk.accessibility.jmod",
      "jdk.aot.jmod",
      "jdk.attach.jmod",
      "jdk.charsets.jmod",
      "jdk.compiler.jmod",
      "jdk.crypto.cryptoki.jmod",
      "jdk.crypto.ec.jmod",
      "jdk.dynalink.jmod",
      "jdk.editpad.jmod",
      "jdk.hotspot.agent.jmod",
      "jdk.httpserver.jmod",
      "jdk.internal.ed.jmod",
      "jdk.internal.jvmstat.jmod",
      "jdk.internal.le.jmod",
      "jdk.internal.opt.jmod",
      "jdk.internal.vm.ci.jmod",
      "jdk.internal.vm.compiler.jmod",
      "jdk.internal.vm.compiler.management.jmod",
      "jdk.jartool.jmod",
      "jdk.javadoc.jmod",
      "jdk.jcmd.jmod",
      "jdk.jconsole.jmod",
      "jdk.jdeps.jmod",
      "jdk.jdi.jmod",
      "jdk.jdwp.agent.jmod",
      "jdk.jfr.jmod",
      "jdk.jlink.jmod",
      "jdk.jshell.jmod",
      "jdk.jsobject.jmod",
      "jdk.jstatd.jmod",
      "jdk.localedata.jmod",
      "jdk.management.agent.jmod",
      "jdk.management.jfr.jmod",
      "jdk.management.jmod",
      "jdk.naming.dns.jmod",
      "jdk.naming.rmi.jmod",
      "jdk.net.jmod",
      "jdk.pack.jmod",
      "jdk.rmic.jmod",
      "jdk.scripting.nashorn.jmod",
      "jdk.scripting.nashorn.shell.jmod",
      "jdk.sctp.jmod",
      "jdk.security.auth.jmod",
      "jdk.security.jgss.jmod",
      "jdk.unsupported.desktop.jmod",
      "jdk.unsupported.jmod",
      "jdk.xml.dom.jmod",
      "jdk.zipfs.jmod"
  };

  /**
   * The jars required for OpenJFX.
   */
  public static final String[] JAVA_FX_JARS = {
      "javafx-swt.jar",
      "javafx.base.jar",
      "javafx.controls.jar",
      "javafx.fxml.jar",
      "javafx.graphics.jar",
      "javafx.media.jar",
      "javafx.swing.jar",
      "javafx.web.jar"
  };

}
