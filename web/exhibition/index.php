<?php

$PAGE_TITLE = "Mobile Processing &raquo; Exhibition";
require '../header.inc.php';
?>
<div class="column2x">
<img src="images/header.png"><br>
<br>
<br>
<table border="0" cellspacing="0" cellpadding="0">
  <tr>
    <td valign="top" width="224">
<a href="http://todbot.com/blog/2006/09/12/roombactrl-drive-your-roomba-with-your-cell-phone/" target="_new"><img src="images/roombactrl.png"></a><br>
<br>
<table width="200" border="0" cellspacing="0" cellpadding="0">
  <tr>
    <td>
<b><a href="http://todbot.com/blog/2006/09/12/roombactrl-drive-your-roomba-with-your-cell-phone/" target="_new">RoombaCtrl</a></b>
    </td>
  </tr>
  <tr height="5">
  </tr>
  <tr>
    <td>
<b>by todbot</b><br />
<br />
RoombaCtrl works with the build-your-own Bluetooth adapter shown in the book "Hacking Roomba" or the pre-built RooTooth so you can drive your Roomba with your cellphone.
    </td>
  </tr>
</table>
    </td>
    <td valign="top" width="224">
<a href="http://i.document.m05.de/?p=189" target="_new"><img src="images/pixiebubble.png"></a><br>
<br>
<table width="200" border="0" cellspacing="0" cellpadding="0">
  <tr>
    <td>
<b><a href="http://i.document.m05.de/?p=189" target="_new">pixiebubble</a></b>
    </td>
  </tr>
  <tr height="5">
  </tr>
  <tr>
    <td>
<b>by michael</b><br />
<br />
pixiebubble is an application for mobile phones where images are processed into sound sequences. pixels of a photo shot with the phones camera are translated into bubble sounds.<br />
<br />
    </td>
  </tr>
</table>
    </td>
  </tr>
  <tr>
    <td colspan="2">&nbsp;</td>
  </tr>
  <tr>
    <td valign="top" width="224">
<a href="mandelbrotMobile/applet/index.html" target="_new"><img src="images/mandelbrotMobile.png"></a><br>
<br>
<table width="200" border="0" cellspacing="0" cellpadding="0">
  <tr>
    <td>
<b><a href="mandelbrotMobile/applet/index.html" target="_new">mandelbrotMobile</a></b>
    </td>
  </tr>
  <tr height="5">
  </tr>
  <tr>
    <td>
<b>by skanaar</b>
    </td>
  </tr>
</table>
    </td>
    <td valign="top" width="224">
<a href="http://www.mimeomai.f2s.com/applet/rosalymob/applet/index.html" target="_new"><img src="images/rosaly.png"></a><br>
<br>
<table width="200" border="0" cellspacing="0" cellpadding="0">
  <tr>
    <td>
<b><a href="http://www.mimeomai.f2s.com/applet/rosalymob/applet/index.html" target="_new">Rosaly</a></b>
    </td>
  </tr>
  <tr height="5">
  </tr>
  <tr>
    <td>
<b>by Oolong</b><br>
<br>
0 toggles lines/dots<br>
9 changes colours<br>
Use direction arrows for other controls<br>
<br>
<a href="http://oolong.co.uk/">Oolong</a><br />
<br />
    </td>
  </tr>
</table>
    </td>
  </tr>
  <tr>
    <td colspan="2">&nbsp;</td>
  </tr>
  <tr>
    <td valign="top" width="224">
<a href="http://www.mimeomai.f2s.com/applet/resmob/applet/index.html" target="_new"><img src="images/resonata.png"></a><br>
<br>
<table width="200" border="0" cellspacing="0" cellpadding="0">
  <tr>
    <td>
<b><a href="http://www.mimeomai.f2s.com/applet/resmob/applet/index.html" target="_new">Resonata</a></b>
    </td>
  </tr>
  <tr height="5">
  </tr>
  <tr>
    <td>
<b>by Oolong</b><br>
<br>
This is a spring system, designed to demonstrate many of the universal features of waves.<br>
<br>
<a href="http://oolong.co.uk/">Oolong</a>
    </td>
  </tr>
</table>
    </td>
    <td valign="top" width="224">
    </td>
  </tr>
</table>
</div>
<div class="column">
<img src="images/network.png"><br />
<br />
<a href="submit.php"><img border="0" src="images/addlink.png"></a><br />
<br />
<?php
$lockfp = fopen('network/lockfile', 'r');
if ($lockfp !== FALSE) {
    if (flock($lockfp, LOCK_SH)) {
        include 'network/links.inc.php';
    }
}
?>
</div>
<?php
 require '../footer.inc.php';
?>