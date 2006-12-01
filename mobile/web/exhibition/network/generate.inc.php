<?php

function format_link($data) {
    $submitted = date("d M 'y", intval($data['submitted']));
    $line = <<<EOE
<a href="{$data['url']}">{$data['title']}</a><br />
{$data['name']}<br />
<span class="date">{$submitted}</span><br />
<br />
EOE;
    return $line;
}

function links_generate() {
    //// number of links per column to display
    $linkspercolumn = 12;
    //// location of this script file, also location of generated files
    $dirname = dirname(__FILE__);

    $lockfp = fopen($dirname . '/lockfile', 'r');
    if ($lockfp !== FALSE) {
        if (flock($lockfp, LOCK_EX)) {
            $fp = fopen($dirname . '/links.inc.php', 'wb');
            if ($fp !== FALSE) {
                $link = db_connect();
                $result = mysql_query("SELECT * FROM links ORDER BY submitted DESC LIMIT {$linkspercolumn}");
                while ($data = mysql_fetch_assoc($result)) {
                    $line = format_link($data);
                    fwrite($fp, $line);
                }
                mysql_free_result($result);

                $result = mysql_query("SELECT COUNT(*) FROM links");
                $count = mysql_result($result, 0);
                mysql_free_result($result);
                //// add footer link to more pages, if necessary
                if ($count > $linkspercolumn) {
                    fwrite($fp, "<br /><a href=\"network/index.php?page=2\">Previous links</a>");
                }
                fclose($fp);

                //// calculate total number of pages, write to countfile
                $linksperpage = 3 * $linkspercolumn;
                $pages = ceil(($count - $linkspercolumn) / $linksperpage);
                $fp = fopen($dirname .'/pagecount.txt', 'wb');
                fwrite($fp, $pages);
                fclose($fp);

                //// generate those additional pages
                $offset = $linkspercolumn;
                for ($i = 0; $i < $pages; $i++) {
                    //// query each page of links
                    $result = mysql_query("SELECT * FROM links ORDER BY submitted DESC LIMIT {$offset}, {$linksperpage}");
                    //// generate the file
                    $page = $i + 2;
                    $fp = fopen("{$dirname}/links.{$page}.inc.php", 'wb');
                    fwrite($fp, '<div class="column">');

                    $count = 0;
                    while ($data = mysql_fetch_assoc($result)) {
                        $line = format_link($data);
                        fwrite($fp, $line);
                        $count++;

                        if ($count == $linkspercolumn) {
                            fwrite($fp, '</div><div class="column">');
                            $count = 0;
                        }
                    }

                    fwrite($fp, '</div>');
                    fclose($fp);
                    mysql_free_result($result);
                    $offset += $linksperpage;
                }
            }
            flock($lockfp, LOCK_UN);
            fclose($lockfp);
        }
    }
}
?>