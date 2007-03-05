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
To test your phone:<br />
<ol>
<li>Go to <b>wapmp.at</b> in your phone's mobile web browser.<br /><br /></li>
<li>Choose <b>My Phone</b> from the main menu.<br /><br /></li>
<li>Download the <b>Profiler</b> sketch.<br /><br /></li>
<li>Run the Profiler and choose <b>Share Results</b> from the menu to upload the results to this website.<br /><br /></li>
</div>
<div class="column2x">
<table width="100%" border="0" cellspacing="0" cellpadding="0">
<?php
//// get the latest 20 submissions
$link = db_connect();
$query = "SELECT useragent, stamp, bluetooth, image2, messaging, phone, sound, videoplayback, videosnapshot, xml, configuration, profiles FROM profile_summary, profile_libraries, profile_microedition WHERE profile_summary.id=profile_libraries.id AND profile_microedition.id=profile_summary.id ORDER BY stamp DESC LIMIT 20";
$result = mysql_query($query);
$wurfl = new tera_wurfl();
$devices = array();
while ($data = mysql_fetch_assoc($result)) {
  //// look up each one in wurfl
  if ($wurfl->getDeviceCapabilitiesFromAgent($data['useragent'])) {
    $name = $wurfl->brand .' '. $wurfl->model;
    if (is_null($devices[$name])) {
?>
  <tr>
    <td width="100" align="right">
      <image src="../lib/tera-wurfl/<?php echo $wurfl->device_image ?>"><br />
      <br />
      <br />
    </td>
    <td align="left" valign="top">
<?php
      echo '<b>'. $name .'</b><br />';
      echo $data['configuration'] .', '. $data['profiles'] .'<br />';
?>
    </td>
  </tr> 
<?php
      $devices[$name] = $data['useragent'];
    }
  }
}
?>
</div>
</table>
<?php
require '../footer.inc.php';
?>
