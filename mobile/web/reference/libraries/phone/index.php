<?php

$PAGE_TITLE = "Phone &raquo; Libraries &raquo; Mobile Processing";

$PAGE_SHOWBACKINDEX = true;
$PAGE_BACK_LINK = '../index.php';
$PAGE_BACK_NAME = 'Libraries';

require '../../../header.inc.php';

?>
<div class="column">
<h3>Phone</h3><br>
<br>
The Phone library allows Mobile Processing sketches to control various phone-specific features. The phone must implement the MIDP 2.0 specification to support this library.
</div>
<div class="column" style="padding-left: 40px">
<b>Phone</b><br>
This class provides access to the phone-specific features of the Phone library.<br>
<br>
<a href="<?php echo reflink("Phone") ?>">Phone</a><br>
<a href="<?php echo reflink("Phone_vibrate") ?>">vibrate()</a><br>
<a href="<?php echo reflink("Phone_flash") ?>">flash()</a><br>
<a href="<?php echo reflink("Phone_call") ?>">call()</a><br>
<a href="<?php echo reflink("Phone_launch") ?>">launch()</a><br>
<a href="<?php echo reflink("Phone_fullscreen") ?>">fullscreen()</a><br>
<a href="<?php echo reflink("Phone_noFullscreen") ?>">noFullscreen()</a><br>
</div>
<?php
require '../../../footer.inc.php';
?>
