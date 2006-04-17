<?

define('COVER', true);
require('updates.php');
require('../templates/rss.php');

$benchmark_start = microtime_float();

$rss = new RSS('Processing.org Updates', 'Updates and News about Processing programming project and Processing.org');
$rss->set_items(get_updates_rss('10'));
writeFile("updates.xml", $rss->out());

$benchmark_end = microtime_float();
$execution_time = round($benchmark_end - $benchmark_start, 4);

echo <<<EOC
<h2>Updates.xml Generation Successful</h2>
<p>Generator took $execution_time seconds to execute</p>
EOC;

?>