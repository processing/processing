<?php

$PAGE_TITLE = "Language (API) \ Mobile Processing";
require_once '../header.inc.php';
require_once 'generate.inc.php';

?>
<img src="images/header.png"><br>
<br>
<br>
<div class="column">
<?php 
foreach ($categories as $cat => $entry) { 
    if (array_search($cat, $columns) !== false) {
?>
</div>
<div class="column">
<?php     
} 
?>
    <img src="images/<?php echo strtolower(str_replace('/', '', $cat)) ?>.gif"><br>
<br>
<?php     
    foreach ($entry as $subcat => $e) {
        if (count($e) > 0) {
            asort($e);
            if ($subcat != '') {
?>
    <i><?php echo $subcat ?></i><br>
<?php
            }
            foreach ($e as $se) {
                $filename = strtok($se, "\n");
                $displayname = strtok("\n");
?>
    <a href="<?php echo reflink($filename) ?>"><?php echo $displayname ?></a><br>
<?php
            }
?>
    <br>
<?php
        }
    }
?>
    <br>
    <br>
<?php
} 
?>
</div>
<?php
require '../footer.inc.php';
?>
