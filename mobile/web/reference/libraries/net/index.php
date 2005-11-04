<?php

$PAGE_TITLE = "Net &raquo; Libraries &raquo; Mobile Processing";

$PAGE_SHOWBACKINDEX = true;
$PAGE_BACK_LINK = '../index.php';

require '../../../header.inc.php';
?>
<div class="column">
<img src="images/header.png"><br>
<br>
The Net library allows Mobile Processing sketches to send and receive data via the Internet. Mobile sketches are allowed to connect to web servers in the same way as desktop web browsers.
</div>
<div class="column" style="padding-left: 40px">
<b>Client</b><br>
The client class is used to create client Objects which connect to a server to exchange data.<br>
<br>
<a href="reference.php?name=Client">Client()</a><br>
<a href="reference.php?name=Client_GET">GET()</a><br>
<a href="reference.php?name=Client_POST">POST()</a><br>
<a href="reference.php?name=Client_read">read()</a><br>
<a href="reference.php?name=Client_readChar">readChar()</a><br>
<a href="reference.php?name=Client_readBytes">readBytes()</a><br>
<a href="reference.php?name=Client_readString">readString()</a><br>
</div>
<?php
require '../../../footer.inc.php';
?>
