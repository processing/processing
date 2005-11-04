<?php

$PAGE_TITLE = "Mobile Processing &raquo; Language (API)";
$PAGE_SHOWBACKINDEX = true;

$pos = strpos($_GET['name'], "_");
if ($pos !== false) {
    $parent = substr($_GET['name'], 0, $pos);
    if (file_exists('API/'. $parent .'.xml')) {
        $PAGE_BACK_LINK = 'reference.php?name='. $parent;
        $PAGE_BACK_NAME = 'Back';
    }
}

require '../header.inc.php';

require 'reference.inc.php';

require '../footer.inc.php';
?>
