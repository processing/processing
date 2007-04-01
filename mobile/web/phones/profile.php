<?php

//// function for outputing lists of encodings
function html_encodings($encodings) {
  if (strcmp($encodings, "NULL") == 0) {
    $encodings = "None";
  } else {
    $encodings = str_replace("encoding=", "", $encodings);
    $encodings = str_replace(" ", "<br />", $encodings);
  }

  return $encodings;
}

require_once '../db.inc.php';

require_once '../lib/tera-wurfl/tera_wurfl.php';

$PAGE_TITLE = "Phones \ Mobile Processing";
$PAGE_SHOWBACKINDEX=true;
if ($_GET['list']) {
  $PAGE_BACK_LINK="list.php";
}
require '../header.inc.php';

$link = db_connect();
$query = "SELECT *, DATE_FORMAT(stamp, '%e %b %Y') AS fmt_stamp FROM profile_summary WHERE id={$_GET['id']}";
$result = mysql_query($query);
$wurfl = new tera_wurfl();
$summary = mysql_fetch_assoc($result);
mysql_free_result($result);
$name = $summary['useragent'];
if ($wurfl->getDeviceCapabilitiesFromAgent($summary['useragent'])) {
  $name = $wurfl->brand .' '. $wurfl->model;
}
?>
<table border="0" cellspacing="0" cellpadding="0">
  <tr>
    <td width="75" valign="top">
<image border="0" src="../lib/tera-wurfl/<?php echo $wurfl->device_image ?>">
    </td>
    <td valign="top">
<h3><?php echo $name ?></h3>
<br />
<br />
<table border="0" cellspacing="0" cellpadding="0">
<?php
$query = "SELECT * FROM profile_microedition WHERE id={$_GET['id']}";
$result = mysql_query($query);
$microedition = mysql_fetch_assoc($result);
mysql_free_result($result);
?>
  <tr>
    <td class="proffieldheader">Configuration</td>
    <td class="proffield"><?php echo $microedition['configuration'] ?></td>
  </tr>
  <tr>
    <td class="proffieldheader">Profiles</td>
    <td class="proffield"><?php echo $microedition['profiles'] ?></td>
  </tr>
<?php
$query = "SELECT * FROM profile_display WHERE id={$_GET['id']}";
$result = mysql_query($query);
$display = mysql_fetch_assoc($result);
mysql_free_result($result);
?>
  <tr>
    <td class="proffieldheader">Screen (w x h)</td>
    <td class="proffield"><?php echo $display['width'] ?> x <?php echo $display['height'] ?></td>
  </tr>
  <tr>
    <td class="proffieldheader">Fullscreen (w x h)</td>
    <td class="proffield"><?php echo $display['fullWidth'] ?> x <?php echo $display['fullHeight'] ?></td>
  </tr>
  <tr>
    <td class="proffieldheader">Colors</td>
    <td class="proffield"><?php echo $display['colors'] ?></td>
  </tr>
  <tr>
    <td class="proffieldheader">Alpha (levels)</td>
    <td class="proffield"><?php echo $display['alpha'] ?></td>
  </tr>
<?php
$query = "SELECT * FROM profile_libraries WHERE id={$_GET['id']}";
$result = mysql_query($query);
$libraries = mysql_fetch_assoc($result);
mysql_free_result($result);
?>
  <tr>
    <td class="proffieldheader">Library compatibility</td>
    <td class="proffield">
      <table border="0" cellspacing="0" cellpadding="0">
        <tr>
          <td class="subfieldheader">Bluetooth</td>
          <td class="subfield"><?php echo $libraries['bluetooth'] != 0 ? "yes" : "no"  ?></td>
        </tr>        
        <tr>
          <td class="subfieldheader">Image2</td>
          <td class="subfield"><?php echo $libraries['image2'] != 0 ? "yes" : "no"  ?></td>
        </tr>        
        <tr>
          <td class="subfieldheader">Messaging</td>
          <td class="subfield"><?php echo $libraries['messaging'] != 0 ? "yes" : "no"  ?></td>
        </tr>
        <tr>
          <td class="subfieldheader">Phone</td>
          <td class="subfield"><?php echo $libraries['phone'] != 0 ? "yes" : "no"  ?></td>
        </tr>        
        <tr>
          <td class="subfieldheader">Sound</td>
          <td class="subfield"><?php echo $libraries['sound'] != 0 ? "yes" : "no"  ?></td>
        </tr>        
        <tr>
          <td class="subfieldheader">Video (playback)</td>
          <td class="subfield"><?php echo $libraries['videoplayback'] != 0 ? "yes" : "no"  ?></td>
        </tr>        
        <tr>
          <td class="subfieldheader">Video (snapshot)</td>
          <td class="subfield"><?php echo $libraries['videosnapshot'] != 0 ? "yes" : "no"  ?></td>
        </tr>        
        <tr>
          <td class="subfieldheader">XML</td>
          <td class="subfield"><?php echo $libraries['xml'] != 0 ? "yes" : "no"  ?></td>
        </tr>        
      </table>
    </td>
  </tr>
