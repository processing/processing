<?php

$PAGE_TITLE = "Mobile Processing &raquo; Exhibition";
require '../header.inc.php';
?>
<div class="column2x">
<img src="images/header.png"><br>
<br>
<br>
<?php 
$lockfp = fopen('curated/lockfile', 'r');
if ($lockfp !== FALSE) {
    if (flock($lockfp, LOCK_SH)) {
        require 'curated/generated/links.inc.php';
    }
}
?>
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
        require 'network/generated/links.inc.php';
    }
}
?>
</div>
<?php
require '../footer.inc.php';
?>