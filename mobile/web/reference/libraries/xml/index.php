<?php

$PAGE_TITLE = "XML &raquo; Libraries &raquo; Mobile Processing";

$PAGE_SHOWBACKINDEX = true;
$PAGE_BACK_LINK = '../index.php';
$PAGE_BACK_NAME = 'Libraries';

require '../../../header.inc.php';
?>
<div class="column">
<h3>XML</h3><br>
<br>
The XML library allows Mobile Processing sketches to parse and out XML documents. It is based on a combination of the <a href="http://www.texone.org/proxml/">proXML</a> library and the <a href="http://kxml.sourceforge.net/">KXML2</a> open source library.
</div>
<div class="column" style="padding-left: 40px">
<b>XMLElement</b><br>
This class represents a node in an XML document.<br>
<br>
<a href="<?php echo reflink("XMLElement") ?>">XMLElement</a><br>
<br>
<br>
<b>XMLParser</b><br>
This class is used to parse XML documents.<br>
<br>
<a href="<?php echo reflink("XMLParser") ?>">XMLParser</a><br>
<a href="<?php echo reflink("XMLParser_parse") ?>">parse()</a><br>
<a href="<?php echo reflink("XMLParser_start") ?>">start()</a><br>
<a href="<?php echo reflink("XMLParser_stop") ?>">stop()</a><br>
<a href="<?php echo reflink("XMLParser_attribute") ?>">attribute()</a><br>
<br>
<a href="<?php echo reflink("XMLParser_EVENT_TAG_START") ?>">EVENT_TAG_START</a><br>
<a href="<?php echo reflink("XMLParser_EVENT_TEXT") ?>">EVENT_TEXT</a><br>
<a href="<?php echo reflink("XMLParser_EVENT_TAG_END") ?>">EVENT_TAG_END</a><br>
<a href="<?php echo reflink("XMLParser_EVENT_DOCUMENT_END") ?>">EVENT_DOCUMENT_END</a>
</div>
<?php
require '../../../footer.inc.php';
?>
