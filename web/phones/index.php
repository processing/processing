<?php
require_once '../db.inc.php';

require_once '../lib/tera-wurfl/tera_wurfl.php';

$PAGE_TITLE = "Phones \ Mobile Processing";

require '../header.inc.php';
?>
<img src="images/header.png"><br />
<br />
<br />
<div class="columnpadded">
Mobile Processing sketches run on most mobile phones that support Java games and applications.<br />
<br />
<b>To test your phone:</b><br />
<br />
<ol>
<li>Go to <b>wapmp.at</b> in your phone's mobile web browser.*<br /><br /></li>
<li>Choose <b>My Phone</b> from the main menu.<br /><br /></li>
<li>Download a <b>Profiler</b> sketch.<br /><br /></li>
<li>Run the Profiler and choose <b>Share Results</b> from the menu to upload the results to this website.<br /><br /></li>
<li>On success, refresh this page. Recent results are displayed on the right.</li>
</ol>
<br />
<br />
<br />
<span style="font-size: smaller">* The Profiler must be downloaded on your phone in order to properly identify your device upon submission. If you download the Profiler on your computer and transfer it to your phone, your submission will not appear in the list.</span>
</div>
<div class="column2x">
<table width="480" cellspacing="0" cellpadding="0" border="0">
  <tr>
<?php
//// get the latest 20 submissions
$link = db_connect();
$query = "SELECT id, useragent, stamp FROM profile_summary ORDER BY stamp DESC LIMIT 20";
$result = mysql_query($query);
$wurfl = new tera_wurfl();
$devices = array();
$count = 0;
while ($data = mysql_fetch_assoc($result)) {
  //// look up each one in wurfl
  if ($wurfl->getDeviceCapabilitiesFromAgent($data['useragent'])) {
    if ($wurfl->capabilities['product_info']['is_wireless_device']) {
      $name = $wurfl->brand .' '. $wurfl->model;
      if (is_null($devices[$name])) {
?>
  <td width="160" align="center">
      <a href="profile.php?id=<?php echo $data['id'] ?>"><image border="0" src="../lib/tera-wurfl/<?php echo $wurfl->device_image ?>"></a><br />
      <b><a href="profile.php?id=<?php echo $data['id'] ?>"><?php echo $name ?></a></b><br />
      <br />
      <br />
      <br />
      <br />
  </td>
<?php
        $devices[$name] = $data['useragent'];
        $count++;
        if (($count % 3) == 0) {
          echo '</tr><tr>';
        }
      }
    }
  }
  if ($count == 15) {
    break;
  }
}
for ($i = $count % 3; ($i % 3) != 0; $i++) {
    echo '<td width="160">&nbsp;</td>';
}
?>
  </tr>
</table>
<p align="right"><b><a href="list.php">Browse all submissions...</a></b></p>
</div>
</table>
<?php
require '../footer.inc.php';
?>
