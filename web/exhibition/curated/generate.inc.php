<?php

function format_page_link($data) {
    $line = <<<EOE
<a href="{$data['url']}" target="_new"><img src="{$data['imgurl']}"></a><br />
<br />
<table width="200" border="0" cellspacing="0" cellpadding="0">
  <tr>
    <td>
<b><a href="{$data['url']}" target="_new">{$data['title']}</a></b>
    </td>
  </tr>
  <tr height="5">
  </tr>
  <tr>
    <td>
<b>by {$data['name']}</b><br />
<br />
{$data['description']}<br />
<br />
<br />
    </td>
  </tr>
</table>
EOE;
    return $line;
}

function format_home_link($data) {
    $line = <<<EOE
<a href="{$data['url']}" target="_new"><img src="{$data['imgurl']}"></a><br />
<br />
<table width="200" border="0" cellspacing="0" cellpadding="0">
  <tr>
    <td>
<b><a href="{$data['url']}" target="_new">{$data['title']}</a></b>
    </td>
  </tr>
  <tr height="5">
  </tr>
  <tr>
    <td>
<b>by {$data['name']}</b>
    </td>
  </tr>
</table>
EOE;
    return $line;
}

function format_wappage_link($data) {
    $line = <<<EOE
EOE;
    return $line;
}

function format_waphome_link($data) {
    $line = <<<EOE
<img border="0" src="<?php echo get_sized_image("{$data['mobileimgurl']}") ?>" /><br />
<a href="ota.php?jad={$data['jadurl']}"><span class="smaller">{$data['title']}</span></a><br />
<span class="smaller">by {$data['name']}</span>
EOE;
    return $line;
}

function write_header($fp) {
    fwrite($fp, "<table border=\"0\" cellspacing=\"0\" cellpadding=\"0\">");
}

function write_startrow($fp) {
    fwrite($fp, "<tr><td valign=\"top\" width=\"224\">");
}

function write_midrow($fp) {
    fwrite($fp, "</td><td valign=\"top\" width=\"224\">");
}

function write_endrow($fp) {
    fwrite($fp, "</td></tr>");
}

function write_footer($fp) {
    fwrite($fp, "</table>");
}

function links_generate() {
    //// number of links to display on homepage
    $linksonhome = 2;
    //// number of links per page to display
    $linksperpage = 6;
    //// location of this script file, also location of generated files
    $dirname = dirname(__FILE__);

    $lockfp = fopen($dirname . '/lockfile', 'r');
    if ($lockfp !== FALSE) {
        if (flock($lockfp, LOCK_EX)) {
            $fp = fopen($dirname . '/generated/links.inc.php', 'wb');
            $homefp = fopen($dirname . '/generated/home.inc.php', 'wb');
            $waphomefp = fopen($dirname . '/generated/waphome.inc.php', 'wb');
            if ($fp !== FALSE) {
                $link = db_connect();
                $result = mysql_query("SELECT * FROM curated ORDER BY submitted DESC LIMIT {$linksperpage}");
                $count = 0;
                write_header($fp);
                while ($data = mysql_fetch_assoc($result)) {
                    if (($count % 2) == 0) {
                        write_startrow($fp);
                    } else {
                        write_midrow($fp);
                    }
                    $line = format_page_link($data);
                    fwrite($fp, $line);
                    if (($count % 2) == 1) {
                        write_endrow($fp);
                    }
                    //// handle homepage include
                    if ($count == 0) {
                        //// web home
                        $line = format_home_link($data);
                        fwrite($homefp, $line);
                        //// wap home
                        $line = format_waphome_link($data);
                        fwrite($waphomefp, $line);
                    } else if ($count == 1) {
                        //// web home
                        $line = format_home_link($data);
                        fwrite($homefp, "<br /><br />");
                        fwrite($homefp, $line);
                        //// wap home
                        $line = format_waphome_link($data);
                        fwrite($waphomefp, "<br /><br />");
                        fwrite($waphomefp, $line);
                    }

                    $count++;
                }
                if (($count % 2) == 1) {
                    write_midrow($fp);
                    write_endrow($fp);
                }
                write_footer($fp);
                fclose($homefp);
                fclose($waphomefp);
                mysql_free_result($result);

                $result = mysql_query("SELECT COUNT(*) FROM curated");
                $count = mysql_result($result, 0);
                mysql_free_result($result);
                //// add footer link to more pages, if necessary
                if ($count > $linksperpage) {
                    fwrite($fp, "<br /><a href=\"curated/index.php?page=2\">Exhibition continues</a>");
                }
                fclose($fp);

                //// calculate total number of pages, write to countfile
                $pages = ceil(($count - $linksperpage) / $linksperpage);
                $fp = fopen($dirname .'/generated/pagecount.txt', 'wb');
                fwrite($fp, $pages);
                fclose($fp);

                //// generate those additional pages
                $offset = $linksperpage;
                for ($i = 0; $i < $pages; $i++) {
                    //// query each page of links
                    $result = mysql_query("SELECT * FROM curated ORDER BY submitted DESC LIMIT {$offset}, {$linksperpage}");
                    //// generate the file
                    $page = $i + 2;
                    $fp = fopen("{$dirname}/generated/links.{$page}.inc.php", 'wb');

                    write_header($fp);
                    $count = 0;
                    while ($data = mysql_fetch_assoc($result)) {
                        if (($count % 2) == 0) {
                            write_startrow($fp);
                        } else {
                            write_midrow($fp);
                        }
                        $line = format_page_link($data);
                        fwrite($fp, $line);
                        if (($count % 2) == 1) {
                            write_endrow($fp);
                        }
                        $count++;
                    }
                    if (($count % 2) == 1) {
                        write_midrow($fp);
                        write_endrow($fp);
                    }
                    write_footer($fp);

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