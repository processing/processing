<?php

$PAGE_TITLE = "Mobile Processing &raquo; FAQ";

$qs = array();
$as = array();

$fp = fopen("general.txt", "r");
while (!feof($fp)) {
    $id = fgets($fp);
    if (feof($fp)) {
        break;
    }
    $q = fgets($fp);
    if (feof($fp)) {
        break;
    }
    $a = fgets($fp);
    $qs[$id] = $q;
    $as[$id] = $a;
}
fclose($fp);

require '../header.inc.php';
?>
<img src="images/header.png"><br>
<br>
<a name="top"></a>
<?php
foreach ($qs as $id => $q) {
    echo "<a href=\"#". $id ."\">". $q ."</a><br>\n";
}
?>
</div>
<div class="backnavigation">
  <table border="0" cellspacing="0" cellpadding="0">
<?php
foreach ($qs as $id => $q) {
?>
    <tr>
      <td align="right" valign="top" width="50">
        <a name="<?php echo $id ?>" href="#top">
          <img src="images/up_arrow_small.png" border="0" align="middle">
        </a>
      </td>
      <td valign="top">
        <b><?php echo $q ?></b><br>
        <br>
        <?php echo $as[$id] ?><br><br><br><br>
      </td>
    </tr>
<?php
}
?>
  </table>
<?php
require '../footer.inc.php';
?>
