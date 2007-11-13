<?php

$PAGE_TITLE = "Learning";
$PAGE_HEADER = <<<EOF
<a href="index.php">Cover</a> \ Learning
EOF;
$PAGE_FOOTER = $PAGE_HEADER;

require_once 'header.inc.php';

include_once $WEB_ROOT .'/learning/generated/waplinks.inc.php';

require_once 'footer.inc.php';
?>
