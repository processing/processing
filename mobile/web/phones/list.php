<?php
require_once '../db.inc.php';

require_once '../lib/tera-wurfl/tera_wurfl.php';

$PAGE_TITLE = "Phones \ Mobile Processing";

require '../header.inc.php';
?>
<img src="images/header.png"><br />
<br />
<br />
<a href="index.php">1</a> \ 2<br />
<br />
<?php
$link = db_connect();
$query = "SELECT DISTINCT id, useragent, stamp FROM profile_summary ORDER BY useragent, stamp DESC";
$result = mysql_query($query);
$devices = array();
$unrecognized = array();
$count = 0;
$brand = NULL;
while ($data = mysql_fetch_assoc($result)) {
  //// look up each one in wurfl
  $wurfl = new tera_wurfl();
  if ($wurfl->GetDeviceCapabilitiesFromAgent($data['useragent'])) {
    if ($wurfl->capabilities['product_info']['is_wireless_device']) {
      if (($brand != NULL) && (strcmp($brand, $wurfl->brand) != 0)) {
        echo "<br />";
      }
      $brand = $wurfl->brand;
      $name = $brand .' '. $wurfl->model;
      if (is_null($devices[$name])) {
?>
      <a href="profile.php?id=<?php echo $data['id'] ?>&list=1"><?php echo $name ?></a><br />
<?php
        $devices[$name] = $data['useragent'];
        $count++;
      }
    }
  } else {
    if (array_search($data['useragent'], $unrecognized) === FALSE) {
      $unrecognized[] = $data['useragent'];
    }
  }
}
?>
<br />
<br />
<b>Total:</b> <?php echo $count ?> phones<br />
<br />
<br />
<?php
if (count($unrecognized) > 0) {
?>
Unrecognized Phones:<br />
<?php
  for ($i = 0; $i < count($unrecognized); $i++) {
    echo $unrecognized[$i] . "<br />";
  }
}
?>
<?php
require '../footer.inc.php';
?>
