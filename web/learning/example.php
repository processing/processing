<?php

$name = $_GET['name'];

if (is_null($name)) {
    header('Location: index.php');
    return;
}

//// get contents of pde file
$pde = file_get_contents('examples/'. $name .'/'. $name .'.pde');

//// parse first comment line as title of sketch
$title = "(Untitled)";
if (preg_match('/\/\/ *([^\n]*)/', $pde, $matches) != 0) {
    $title = $matches[1];
}

$PAGE_TITLE = $title .' &raquo; Examples &raquo; Mobile Processing';
$PAGE_SHOWBACKINDEX = true;

require '../header.inc.php';
?>
<h3><?php echo $title ?></h3>
<br>
<table border="0" cellspacing="0" cellpadding="0">
  <tr>
    <td valign="top">
<applet code="com.barteo.emulator.applet.Main"
        width="260" height="509"
        archive="../me-applet.jar,../large.jar,examples/<?php echo $name ?>/midlet/<?php echo $name ?>.jar">
    <param name="midlet" value="<?php echo $name ?>">
    <param name="device" value="net.barteo.me.device.large.LargeDevice">
</applet>
    </td>
    <td valign="top">
<pre><?php echo htmlentities($pde) ?></pre>
    </td>
  </tr>
</table>
<?php
 require '../footer.inc.php';
?>