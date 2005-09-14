<?

require('../config.php');
require('lib/Ref.class.php');
$benchmark_start = microtime_float();

$lang = isSet($_GET['lang']) ? $_GET['lang'] : 'en';

// get categories and subcategories
$xml =& openXML('reference_en.xml');

$cats = $xml->getElementsByTagName('cat');
$cats = $cats->toArray();
foreach ($cats as $cat) {
    $name = chars($cat->getAttribute('name'));
    $list[$name] = array();
    $list[$name][''] = array();
    $subs = $cat->getElementsByTagName('sub');
    $subs = $subs->toArray();
    foreach ($subs as $sub) {
        $list[$name][chars($sub->getAttribute('name'))] = array();
    }
}

// get array of reference files for each language
$files = array();

// set directory path
$dir = CONTENTDIR."api_$lang";
// open directory pointer
if ($dp = @opendir($dir)) {
    // iterate through file pointers
    while ($fp = readdir($dp)) {
        // add file pointer to array 
        if (strstr($fp, '.xml') && $fp != 'blank.xml') {
            $files[] = $fp;
        }
    }
} else {
    echo 'Could not open directory ' . $lang;
}

foreach ($files as $file) {
    $ref = new Ref("api_$lang/".$file);
    if ($ref->index()) {
        $list[$ref->category][$ref->subcategory][] = $ref;
        $alpha_list[] = $ref;
    }
}

function extended($var)
{
    return $var->level != 'Extended';
}

//$abridged = array_filter($list, 'extended');
//$abridged_alpha = array_filter($alpha_list, 'extended');

sort($alpha_list);
//sort($complete_alpha);

/*foreach ($abridged as $cat => $subs) {
    if (count($subs) > 0) {
        echo "<b>$cat</b><br/>";
        foreach ($subs as $sub => $refs) {
            if (count($refs) > 0) {
                if ($sub != '') echo "<i>$sub</i><br/>";
                foreach ($refs as $ref) {
                    echo "<a href=\"". $ref->name() ."\">{$ref->name}</a><br/>";
                }
            }
        }
    }
}*/

echo '<pre>';
print_r($alpha_list);
/*
$compare = strtolower(substr($abridged_alpha[0]->name, 1));;
foreach ($abridged_alpha as $ref) {
    if ($compare != strtolower(substr($ref->name, 1))) {
        $compare = strtolower(substr($ref->name, 1));
        echo '<br /><br />';
    }
    echo "<a href=\"". $ref->name() ."\">{$ref->name}</a><br/>";
}

/*

$count = 0;
$extcount = 0;
// get arrays by category and alphabetical for each language
$alpha = array();
foreach ($files as $lang => $array) {
    foreach ($array as $file) {
        $ref = new Ref("api_$lang/".$file);
        if (strtolower($ref->level) != 'extended' && $ref->index()) {
            $list[$ref->category][$ref->subcategory][] = $ref;
            $count++;
        }
        //$alpha[$lang][$ref->name][] =& $ref;
    }
}
?>
<style>div { width: 30%; margin-right: 2%; border: 1px solid #eee; float: left; }</style>
<?

// organize arrays, format links, and write files
echo '<div>';
$percolumn = round($count/3);
$i = 0;
foreach ($list as $cat => $subs) {
    if (count($subs) > 0) {
        echo "<h3>$cat</h3>";
        foreach ($subs as $sub => $items) {
            if ($sub != '' && count($items) > 0) {
                echo "<h5>$sub</h5>";
                foreach ($items as $item) {
                    $name = $item->name();
                    echo "<a href=\"$name\">{$item->name}</a><br/>";
                    $i++;
                }
            }
        }
        if ($i >= $percolumn) {
            echo '</div><div>';
            $i = 0;
        }
    }
}
echo '</div>';

*/
?>