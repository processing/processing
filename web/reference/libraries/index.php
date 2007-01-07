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
<img src="images/core.png"><br>
<br>
<div class="column">
<b><a href="phone/index.<?php echo $ext ?>">Phone</a></b><br>
Controlling phone-specific features.<br>
<br>
<br>
<b><a href="bluetooth/index.<?php echo $ext ?>">Bluetooth</a></b><br>
Sending and receiving data via Bluetooth wireless connections.<br>
<br>
<br>
<b><a href="image2/index.<?php echo $ext ?>">Image2</a></b><br>
Provides a new PImage-compatible image with an editable pixel buffer.
</div>
<div class="column">
<b><a href="sound/index.<?php echo $ext ?>">Sound</a></b><br>
Sound playback.<br>
<br>
<br>
<b><a href="xml/index.<?php echo $ext ?>">XML</a></b><br>
Parsing and outputting XML documents.
</div>
<div class="column">
<b><a href="video/index.<?php echo $ext ?>">Video</a></b><br>
Video playback and frame capture.<br>
<br>
<br>
<b><a href="messaging/index.<?php echo $ext ?>">Messaging</a></b><br>
Sending and receiving text and data messages.
</div>
<?php
require '../../footer.inc.php';
?>
