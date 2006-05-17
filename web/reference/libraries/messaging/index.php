<?php

$PAGE_TITLE = "Messaging &raquo; Libraries &raquo; Mobile Processing";

$PAGE_SHOWBACKINDEX = true;
$PAGE_BACK_LINK = '../index.php';

require '../../../header.inc.php';
?>
<div class="column">
<h3>Messaging</h3><br>
<br>
The Messaging library allows Mobile Processing sketches to send and receive text and data via phone messaging services.
</div>
<div class="column" style="padding-left: 40px">
<b>Messenger</b><br>
This class provides the primary interface for sending and receiving messages.<br>
<br>
<a href="reference.php?name=Messenger">Messenger</a><br>
<a href="reference.php?name=Messenger_send">send()</a><br>
<br>
<a href="reference.php?name=Messenger_EVENT_MSG_RECEIVED">EVENT_MSG_RECEIVED</a><br>
<br>
<br>
<b>Message</b><br>
This class represents messages that have been received.
<br>
<a href="reference.php?name=Message">Message</a><br>
<a href="reference.php?name=Message_readBytes">readBytes()</a><br>
<a href="reference.php?name=Message_readString">readString()</a><br>
</div>
<?php
require '../../../footer.inc.php';
?>
