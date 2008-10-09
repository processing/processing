<?

require('../config.php');
$benchmark_start = microtime_float();


// make troubleshooting page
$source = CONTENTDIR."static/tutorials/";
$path = BASEDIR;

// update the files on the server via SVN

// look for the .subversion folder somewhere else
// otherwise will go looking for /home/root/.subversion or some other user
$where = CONTENTDIR . 'api_media';
putenv('HOME=' . CONTENTDIR);

// do the initial checkout
//`cd /var/www/processing && /usr/local/bin/svn co svn://processing.org/trunk/web/content/`;

`cd $where && /usr/local/bin/svn update`;


if (!is_dir(REFERENCEDIR.'media')) { mkdir(REFERENCEDIR.'media', '0757'); }
copydirr(CONTENTDIR.'api_media', REFERENCEDIR.'media', null, 0757, true);

$benchmark_end = microtime_float();
$execution_time = round($benchmark_end - $benchmark_start, 4);

?>

<h2>Reference Media Files copied</h2>
<p>Copied files in <?=$execution_time?> seconds.</p>