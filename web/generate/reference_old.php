<?

require('../config.php');
require('lib/Ref.class.php');
$benchmark_start = microtime_float();

$languages = isSet($_POST['lang']) ? explode(',', $_POST['lang']) : $languages;

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
// create Ref objects for each file
$ref_objs = array();
foreach ($files as $lang => $array) {
    foreach ($array as $file) {
        echo "parsing api_$lang/$file<br/>";
        $ref_objs[$lang][] = new Ref("api_$lang/".$file);    }
}

echo 'applying templates and writing files<br/>';
// apply templates to each Ref object and save static file
$counter = 0;
foreach ($ref_objs as $lang => $array) {
    foreach ($array as $obj) {
        // put file in language folder if not english
        $filepath = REFERENCEDIR . ($lang == 'en' ? '' : "$lang/") . $obj->name();
        
        // make template
        $page = new Page($obj->title() . ($lang == 'en' ? '' : " \ $LANGUAGES[$lang][0]") .' \ Language (API)', 'Language');
        $page->subtemplate('template.reference.item.html');
        $page->content($obj->display()); 
        $page->language($lang);
        
        // save file
        if ($fp = fopen($filepath, 'w+')) {
            fwrite($fp, $page->out());
            $counter++;
        } else {
            echo 'could not open file to write';
        }
    }
}

$benchmark_end = microtime_float();
$execution_time = round($benchmark_end - $benchmark_start, 4);

?>

<h2>Reference Generation Successful</h2>
<p>Generated <?=$counter?> files in <?=count($languages)?> languages</p>
<p>Generator took <?=$execution_time?> seconds to execute</p>