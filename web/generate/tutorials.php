<?

require('../config.php');
$benchmark_start = microtime_float();

// make troubleshooting page
$source = CONTENTDIR."static/tutorials/";
$path = BASEDIR;


// update the files on the server via SVN

// look for the .subversion folder somewhere else
// otherwise will go looking for /home/root/.subversion or some other user
$where = CONTENTDIR . 'static/tutorials';
putenv('HOME=' . CONTENTDIR);

// do the initial checkout
//`cd /var/www/processing && /usr/local/bin/svn co svn://processing.org/trunk/web/content/`;

`cd $where && /usr/local/bin/svn update`;

// Copy over the images for the tutorials index
if (!is_dir($path.'learning/tutorials/imgs')) { mkdir($path.'learning/tutorials/imgs', '0757'); }
copydirr($source.'imgs', $path.'learning/tutorials/imgs', null, 0757, true);

$page = new Page("Tutorials", "Tutorials");
$page->content(file_get_contents($source."index.html"));
writeFile('learning/tutorials/index.html', $page->out());

$page = new Page("Processing in Eclipse", "Tutorials");
$page->content(file_get_contents($source."eclipse/index.html"));
writeFile('learning/tutorials/eclipse/index.html', $page->out());
if (!is_dir($path.'learning/tutorials/eclipse/imgs')) { mkdir($path.'learning/tutorials/eclipse/imgs', '0757'); }
copydirr($source.'eclipse/imgs', $path.'learning/tutorials/eclipse/imgs', null, 0757, true);

$page = new Page("Basics", "Tutorials");
$page->content(file_get_contents($source."basics/index.html"));
writeFile('learning/tutorials/basics/index.html', $page->out());
if (!is_dir($path.'learning/tutorials/basics/imgs')) { mkdir($path.'learning/tutorials/basics/imgs', '0757'); }
copydirr($source.'basics/imgs', $path.'learning/tutorials/basics/imgs', null, 0757, true);

$page = new Page("RGB Color", "Tutorials");
$page->content(file_get_contents($source."color/index.html"));
writeFile('learning/tutorials/color/index.html', $page->out());
if (!is_dir($path.'learning/tutorials/color/imgs')) { mkdir($path.'learning/tutorials/color/imgs', '0757'); }
copydirr($source.'color/imgs', $path.'learning/tutorials/color/imgs', null, 0757, true);

$page = new Page("Regular Polygon", "Tutorials");
$page->content(file_get_contents($source."regular_polygon/index.html"));
writeFile('learning/tutorials/regular_polygon/index.html', $page->out());
if (!is_dir($path.'learning/tutorials/regular_polygon/imgs')) { mkdir($path.'learning/tutorials/regular_polygon/imgs', '0757'); }
copydirr($source.'regular_polygon/imgs', $path.'learning/tutorials/regular_polygon/imgs', null, 0757, true);

$page = new Page("Trig", "Tutorials");
$page->content(file_get_contents($source."trig/index.html"));
writeFile('learning/tutorials/trig/index.html', $page->out());
if (!is_dir($path.'learning/tutorials/trig/imgs')) { mkdir($path.'learning/tutorials/trig/imgs', '0757'); }
copydirr($source.'trig/imgs', $path.'learning/tutorials/trig/imgs', null, 0757, true);

$benchmark_end = microtime_float();
$execution_time = round($benchmark_end - $benchmark_start, 4);

?>

<h2>Static page generation Successful</h2>
<h2>Updated <?=$where?> </h2>
<p>Generated files in <?=$execution_time?> seconds.</p>
<!--<p>Page put here: <?=$source."faq.html"?></p>-->
<!--<p>Page put here: <?=$path.'faq.html'?></p>-->
<p><?=$path.'learning/tutorials/eclipse/imgs'?></p>