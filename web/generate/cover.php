<?

require_once('../config.php');
DEFINE('COVER', true);
//require_once('updates.php');
//if (!defined('SUBMIT')) {
//    require_once('happenings.php');
//    require_once('courses.php');
//}
require_once('exhibition.php');


$benchmark_start = microtime_float();

$page = new Page('', 'Cover');
$page->subtemplate('template.cover.html');
//$page->set('updates', get_updates(24));
//$page->set('happenings', get_happenings(5));
//$page->set('courses', get_courses_short(5));
$page->set('exhibition', get_curated_short(2));
writeFile("index.php", $page->out());
    
$benchmark_end = microtime_float();
$execution_time = round($benchmark_end - $benchmark_start, 4);

if (!defined('SUBMIT')) {
    echo <<<EOC
<h2>Index.php Generation Successful</h2>
<p>Generator took $execution_time seconds to execute</p>
EOC;
}
?>