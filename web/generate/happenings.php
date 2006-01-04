<?

require_once('../config.php');
require_once('lib/Happening.class.php');

function get_happenings($num = 5)
{
    // open and parse happenings.xml
    $xml =& openXML('happenings.xml');
    
    // get item nodes
    $items = $xml->getElementsByTagName('item');
    $items = $items->toArray();
    $items = array_reverse($items);
    
    // create Happening objects
    $i = 1;
    foreach ($items as $item) {
        $happenings[] = new Happening($item);
        if ($i >= $num && $num != 'all') { break; }
        $i++;
    }
    
    // output html
    $html = '';
    $html .= '<dl>';
    foreach ($happenings as $happening) {
        $html .= $happening->display();
    }
    $html .= '</dl>';
    return $html;
}

if (!defined('COVER')) {
    $benchmark_start = microtime_float();

    $page = new Page('Happenings', 'Happenings');
    $page->subtemplate('template.happenings.html');
    $page->content(get_happenings('all'));
    writeFile("happenings.html", $page->out());
    
    $benchmark_end = microtime_float();
    $execution_time = round($benchmark_end - $benchmark_start, 4);
    
    if (!defined('SUBMIT')) {
        echo <<<EOC
<h2>Happenings.html Generation Successful</h2>
<p>Generator took $execution_time seconds to execute</p>
EOC;
    }
}

?>