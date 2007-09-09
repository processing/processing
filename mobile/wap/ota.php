<?php

require_once 'settings.inc.php';
require_once $WEB_ROOT .'/db.inc.php';

//// start setting up header vars
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

        $PAGE_TITLE = $data['title'];
        $PAGE_HEADER = <<<EOF
{$PAGE_HEADER} <a href="exh.php">Exhibition</a> \ {$data['title']}
EOF;
    }
} else {
    //// example
}

$PAGE_FOOTER = $PAGE_HEADER;

require_once 'header.inc.php';
if (($code % 2) == 0) {
    //// exhibition item
?>
<img src="<?php echo get_sized_image($data['mobileimgurl']) ?>" /><br />
<?php echo $data['title'] ?><br />
<span class="smaller">by <?php echo $data['name'] ?></span><br />
<br />
<?php echo $data['description'] ?><br />
<br />
<?php
     $accesskey = 1;
     if (isset($data['jadurl']) && ($data['jadurl'] != "")) {
?>
<span class="accesskey"><?php echo $accesskey ?></span> <a href="go.php?code=<?php echo $code?>" accesskey="<?php echo $accesskey ?>">Download</a><br />
<?php
         $accesskey++;
     } else {
?>
This sketch is not available for download.<br />
<br />
<?php
     }
     if (isset($data['mobileurl']) && ($data['mobileurl'] != "")) {
?>
<span class="accesskey"><?php echo $accesskey ?></span> <a href="<?php echo $data['mobileurl'] ?>" accesskey="<?php echo $accesskey ?>">Go to Mobile Homepage</a>
<?php
     }
} else {
    //// example
?>
<?php
}
require_once 'footer.inc.php';
?>
