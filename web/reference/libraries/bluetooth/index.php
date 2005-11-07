<?php

$PAGE_TITLE = "Bluetooth &raquo; Libraries &raquo; Mobile Processing";

$PAGE_SHOWBACKINDEX = true;
$PAGE_BACK_LINK = '../index.php';

require '../../../header.inc.php';
?>
<div class="column">
<h3>Bluetooth</h3><br>
<br>
The Bluetooth library allows Mobile Processing sketches to send and receive data via Bluetooth wireless networks.<br>
<br>
Bluetooth is a radio standard for short-range "personal area networks." The standard includes communication protocols for discovering other devices and the software services running on those devices.<br>
<br>
Using this library, a Mobile Processing sketch running on a supported phone can connect to other Bluetooth devices as well as act as a service that other devices can connect to.
</div>
<div class="column" style="padding-left: 40px">
<b>Bluetooth</b><br>
This class provides the primary interface for discovering and establishing a Bluetooth network connection.<br>
<br>
<a href="reference.php?name=Bluetooth">Bluetooth</a><br>
<a href="reference.php?name=Bluetooth_cancel">cancel()</a><br>
<a href="reference.php?name=Bluetooth_discover">discover()</a><br>
<a href="reference.php?name=Bluetooth_start">start()</a><br>
<a href="reference.php?name=Bluetooth_stop">stop()</a><br>
<br>
<a href="reference.php?name=Bluetooth_EVENT_DISCOVER_DEVICE">EVENT_DISCOVER_DEVICE</a><br>
<a href="reference.php?name=Bluetooth_EVENT_DISCOVER_DEVICE_COMPLETED">EVENT_DISCOVER_DEVICE_COMPLETED</a><br>
<a href="reference.php?name=Bluetooth_EVENT_DISCOVER_SERVICE">EVENT_DISCOVER_SERVICE</a><br>
<a href="reference.php?name=Bluetooth_EVENT_DISCOVER_SERVICE_COMPLETED">EVENT_DISCOVER_SERVICE_COMPLETED</a><br>
<a href="reference.php?name=Bluetooth_EVENT_CLIENT_CONNECTED">EVENT_CLIENT_CONNECTED</a><br>
<br>
<br>
<b>Device</b><br>
Objects of this class represent nearby devices discovered on the Bluetooth network.<br>
<br>
<a href="reference.php?name=Device">Device</a><br>
<a href="reference.php?name=Device_address">address()</a><br>
<a href="reference.php?name=Device_cancel">cancel()</a><br>
<a href="reference.php?name=Device_discover">discover()</a><br>
<a href="reference.php?name=Device_name">name()</a><br>
<br>
<br>
<b>Service</b><br>
Objects of this class represent software running on devices that can be connected to via the Bluetooth network.<br>
<br>
<a href="reference.php?name=Service">Service</a><br>
<a href="reference.php?name=Service_connect">connect()</a><br>
<a href="reference.php?name=Service_description">description()</a><br>
<a href="reference.php?name=Service_name">name()</a><br>
<a href="reference.php?name=Service_provider">provider()</a><br>
<br>
<br>
<b>Client</b><br>
Client objects are used to communicate with other devices and services.<br>
<br>
<a href="reference.php?name=Client">Client</a><br>
<a href="reference.php?name=Client_read">read()</a><br>
<a href="reference.php?name=Client_readBoolean">readBoolean()</a><br>
<a href="reference.php?name=Client_readBytes">readBytes()</a><br>
<a href="reference.php?name=Client_readChar">readChar()</a><br>
<a href="reference.php?name=Client_readInt">readInt()</a><br>
<a href="reference.php?name=Client_readUTF">readUTF()</a><br>
<a href="reference.php?name=Client_skipBytes">skipBytes()</a><br>
<a href="reference.php?name=Client_stop">stop()</a><br>
<a href="reference.php?name=Client_write">write()</a><br>
<a href="reference.php?name=Client_writeBoolean">writeBoolean()</a><br>
<a href="reference.php?name=Client_writeChar">writeChar()</a><br>
<a href="reference.php?name=Client_writeInt">writeInt()</a><br>
<a href="reference.php?name=Client_writeUTF">writeUTF()</a><br>
</div>
<?php
require '../../../footer.inc.php';
?>
