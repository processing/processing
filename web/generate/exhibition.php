<?

define('CURATED_PER_PAGE', 12);
define('NETWORK_FIRST_PAGE', 25);
define('NETWORK_PER_PAGE', 90);

if (!defined('SUBMIT')) {
    require('../config.php');
}
require(GENERATEDIR.'lib/Curated.class.php');
require(GENERATEDIR.'lib/Network.class.php');

// update the files on the server via SVN

// look for the .subversion folder somewhere else
// otherwise will go looking for /home/root/.subversion or some other user
$source = CONTENTDIR;
$path = BASEDIR;
$where = CONTENTDIR;
$there = CONTENTDIR;
putenv('HOME=' . CONTENTDIR);

// do the initial checkout
//`cd /var/www/processing && /usr/local/bin/svn co svn://processing.org/trunk/web/content/`;

`cd $there && /usr/local/bin/svn update curated.xml`;


/******************************************** CURATED ***/

function get_curated($curated, $start = 0, $num = 12)
{
    // output html
    $html = '<table width="448" cellspacing="0" cellpadding="0" border="0">';
    $j = 0;
    for ($i = $start; $i < $start+$num; $i++) {
        if ($curated[$i]) {
            if ($j % 2 == 0) $html .= '<tr>';
            $html .= '<td>' . $curated[$i]->display() . '</td>';
            if ($j % 2 != 0) $html .= '</tr>';
            $j++;
        }
    }
    if ($j % 2 != 0) $html .= '<td>&nbsp;</td></tr>';
    return $html . '</table>';
}

function get_curated_one($curated, $start = 0, $num = 12)
{
    // output html
    $html = '<table width="224" cellspacing="0" cellpadding="0" border="0">';
    $j = 0;
    for ($i = $start; $i < $start+$num; $i++) {
      if ($curated[$i]) {
        $html .= '<tr>';
	$html .= '<td>' . $curated[$i]->display() . '</td>';
        $html .= '</tr>';
        $j++;
      }
    }
    // if ($j % 2 != 0) $html .= '<td>&nbsp;</td></tr>';
   return $html . '</table>';
}

function get_curated_three($curated, $start = 0, $num = 12)
{
  // output html                                                                                                          
  $html = '<table width="672" cellspacing="0" cellpadding="0" border="0">';
  $j = 0;
  for ($i = $start; $i < $start+$num; $i++) {
    if ($curated[$i]) {
      if ($j % 3 == 0) $html .= '<tr>';
      $html .= '<td>' . $curated[$i]->display() . '</td>';
      if ($j % 3 == 2) $html .= '</tr>';
      $j++;
    }
  }
  if ($j % 3 == 1) $html .= '<td>&nbsp;</td></tr>';                                                                    
  if ($j % 3 == 2) $html .= '<td>&nbsp;</td><td>&nbsp;</td></tr>';
  return $html . '</table>';
}


function get_curated_short()
{
    $curated = curated_xml(2);
    
    // output html
    foreach ($curated as $c) {
        $html .= $c->display_short();
    }
    return $html;
}

function curated_xml($num)
{
    // open and parse curated.xml
    $xml =& openXML('curated.xml');
    
    // get software nodes
    $softwares = $xml->getElementsByTagName('software');
    $softwares = $softwares->toArray();
    
    // create curated objects
    $i = 1;
    foreach ($softwares as $software) {
        $curated[] = new Curated($software);
        if ($i >= $num && $num != 'all') { break; }
        $i++;
    }
       
    return $curated;
}

/******************************************** NETWORK ***/
function get_network_list(&$network, $num = 30)
{    
    // output html
    $html = '<dl class="network">'."\n";
    for ($i = 0; $i < $num; $i++) {
        $html .= $network[$i]->display();
    }
    $html .= "</dl>\n\n";
    return $html;
}

function get_network_table(&$network, $start = NETWORK_FIRST_PAGE, $num = 90)
{
    $html = "\n<table border=\"0\" width=\"100%\">\n\t<tr>\n";
    $j = 0;
    for ($i = $start; $i < min(count($network), $start+$num); $i++) {
        if ($j % 3 == 0 && $j != 0) { $html .= "\t</tr>\n\t<tr>\n"; }
        if ($network[$i]) {
            $html .= "\t\t<td>" . $network[$i]->display_cell() . "</td>\n";
        } else {
            $html .= "\t\t<td>&nbsp;</td>\n";
        }
        $j++;
    }
    return $html . "\t</tr>\n</table>\n";
}

function network_xml($num)
{
    // open and parse network.xml
    $xml =& openXML('network.xml');
    
    // get software nodes
    $softwares = $xml->getElementsByTagName('software');
    $softwares = $softwares->toArray();
    
    // create network objects
    $i = 1;
    foreach ($softwares as $software) {
        $network[] = new Network($software);
        if ($i >= $num && $num != 'all') { break; }
        $i++;
    }
    
    return $network;   
}

