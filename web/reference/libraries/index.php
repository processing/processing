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
<div style="clear: left">
<br />
<br />
<br />
<img src="images/contributions.png">
<br />
<br />
</div>
<div class="columnpadded">
<b><a href="http://mjs.darkgreenmedia.com/site/?service=Sections&task=showSection&sectionID=3&contentID=19">MarlonJ</a></b> has contributed a number of alternative and new libraries for Mobile Processing, many based on the latest new <a href="http://developers.sun.com/techtopics/mobility/apis/">Optional Packages</a> for Java Micro Edition.<br />
<br />
Not all phones will support the libraries, but if you're looking to experiment with the latest and greatest in Java Micro Edition technologies, you'll find most of them here!<br />
<br />
<br />
</div>
<div class="column">
<b><a href="http://mjs.darkgreenmedia.com/site/?service=Sections&task=showSection&sectionID=3&contentID=61">MAudio3D</a></b><br />
Library for 3D audio.<br />
<br />
<br />
<br />
<br />
<b><a href="http://mjs.darkgreenmedia.com/site/?service=Sections&task=showSection&sectionID=3&contentID=44">M3D</a></b><br />
Library for 3D graphics.<br />
<br />
<br />
<br />
<br />
<b><a href="http://mjs.darkgreenmedia.com/site/?service=Sections&task=showSection&sectionID=3&contentID=29">MBluetooth</a></b><br />
Library for Bluetooth communication.<br />
<br />
<br />
<b><a href="http://mjs.darkgreenmedia.com/site/?service=Sections&task=showSection&sectionID=3&contentID=30">MClientServer</a></b><br />
Library for creating client/server connections. Used with MBluetooth for creating discoverable services.<br />
<br />
<br />
<b><a href="http://mjs.darkgreenmedia.com/site/?service=Sections&task=showSection&sectionID=3&contentID=20">MLocation</a></b><br />
Library for location positioning.<br />
<br />
<br />
<b><a href="http://mjs.darkgreenmedia.com/site/?service=Sections&task=showSection&sectionID=3&contentID=22">MMessaging</a></b><br />
Library for sending and receiving text messages.<br />
<br />
<br />
</div>
<div class="column">
<b><a href="http://mjs.darkgreenmedia.com/site/?service=Sections&task=showSection&sectionID=3&contentID=56">MNokiaUI</a></b><br />
Library for supporting the proprietary<br />Nokia UI API (including alpha channel support).  Early Nokia phones only.<br />
<br />
<br />
<b><a href="http://mjs.darkgreenmedia.com/site/?service=Sections&task=showSection&sectionID=3&contentID=51">MPush</a></b><br />
Library for supporting push registry activation of applications (timer or connection activated).<br />
<br />
<br />
<b><a href="http://mjs.darkgreenmedia.com/site/?service=Sections&task=showSection&sectionID=3&contentID=66">MQRCode</a></b><br />
Library for decoding QRCode 2D barcodes.<br />
<br />
<br />
<b><a href="http://mjs.darkgreenmedia.com/site/?service=Sections&task=showSection&sectionID=3&contentID=25">MSound</a></b><br />
Library for sound playback and recording.<br />
<br />
<br />
<br />
<br />
<b><a href="http://mjs.darkgreenmedia.com/site/?service=Sections&task=showSection&sectionID=3&contentID=27">MVideo</a></b><br />
Library for video playback.<br />
<br />
<br />
<b><a href="http://mjs.darkgreenmedia.com/site/?service=Sections&task=showSection&sectionID=3&contentID=32">MWebServices</a></b><br />
Library for consuming web services.<br />
<br />
<br />
</div>
<?php
require '../../footer.inc.php';
?>
