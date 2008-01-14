<?php
$PAGE_TITLE = "Mobile Processing &raquo; Libraries";
require '../../header.inc.php';

if ($offline) {
    $libs = array("phone", "bluetooth", "image2", "sound", "xml", "video", "messaging");
    foreach ($libs as $l) {
        chdir($l);
        clean();
        exec($argv[2] ." index.php ../../../ ". $argv[2] ." > index.html");
        chdir("..");
    }
}

?>
<img src="images/header.png"><br>
<br>
<br>
<?php require 'core.inc.php' ?>
<div style="clear: left">
<br />
<br />
<br />
<?php require URL_ROOT .'wiki/index.php?n=Libraries.HomePage&include=1' ?>
</div>
<?php
require '../../footer.inc.php';
?>
