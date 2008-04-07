<?php

$PAGE_TITLE = "Mobile Processing &raquo; Environment";
require '../../header.inc.php';
?>
<img src="images/header.png"><br />
<br />
<br />
The Mobile Processing environment is based on original Processing Environment and shares most of the same features. Refer to the Processing.org <a href="http://processing.org/reference/environment/">Environment</a> documentation to begin. The following are some differences:<br />
<br />
1. Mobile Processing currently does not support alternative rendering modes.<br />
<br />
2. The Settings dialog contains a new Mobile tab, which is used to specify the location of the wireless toolkit for building applications and additional options for advanced users.<br />
<br />
<img src="images/settings.png"><br />
<br />
3. Bitmapped fonts can significantly increase the size and runtime memory needs of an application.  The Create Font dialog allows you to select only the specific characters you need to display in your application.  To specify specific characters, check the "Other:" box and enter them in the text field.<br />
<br />
<img src="images/createfont.png" /><br />
<br />
4. Mobile Processing maintains separate version information for the PDE, Core APIs, documentation, and libraries. Run "Check for updates..." to download and install any new versions. You can view the current version information in the About dialog and compare them to the versions listed on the <a href="/download/">Download</a> page.<br />
<br />
<img src="images/about.png" /><br />
<br />
  5. EXPERIMENTAL: If you are being a proxy and cannot run "Check for updates...", you can set additional properties in your preferences file.  Find the location of your preferences file by opening "Preferences" from the "File" menu and looking at the path specified at the bottom of the dialog.  Add the following properties to your preferences file, depending upon your proxy configuration:<br />
<br />
http.proxyHost<br />
http.proxyPort<br />
<br />
socksProxyHost<br />
socksProxyPort<br />
java.net.socks.username<br />
java.net.socks.password<br />
<br />
Refer to the following documentation links at Sun Microsystems for more information on Java and network proxies:<br />
<br />
<a href="http://java.sun.com/javase/6/docs/technotes/guides/net/proxies.html">Java Networking and Proxies</a><br />
<a href="http://java.sun.com/javase/6/docs/technotes/guides/net/properties.html">Networking Properties</a><br />
<br />

<?php
require '../../footer.inc.php';
?>
