<?php

$PAGE_TITLE = "Mobile Processing &raquo; Examples";
require '../header.inc.php';
?>
<img src="images/header.png"><br />
<br />
<br />
<?php 
$lockfp = fopen('lockfile', 'r');
if ($lockfp !== FALSE) {
    if (flock($lockfp, LOCK_SH)) {
        require 'generated/links.inc.php';
    }
}
?>
<?php
 require '../footer.inc.php';
?>