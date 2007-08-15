<?php

require_once '../mobile/db.inc.php';
require_once '../mobile/lib/tera-wurfl/tera_wurfl.php';

$wurfl = new tera_wurfl();
$is_nokia = false;
if ($wurfl->getDeviceCapabilitiesFromAgent($_SERVER['HTTP_USER_AGENT'])) {
  if ($wurfl->model == "Web Browser for S60") {
    $is_nokia = true;
  }
}

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
<?php
if ($is_nokia) {
?>
<b>Note:</b> The model of your phone cannot be detected when using the Nokia Web Browser for S60.  It is recommended that you visit this mobile website using the Nokia Services Browser in order to download the Profiler.  Return to the main menu, open the Applications folder, then choose Services.<br />
<br />
<?php
}
?>
To test your phone, choose a link below to download a <b>Profiler</b> application. If you have a MIDP 2.0 phone, choose <b>Advanced</b>. If you don't know, are unsure, or have a MIDP 1.0 phone, choose <b>Basic</b>.<br />
<br />
<span class="accesskey">1</span> <a href="prof/basic/basic.jad" accesskey="1">Profiler Basic</a><br />
<span class="accesskey">2</span> <a href="prof/adv/adv.jad" accesskey="2">Profiler Advanced</a><br />
<?php
require_once 'footer.inc.php';
?>
