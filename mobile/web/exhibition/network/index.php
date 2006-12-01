<?php

$PAGE_TITLE = "Network links \ Mobile Processing";
require '../../header.inc.php';
?>
<img src="../images/network.png"><br />
<br />
<a href="../submit.php"><img border="0" src="../images/addlink.png"></a><br />
<br />
<a href="../index.php">1</a> \
<?php

$pages = 0;

//// acquire read lock
$lockfp = fopen(dirname(__FILE__) . '/lockfile', 'r');
if ($lockfp !== FALSE) {
    if (flock($lockfp, LOCK_SH)) {
        //// read total number of pages
        $fp = fopen('generated/pagecount.txt', 'rb');
        $pages = fgets($fp);
        fclose($fp);


        //// generate page links
        $lastpage = $pages + 1;
        for ($i = 2; $i <= $lastpage; $i++) {
            if ($i != $_GET['page']) {
                echo "<a href=\"index.php?page={$i}\">";
            }
            echo $i;
            if ($i != $_GET['page']) {
                echo '</a>';
            }
            if ($i < $pages) {
                echo ' \ ';
            }
        }
        echo '<br /><br />';
        
        //// insert page of links
        require "generated/links.{$_GET['page']}.inc.php";

        //// release lock
        flock($lockfp, LOCK_UN);
    }
    fclose($lockfp);
}
?>
<?php
require '../../footer.inc.php';
?>
