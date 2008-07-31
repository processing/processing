<?php

$PAGE_TITLE = "Exhibition";
$PAGE_HEADER = <<<EOF
<a href="index.php">Cover</a> \ Exhibition
EOF;
$PAGE_FOOTER = $PAGE_HEADER;

require_once 'header.inc.php';
$BANGO_REF = $BANGO_REF .'?page='. $_GET['page'];

$pages = 0;

//// acquire read lock
$lockfp = fopen($WEB_ROOT .'/exhibition/curated/lockfile', 'r');
if ($lockfp !== FALSE) {
    if (flock($lockfp, LOCK_SH)) {
        if (isset($_GET['page'])) {
            //// read total number of pages
            $fp = fopen($WEB_ROOT .'/exhibition/curated/generated/pagecount.txt', 'rb');
            $pages = fgets($fp);
            fclose($fp);

            //// generate page links
            $lastpage = $pages + 1;
            $links = "<a href=\"exh.php\">1</a> \ ";
            for ($i = 2; $i <= $lastpage; $i++) {
                if ($i != $_GET['page']) {
                    $links = $links ."<a href=\"exh.php?page={$i}\">";
                }
                $links = $links . $i;
                if ($i != $_GET['page']) {
                    $links = $links .'</a>';
                }
                if ($i < $pages) {
                    $links = $links .' \ ';
                }
            }
            echo $links .'<br /><br />';
            //// insert page of links
            require_once $WEB_ROOT .'/exhibition/curated/generated/waplinks.'. $_GET['page'] .'.inc.php';
            echo '<br /><br />'. $links;
        } else {
            require_once $WEB_ROOT .'/exhibition/curated/generated/waplinks.inc.php';
        }

        //// release lock
        flock($lockfp, LOCK_UN);
    }
    fclose($lockfp);
}
require_once 'footer.inc.php';
?>
