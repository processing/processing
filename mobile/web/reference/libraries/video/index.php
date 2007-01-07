<?php

$PAGE_TITLE = "Video &raquo; Libraries &raquo; Mobile Processing";

$PAGE_SHOWBACKINDEX = true;
$PAGE_BACK_LINK = '../index.php';
$PAGE_BACK_NAME = 'Libraries';

require '../../../header.inc.php';
?>
<div class="column">
<h3>Video</h3><br>
<br>
The Video library allows Mobile Processing sketches to play and record sounds on supported mobile phones.
</div>
<div class="column" style="padding-left: 40px">
<b>Capture</b><br>
The capture class allows live video output and frame capture.<br>
<br>
<a href="<?php echo reflink("Capture") ?>">Capture</a><br>
<br>
<a href="<?php echo reflink("Capture_viewX") ?>">viewX</a><br>
<a href="<?php echo reflink("Capture_viewY") ?>">viewY</a><br>
<a href="<?php echo reflink("Capture_viewWidth") ?>">viewWidth</a><br>
<a href="<?php echo reflink("Capture_viewHeight") ?>">viewHeight</a><br>
<a href="<?php echo reflink("Capture_sourceWidth") ?>">sourceWidth</a><br>
<a href="<?php echo reflink("Capture_sourceHeight") ?>">sourceHeight</a><br>
<br>
<a href="<?php echo reflink("Capture_close") ?>">close()</a><br>
<a href="<?php echo reflink("Capture_hide") ?>">hide()</a><br>
<a href="<?php echo reflink("Capture_read") ?>">read()</a><br>
<a href="<?php echo reflink("Capture_show") ?>">show()</a><br>
</div>
<?php
require '../../../footer.inc.php';
?>