if (!defined('COVER')) {
    $benchmark_start = microtime_float();
    
    // get xml
    $curated = curated_xml('all');
    $network = network_xml('all');
    // count number of items
    $ctotal = count($curated);
    $ntotal = count($network);
    // count number of pages needed
    $cnum_pages = ceil($ctotal / CURATED_PER_PAGE);
    $nnum_pages = ceil($ntotal / NETWORK_PER_PAGE);
    
    // create and write the first page
    $page = new Page('Exhibition', 'Index');
    $page->subtemplate('template.exhibition.html');
    $page->set('exhibition', get_curated_one($curated, 0, CURATED_PER_PAGE/2));
    $page->set('network', get_network_list($network, NETWORK_FIRST_PAGE));
    writeFile("exhibition/index.html", $page->out());
    
    // create and write the other pages
    for ($i = 0; $i <= $cnum_pages; $i++) {
        $page = new Page('Exhibition Archives', 'Collection');
        $page->subtemplate('template.curated.archive.html');
        $page->set('curated_nav', curated_nav($cnum_pages, $i+1));
        $page->set('exhibition', get_curated_three($curated, CURATED_PER_PAGE*$i, CURATED_PER_PAGE));
        //$pagename = sprintf("curated_page_%d.html", $i+1);
        if ($i == 0 ) {
          $pagename = sprintf("curated_page_new.html");
        } else {
          $pagename = sprintf("curated_page_%d.html", $cnum_pages-$i);
        }
        writeFile("exhibition/".$pagename, $page->out());
    }
    
    for ($i = 0; $i <= $nnum_pages; $i++) {
        $page = new Page('Network Archives', 'Network Links');
        $page->subtemplate('template.network.archive.html');
        $page->set('network_nav', network_nav($nnum_pages, $i+1));
        //$page->set('network', get_network_table($network, NETWORK_PER_PAGE*($i-2)+NETWORK_FIRST_PAGE, NETWORK_PER_PAGE));
        $page->set('network', get_network_table($network, NETWORK_PER_PAGE*$i, NETWORK_PER_PAGE));
        //$pagename = sprintf("network_page_%d.html", $i);
        if ($i == 0 ) {
          $pagename = sprintf("network_page_new.html");
        } else {
          $pagename = sprintf("network_page_%d.html", $nnum_pages-$i);
        }
        writeFile("exhibition/".$pagename, $page->out());
    }
    
    $benchmark_end = microtime_float();
    $execution_time = round($benchmark_end - $benchmark_start, 4);
    
    if (!defined('SUBMIT')) {
        echo <<<EOC
<h2>Exhibition Generation Successful</h2>
<p>Generator took $execution_time seconds to execute</p>
EOC;
    }
}

function curated_nav($num, $current)
{
    $html = '<p class="exhibition-nav">';
    //if ($num == current) {
    //  $links[] = sprintf("%d", $num);
    //} else {
    //  $links[] = sprintf("<a href=\"curated_page_%d.html\">%d</a>", $num, $num);
    //}
    for ($i = $num; $i > 0; $i--) {
    	if ($i == $num) {
    	  $links[] = sprintf("<a href=\"curated_page_new.html\">%d</a>", $i);
    	} else {
          $links[] = (($num-$i+1) == $current) ? $i : sprintf("<a href=\"curated_page_%d.html\">%d</a>", $i, $i);
    	}
    }
    $html .= implode(' \\ ', $links);
    $html .= '</p>&nbsp;';
    return $html;
}

/*
function curated_nav($num, $current)
{
  $html = '<p class="exhibition-nav">';
  $links[] = '<a href="curated_page_1.html">1</a>';
  for ($i = 2; $i <= $num; $i++) {
    $links[] = ($i == $current) ? $i : sprintf("<a href=\"curated_page_%d.html\">%d</a>", $i, $i);
  }
  $html .= implode(' \\ ', $links);
  $html .= '</p>&nbsp;';
  return $html;
}
*/

function network_nav($num, $current)
{
    $html = '<p class="exhibition-nav">';
    //$links[] = '<a href="network_page_1.html">1</a>';
    for ($i = $num; $i > 0; $i--) {
    	if ($i == $num) {
    	  $links[] = sprintf("<a href=\"network_page_new.html\">%d</a>", $i);
    	} else {
          $links[] = (($num-$i+1) == $current) ? $i : sprintf("<a href=\"network_page_%d.html\">%d</a>", $i, $i);
    	}
    }
    $html .= implode(' \\ ', $links);
    $html .= '</p>&nbsp;';
    return $html;
}

?>