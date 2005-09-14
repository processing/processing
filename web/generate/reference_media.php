<?

require('../config.php');
$benchmark_start = microtime_float();

copydirr(CONTENTDIR.'api_media', REFERENCEDIR.'media');

$benchmark_end = microtime_float();
$execution_time = round($benchmark_end - $benchmark_start, 4);

?>

<h2>Reference Media Files copied</h2>
<p>Copied files in <?=$execution_time?> seconds.</p>