<?php
$PAGE_TITLE = "Mobile Processing &raquo; Download";

require '../header.inc.php';
?>
<img src="images/header.png"><br>
&nbsp;
<ol>
  <li>Download and install the Wireless Toolkit (WTK) from Sun. The latest version is 2.2.<br><br>
<a href="http://java.sun.com/products/j2mewtoolkit/">http://java.sun.com/products/j2mewtoolkit/</a><br>&nbsp;</li>
  <li>Download and install Mobile Processing.<br><br>
0092 ALPHA&nbsp;&nbsp|&nbsp;&nbsp;19 09 2005&nbsp;&nbsp;Windows <a href="processing-0092.zip">Standard</a> or <a href="processing-0092-expert.zip">without Java</a><br><br></li>
  <li>Modify <b>preferences.txt</b> to include the following line, based on your installation location of the WTK:<br>
<br>
wtk.path=C:\WTK22<br>&nbsp;</li>
</ol>

<?php
require '../footer.inc.php';
?>