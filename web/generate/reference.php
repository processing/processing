<?

require('../config.php');
require('lib/Ref.class.php');
require('lib/Translation.class.php');
$benchmark_start = microtime_float();

// arguments
$lang = isSet($_POST['lang']) ? $_POST['lang'] : 'en';

// get translation file
$translation = new Translation($lang);

// get reference files for the language
$files = getRefFiles($lang);

// create Ref objects for each file
foreach ($files as $file) {
     $refs[] = new Ref("api_$lang/".$file);
}

// create ReferencePage object
$count = 0;
foreach ($refs as $ref) {
    $page = new ReferencePage($ref, $translation, $lang);
    $page->write();
    $count++;
}

$benchmark_end = microtime_float();
$execution_time = round($benchmark_end - $benchmark_start, 4);

?>

<h2>Reference Generation Successful</h2>
<p>Generated <?=$counter?> files in <?=$execution_time?> seconds.</p>