<?

require('../config.php');
$benchmark_start = microtime_float();

// arguments
$lang = isSet($_POST['lang']) ? $_POST['lang'] : 'en';

// get translation file
$where = CONTENTDIR . 'api_' . $lang;

// look for the .subversion folder somewhere else
// otherwise will go looking for /home/root/.subversion or some other user
putenv('HOME=' . CONTENTDIR);

// do the initial checkout
//`cd /var/www/processing && /usr/local/bin/svn co svn://processing.org/trunk/web/content`;

// do things here
`cd $where && /usr/local/bin/svn update`;

$where = CONTENTDIR . 'static';

`cd $where && /usr/local/bin/svn update`;

$benchmark_end = microtime_float();
$execution_time = round($benchmark_end - $benchmark_start, 4);

?>

<h2>Updated <?=$where?> </h2>
<p>And it only took me <?=$execution_time?> seconds.</p>