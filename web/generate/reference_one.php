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
$file = isSet($_POST['file']) ? $_POST['file'] : 'abs.xml';

// create Ref objects for each file
$ref = new Ref("api_$lang/".$file);

// create ReferencePage object
$page = new ReferencePage($ref, $translation, $lang);
$page->write();

$benchmark_end = microtime_float();
$execution_time = round($benchmark_end - $benchmark_start, 4);

?>

<h2>Reference Generation Successful</h2>
<p>Generated <?=$file?> file in <?=$execution_time?> seconds.</p>
<p><a href="../<?=$page->filepath?>">View page</a></p>