<?

require_once('../config.php');
require_once('lib/Update.class.php');
require_once('../templates/rss.php');

function get_updates($num = 5)
{
    // open and parse updates.xml
    $xml =& openXML('updates.xml');
    
    // get each item node
    $items = $xml->getElementsByTagName('item');
    $items = $items->toArray();
    $items = array_reverse($items);
    
    // create Update objects
    $i = 1;
    foreach ($items as $item) {
        $updates[] = new Update($item);
        if ($i >= $num && $num != 'all') { break; }
        $i++;
    }
    
    // output html
    $html = '<dl>';
    foreach ($updates as $update) {
        $html .= $update->display();
    }
    $html .= '</dl>';
    return $html;
}

function get_updates_rss($num = 5)
{
    // open and parse updates.xml
    $xml =& openXML('updates.xml');
    
    // get each item node
    $items = $xml->getElementsByTagName('item');
    $items = $items->toArray();
    $items = array_reverse($items);
    
    // create Update objects
    $i = 1;
    foreach ($items as $item) {
        $updates[] = new Update($item);
        if ($i >= $num && $num != 'all') { break; }
        $i++;
    }
    
    // output html
    $html = '';
    foreach ($updates as $update) {
        $html .= $update->display_rss();
    }
    return $html;
}

if (!defined('COVER')) {
    $benchmark_start = microtime_float();
    
    $page = new Page('Updates', 'Updates');
    $page->subtemplate('template.updates.html');
    $page->content(get_updates('all'));
    writeFile("updates.html", $page->out());

	$rss = new RSS_feed('Processing.org Updates', 'Updates and News about Processing programming project and Processing.org');
	$rss->set_items(get_updates_rss('10'));
	writeFile("updates.xml", $rss->out());
    
    $benchmark_end = microtime_float();
    $execution_time = round($benchmark_end - $benchmark_start, 4);
    
    echo <<<EOC
<h2>Updates.html Generation Successful</h2>
<p>Generator took $execution_time seconds to execute</p>
EOC;
}
?>