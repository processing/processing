<?php

$PAGE_TITLE = "Sound &raquo; Libraries &raquo; Mobile Processing";

$PAGE_SHOWBACKINDEX = true;
$PAGE_BACK_LINK = '../index.php';
$PAGE_BACK_NAME = 'Libraries';

require '../../../header.inc.php';
?>
<div class="column">
<h3>Sound</h3><br>
<br>
The Sound library allows Mobile Processing sketches to play sounds on supported mobile phones.
</div>
<div class="column" style="padding-left: 40px">
<b>Sound</b><br>
This class is used to create sound objects for playback.<br>
<br>
<a href="<?php echo reflink("Sound") ?>">Sound</a><br>
<a href="<?php echo reflink("Sound_duration") ?>">duration()</a><br>
<a href="<?php echo reflink("Sound_loop") ?>">loop()</a><br>
<a href="<?php echo reflink("Sound_pause") ?>">pause()</a><br>
<a href="<?php echo reflink("Sound_play") ?>">play()</a><br>
<a href="<?php echo reflink("Sound_playTone") ?>">playTone()</a><br>
<a href="<?php echo reflink("Sound_stop") ?>">stop()</a><br>
<a href="<?php echo reflink("Sound_supportedTypes") ?>">supportedTypes()</a><br>
<a href="<?php echo reflink("Sound_time") ?>">time()</a><br>
<a href="<?php echo reflink("Sound_volume") ?>">volume()</a>
</div>
<?php
require '../../../footer.inc.php';
?>
