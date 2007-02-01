<?php
require_once '../db.inc.php';

$PAGE_TITLE = "Phones \ Mobile Processing";

require '../header.inc.php';
?>
<img src="images/header.png"><br />
<br />
<br />
<?php
//// get the latest 8 phones
$link = db_connect();
$query = "SELECT DISTINCT useragent, stamp FROM profile_summary ORDER BY stamp DESC LIMIT 8";
$result = mysql_query($query);
while ($data = mysql_fetch_assoc($result)) {
    echo $result['useragent'] ."<br />";
}
?>
&nbsp;
<?php
require '../footer.inc.php';
?>
