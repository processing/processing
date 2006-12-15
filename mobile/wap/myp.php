<?php

$PAGE_TITLE = "My Phone";
$PAGE_HEADER = <<<EOF
<a href="index.php">Cover</a> \ My Phone
EOF;
$PAGE_FOOTER = $PAGE_HEADER;

require_once 'header.inc.php';
?>
<b>Your browser reports:</b><br />
<?php echo $_SERVER['HTTP_USER_AGENT'] ?><br />
<br />
To test your phone, choose a link below to download a <b>Profiler</b> application. If you have a MIDP 2.0 phone, choose <b>Advanced</b>. If you don't know, are unsure, or have a MIDP 1.0 phone, choose <b>Basic</b>.<br />
<br />
<span class="accesskey">1</span> <a href="pbasic/proguard/pbasic.jad" accesskey="1">Profiler Basic</a><br />
<span class="accesskey">2</span> <a href="" accesskey="2">Profiler Advanced</a><br />
<?php
require_once 'footer.inc.php';
?>
