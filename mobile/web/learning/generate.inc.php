<?php

//// need settings for SITE_ROOT constant
require_once "../settings.inc.php";

function format_example_link($data) {
    if (isset($data['subcategory']) && $data['subcategory'] != "") {
        $subcategory = "<i>". $data['subcategory'] ."</i><br />";
    } else {
        $subcategory ="";
    }
    $line = <<<EOE
<img src="images/{$data['category']}.png"><br />
<br />
{$subcategory}
<a href="example.php?name={$data['filename']}">{$data['name']}</a><br />
<br />
EOE;
    if (isset($data['showonhome'])) {
        $line = $line ."<br />Home preview:<br /><br />";
        $line = $line . format_home_link($data);
    }
    return $line;
}

function format_home_link($data) {
    $root = SITE_ROOT;
    $line = <<<EOE
<a href="{$root}learning/example.php?name={$data['filename']}"><img src="{$root}images/examples/{$data['filename']}.png">
</a>
EOE;
    return $line;
}

function write_header($fp) {
    fwrite($fp, "<table border=\"0\" cellspacing=\"0\" cellpadding=\"0\">");
}

function write_startrow($fp) {
    fwrite($fp, "<tr><td>");
}

function write_midrow($fp) {
    fwrite($fp, "</td><td width=\"1\"></td><td>");
}

function write_endrow($fp) {
    fwrite($fp, "</td></tr>");
}

function write_footer($fp) {
    fwrite($fp, "</table>");
}

function links_generate() {
    //// number of links to display on homepage
    $linksonhome = 9;
    //// location of this script file, also location of generated files
    $dirname = dirname(__FILE__);

    $lockfp = fopen($dirname . '/lockfile', 'r');
    if ($lockfp !== FALSE) {
        if (flock($lockfp, LOCK_EX)) {
            $fp = fopen($dirname . '/generated/links.inc.php', 'wb');
            $homefp = fopen($dirname . '/generated/home.inc.php', 'wb');
            $wapfp = fopen($dirname . '/generated/waplinks.inc.php', 'wb');
            if ($fp !== FALSE) {
                $link = db_connect();
                $result = mysql_query("SELECT * FROM examples ORDER BY submitted DESC");
                $count = 0;
                //// build up associative map of data, create home links
                $map = array();
                write_header($homefp);
                while ($data = mysql_fetch_assoc($result)) {
                    if (!isset($map[$data['category']])) {
                        $map[$data['category']] = array();
                    }
                    $map[$data['category']][$data['subcategory']][] = $data;

                    if (($data['showonhome']) && ($count < 9)) {
                        if (($count % 3) == 0) {
                            write_startrow($homefp);
                        } else {
                            write_midrow($homefp);
                        }
                        $line = format_home_link($data);
                        fwrite($homefp, $line);
                        if (($count % 3) == 2) {
                            write_endrow($homefp);
                        }
                        $count++;
                    }
                }
                if (($count % 3) != 0) {
                    for ($i = 0; $i < (3 - ($count % 3)); $i++) {
                        write_midrow($homefp);
                    }
                    write_endrow($homefp);
                }
                write_footer($homefp);
                fclose($homefp);
                mysql_free_result($result);

                //// now build examples page, columns in this order
                $order = array("form", "net", "input", "core", "color");
                $catspercol = ceil(count($order) / 3);
                fwrite($fp, "<div class=\"column\">");
                for ($i = 0; $i < count($order); $i++) {
                    $category = $order[$i];
                    fwrite($fp, "<img src=\"images/{$category}.png\"><br /><br />");
                    fwrite($wapfp, ucfirst($category) ."<br />");
                    foreach ($map[$category] as $subcategory => $examples) {
                        if ($subcategory != "") {
                            fwrite($fp, "<i>{$subcategory}</i><br />");
                            fwrite($wapfp, "<span class=\"smaller\">{$subcategory}</span><br />");
                        }
                        foreach ($examples as $e) {
                            fwrite($fp, "<a href=\"example.php?name={$e['filename']}\">{$e['name']}</a><br />");
                            fwrite($wapfp, "<a class=\"smaller\" href=\"ota.php?code=". (2*$e['id']+1) ."\">{$e['name']}</a><br />");
                        }
                        fwrite($fp, "<br />");
                        fwrite($wapfp, "<br />");
                    }
                    fwrite($fp, "<br /><br />");
                    if (($i > 0) && (($i % $catspercol) == 1)) {
                        fwrite($fp, "</div><div class=\"column\">");
                    }
                }
                fwrite($fp, "</div>");
                fclose($fp);
                fclose($wapfp);
            }
            flock($lockfp, LOCK_UN);
            fclose($lockfp);
        }
    }
}
?>
