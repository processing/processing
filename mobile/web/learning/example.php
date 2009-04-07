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
<table width="100%" border="0" cellspacing="0" cellpadding="0">
  <tr>
    <td valign="top">
<?php
if (file_exists('examples/'. $name .'/'. $name .'.png')) {
?>
      <img src="examples/<?php echo $name ?>/<?php echo $name ?>.png">    
<?php
} else {
?>
<applet code="org.microemu.applet.Main"
        width="296" height="621"
        archive="../microemu-javase-applet.jar,../microemu-device-large.jar,examples/<?php echo $name ?>/midlet/<?php echo $name ?>.jar">
    <param name="midlet" value="<?php echo $name ?>">
    <param name="device" value="org/microemu/device/large/device.xml">
</applet>
<?php
}
?>
<br>
<br>
Code:<br>
<a href="examples/<?php echo $name ?>/<?php echo $name ?>.pde"><?php echo $name ?>.pde</a><br>
<?php
$dirhandle = opendir("examples/{$name}");
while (false !== ($filename = readdir($dirhandle))) {
  if ((strpos($filename, '.pde') > 0) &&
      (strpos($filename, "{$name}.pde") === false)) {
?>
<a href="examples/<?php echo $name ?>/<?php echo $filename ?>"><?php echo $filename ?></a><br />
<?php
  }
}
?>
<br>
Download:<br>
<a href="examples/<?php echo $name ?>/midlet/<?php echo $name ?>.jad"><?php echo $name ?>.jad</a><br>
<a href="examples/<?php echo $name ?>/midlet/<?php echo $name ?>.jar"><?php echo $name ?>.jar</a><br>
<br>
Obfuscated:<br>
<a href="examples/<?php echo $name ?>/proguard/<?php echo $name ?>.jad"><?php echo $name ?>.jad</a><br>
<a href="examples/<?php echo $name ?>/proguard/<?php echo $name ?>.jar"><?php echo $name ?>.jar</a><br>
    </td>
    <td width="10">&nbsp;&nbsp;&nbsp;&nbsp;</td>
    <td valign="top">
<pre><?php echo htmlentities($pde) ?></pre>
    </td>
  </tr>
</table>
<?php
 require '../footer.inc.php';
?>