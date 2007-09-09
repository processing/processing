<?php

require_once 'settings.inc.php';
require_once $WEB_ROOT .'/db.inc.php';

//// start setting up header vars
$PAGE_TITLE = "Download";
$PAGE_HEADER = <<<EOF
<a href="index.php">Cover</a> \
EOF;

//// look up item in db
$link = db_connect();
$code = $_GET['code'];
if (($code % 2) == 0) {
    //// exhibition item
    $query = "SELECT * FROM curated WHERE id=". ($code / 2);
    $result = mysql_query($query, $link);
    if ($result) {
        $data = mysql_fetch_assoc($result);
        mysql_free_result($result);

        $PAGE_HEADER = <<<EOF
{$PAGE_HEADER} <a href="exh.php">Exhibition</a> \ <a href="ota.php?code={$code}">{$data['title']}</a> \ Download
EOF;
    }
} else {
    //// example
}

$PAGE_FOOTER = $PAGE_HEADER;

require_once 'header.inc.php';
?>
The content you are about to download is hosted on another website. It is not guaranteed to run on your phone. You are responsible for any additional charges or fees that may be incurred during its use. By selecting the link below you agree to these terms.<br />
<br />
<?php
if (($code % 2) == 0) {
    //// exhibition item
?>
<span class="accesskey">1</span> <a href="<?php echo $data['jadurl'] ?>" accesskey="1">I agree, start downloading</a>
<?php
} else {
    //// example
?>
<?php
}
require_once 'footer.inc.php';
?>
