<?
require('../config.php');
require('lib/Ref.class.php');

$language = isset($_GET['lang']) ? $_GET['lang'] : 'en';

$charset = 'utf-8';
if($language == 'zh') { $charset = "big5"; }         # Chinese Traditional if($language == 'zh-cn') { $charset = "GB2312"; }    # Chinese Simplifiedif($language == 'he') { $charset = "Windows-1255"; } # Hebrewif($language == 'ru') { $charset = "ISO-8859-5"; }   # Russianif($language == 'tr') { $charset = "ISO-8859-9"; }   # Turkishif($language == 'pl') { $charset = "ISO-8859-2"; }   # Polishif($language == 'jp') { $charset = "Shift_JIS"; }    # Japanese

echo '<meta http-equiv="Content-Type" content="text/html; charset='.$charset.'">';

$languages = array($language);

// get array of reference xml files (for each language)
$files = array();
echo 'reading xml files<br/>';
foreach ($languages as $lang) {
    // set directory path
    $dir = CONTENTDIR."api_$lang";
    // open directory pointer
    if ($dp = @opendir($dir)) {
        // iterate through file pointers
        while ($fp = readdir($dp)) {
            // points to relative paths
            if ($fp == '.' || $fp == '..') { continue; }
            // point to another directory
            if (is_dir($dir .'/'. $fp)) { continue; }
            // add file pointer to array 
            if (strstr($fp, '.xml') && $fp != 'blank.xml') {
                $files[$lang][] = $fp;
            }
        }
    } else {
        echo 'Could not open directory ' . $lang;
    }
}

echo 'parsing xml files<br/>';
$names = array();
// create Ref objects for each file
foreach ($files as $lang => $array) {
    foreach ($array as $file) {
        $ref = new Ref(CONTENTDIR."api_$lang/".$file); 
        $names[$ref->name] = $ref->name();
    }
}

echo '<table>';
foreach ($names as $name => $file) {
    echo '<tr>';
    echo "<td width=\"200\">$name</td>";
    echo "<td>$file</td>";
    echo "<td>" . convertToFilename($name, $lang == 'zh') . "</td>";
    echo '</tr>';
}
echo '</table>';

/*
echo '<table>';
foreach ($names as $name) {
    echo '<tr>';
    echo "<td width=\"200\">$name</td>";
    if (($s = convertToFilename($name)) != $name) {
        echo '<td>'.convertToFilename($name).'</td>';
    } else {
        echo '<td></td>';
    }
    echo '</tr>';
}
echo '</table>';
*/
?>