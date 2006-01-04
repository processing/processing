<?

require('../config.php');

$minutes_blocked    = 5;
$date_format        = 'd F y';   // 30 Apr '05

$page = new Page('Add Happening', 'Happening');
$title = "<h2><img src=\"/img/processinghappenings.gif\" alt=\"Processing Happenings\" /></h2>";

$date = isset($_POST['date']) ? trim($_POST['date']) : false;
$body  = isset($_POST['body'])  ? strip_tags($_POST['body'], '<a><b><strong><i><em>') : false;

if (!$_POST['preview'] && 
    $date !== false && $body !== false ) { // valid input

    // let's first check back if he / she just added something and
	// politely ask to come back a little later ...

	$ip_date = @file('happenings.txt');
	foreach ($ip_date as $line) {
		if (strpos($line, $_SERVER['REMOTE_ADDR']) !== FALSE) {
			$tuple = 	explode( "\t", $line );
			$tuple[1] = str_replace( "\r", "", $tuple[1] );
			
			if ((date('U') - $tuple[1]) < (60*$minutes_blocked)){
				$page->content( $title . 'Please come back and try again in '.$minutes_blocked.' minutes.' );
				echo $page->out();
				exit;
			}
		}
	}
	
	unset( $ip_date[0] );
		
	$ip_date[] = $_SERVER['REMOTE_ADDR']."\t".date('U')."\r";
		
	$ip_file = fopen('happenings.txt', 'w');
	fwrite($ip_file, implode('', $ip_date));
	fclose($ip_file);
	
    // open and parse happenings.xml
    $doc =& new DOMIT_Document();
    if ($doc->loadXML(CONTENTDIR.'happenings.xml')) {
        $xml =& $doc->documentElement;
    } else {
        echo('Could not open or read XML file: '. $file . '<br />' . $doc->getErrorString());
    }
	
	// add data
    $newxml = <<<XML
<item>
    <date></date>
    <description></description>
</item>
XML;
    $doc2 =& new DOMIT_Document();
    $doc2->parseXML($newxml);
    $xml2 = $doc2->documentElement;
    
    $one =& $xml2->childNodes[0];
    $one->setText($date);
    $two =& $xml2->childNodes[1]; 
    $two->setText($body);
	
	$xml->appendChild($xml2);
	
	$doc->saveXML(CONTENTDIR.'happenings.xml', true);
	
	$page->content( 'Thanks! Your news has been added to the <a href="/index.php">Home Page</a>.' );
    echo $page->out();
    
	DEFINE('SUBMIT', true);
	ob_start();
	include(GENERATEDIR.'happenings.php');
	include(GENERATEDIR.'courses.php');
	include(GENERATEDIR.'cover.php');
	ob_end_clean();
	
    exit;


} else { // preview or missing or invalid submission

    $msg = array(); 
    if (count($_POST) !== 0) {
        if (!$date) {
            $msg['date'] = 'Please enter a date';
        }
        if (!$body) {
            $msg['body'] = 'Please fill in this field';
        }
    }
    
	$msg = array_map(create_function('$str', 'return "<p class=\"error\">$str</p>";'), $msg);

    if ($_POST['preview']) { // preview
        $preview = <<<PREV
<dl>
    <dt>{$date}</dt>
    <dd>{$body}</dd>
</dl>   
PREV;
    } 
    
    // form
    $happening_form = <<<H_FORM
<form accept-charset="utf-8" action="{$_SERVER['PHP_SELF']}" method="post" name="mform">

    <label>Date: <em>format: 01 January 05</em></label><br />
    <input name="date" type="text" size="12" value="{$date}" /><br />
    {$msg['date']}<br />
    
    <label>What&rsquo;s Happening?: <em>Allowed html: &lt;a&gt;, &lt;strong&gt;, &lt;em&gt;</em></label><br />
    <textarea name="body" cols="40" rows="3">{$body}</textarea><br />
    {$msg['body']}<br />
    
    <br />
    <br />
    <br />
    <br />
    
	<input type="image" src="/img/submit.gif" value="Submit" alt="Submit" /> <input type="image" src="/img/preview.gif" name="preview" value="Preview" alt="Preview" />

</form>

H_FORM;

    $page->content($title . $preview . $happening_form);
    echo $page->out();
}

?>