<?php
$query = "SELECT * FROM profile_mmapi WHERE id={$_GET['id']}";
$result = mysql_query($query);
$mmapi = mysql_fetch_assoc($result);
mysql_free_result($result);
?>
  <tr>
    <td class="proffieldheader">Multimedia</td>
    <td class="proffield">
      <table border="0" cellspacing="0" cellpadding="0">
        <tr>
          <td class="subfieldheader">MMAPI Version</td>
          <td class="subfield"><?php echo $mmapi['version'] ?></td>
        </tr>        
        <tr>
          <td class="subfieldheader">Supports Mixing</td>
          <td class="subfield"><?php echo $mmapi['mixing'] != 0 ? "yes" : "no"  ?></td>
        </tr>        
        <tr>
          <td class="subfieldheader">Audio Capture</td>
          <td class="subfield"><?php echo $mmapi['audiocapture'] != 0 ? "yes" : "no"  ?></td>
        </tr>
        <tr>
          <td class="subfieldheader">Video Capture</td>
          <td class="subfield"><?php echo $mmapi['videocapture'] != 0 ? "yes" : "no"  ?></td>
        </tr>        
        <tr>
          <td class="subfieldheader">Recording</td>
          <td class="subfield"><?php echo $mmapi['recording'] != 0 ? "yes" : "no"  ?></td>
        </tr>        
        <tr>
          <td class="subfieldheader">Video Encodings</td>
          <td class="subfield"><?php echo html_encodings($mmapi['videoencodings']) ?></td>
        </tr>        
        <tr>
          <td class="subfieldheader">Audio Encodings</td>
          <td class="subfield"><?php echo html_encodings($mmapi['audioencodings']) ?></td>
        </tr>        
        <tr>
          <td class="subfieldheader">Snapshot Encodings</td>
          <td class="subfield"><?php echo html_encodings($mmapi['snapencodings']) ?></td>
        </tr>        
        <tr>
          <td class="subfieldheader">Streaming</td>
          <td class="subfield"><?php echo html_encodings($mmapi['streamable']) ?></td>
        </tr>        
      </table>
    </td>
  </tr>
  <tr>
    <td class="proffieldheader">Supported Time Zones</td>
    <td class="proffield">
      <form action="">
        <select name="timezones">
<?php 
$timezones = split("(\ |\n)", $summary['timezones']);
foreach ($timezones as $tz) {
  if ($tz != "") {
?>
          <option value=""><?php echo $tz ?></option>
<?php
  }
}
?>
        </select>
      </form>
    </td>
  </tr>
  <tr>
    <td class="proffieldheader">Submissions</td>
    <td class="proffield">
      <table border="0" cellspacing="0" cellpadding="0">
        <tr>
          <td class="subfieldheader"><?php echo $summary['fmt_stamp'] ?></td>
          <td class="subfield"><?php echo $summary['useragent'] ?></td>
        </tr>        
<?php
$prefix = substr($summary['useragent'], 0, strpos($summary['useragent'], '/'));
$query = "SELECT id, useragent, stamp, DATE_FORMAT(stamp, '%e %b %Y') AS fmt_stamp FROM profile_summary WHERE useragent LIKE '{$prefix}/%' AND id <> {$summary[id]} ORDER BY stamp DESC";
$result = mysql_query($query);
while ($submission = mysql_fetch_assoc($result)) {
?>
        <tr>
          <td class="subfieldheader"><?php echo $submission['fmt_stamp'] ?></td>
          <td class="subfield"><a href="profile.php?id=<?php echo $submission['id'] ?>&list=<?php echo $_GET['list'] ?>"><?php echo $submission['useragent'] ?></a></td>
        </tr>     
<?php
}
?>
      </table>
    </td>
  </tr>
</table>
    <td>
  </tr>
</table>
<?php
require '../footer.inc.php';
?>
