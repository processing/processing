<?

require('../../config.php');

$minutes_blocked    = 5;
$date_format        = 'd M \'y';   // 30 Apr '05

$page = new Page('Add Network Link', 'Exhibition');
$title = "<h2><img src=\"/img/exhibition/networklinks.gif\" alt=\"networklinks.gif\" /></h2>";

// check input and decide what to do ...
if ( !isset($_POST['preview'])
		&& ine($_POST['by'])
			&& ine($_POST['name'])
				&& preg_match( '¤^(http[s]{0,1}\://.+\..+(\..+)*/)¤', $_POST['url']) )
{ // Input is ok, ready to prepare data and save to xml ... */

	 
    // let's first check back if he / she just added something and
	// politely ask to come back a little later ...

	$ip_date = @file('ip_block.txt');
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
		
	$ip_file = fopen('ip_block.txt', 'w');
	fwrite($ip_file, implode('', $ip_date));
	fclose($ip_file);
	
	// open and parse network.xml
    $doc =& new DOMIT_Document();
    if ($doc->loadXML(CONTENTDIR.'network.xml')) {
        $xml =& $doc->documentElement;
    } else {
        echo('Could not open or read XML file: '. $file . '<br />' . $doc->getErrorString());
    }
	
	// clone first network node
	$node = $xml->firstChild->cloneNode(true);
	
	// add data
	$node->setAttribute('name', chars($_POST['name']));
	$node->setAttribute('by', chars($_POST['by']));
	$location =& $node->firstChild;
	$location->setText(chars($_POST['url']));
	$date =& $location->nextSibling;
	$date->setText(date($date_format));
	
	$xml->insertBefore($node, $xml->firstChild);
	
	$doc->saveXML(CONTENTDIR.'network.xml', true);
	
	$page->content( 'Thanks! Your project has been added to the <a href="/exhibition/index.html">Network Links</a>.' );
    echo $page->out();
    
	DEFINE('SUBMIT', true);
	include(GENERATEDIR.'exhibition.php');	
    exit;
	
} else { // preview or missing or invalid submission

    $html = '';
	if ( isset($_POST['preview']) ) {
	
		$name = 	chars( $_POST['name'] );
		$by = 		chars( $_POST['by']   );
		$url = 		chars( $_POST['url']  );
		$date = 	date($date_format);
		
		$preview = <<<PREV
<dl class="network"><dt><a href="{$url}">{$name}</a></dt>	<dd>{$by}</dd>	<dd class="date">{$date}</dd>
</dl>
PREV;
	}
	
    $msg = array();
    
	if ( count($_POST) !== 0 ) {
		if ( !ine($_POST['by']) ) {
			$msg['by'] = 'Please fill in this field.';	
		}
		
		if ( !ine($_POST['name']) ) {
			$msg['name'] = 'Please fill in this field.';	
		}
		
		if ( !ine($_POST['url']) ) {
			$msg['url'] = 'Please fill in this field.';	
		} else if ( !preg_match( '¤^(http[s]{0,1}\://.+\..+(\..+)*/)¤', $_POST['url']) ){
			$msg['url'] = 'URL is not well formatted. http://somedomain.abc/';	
		}
	}
	
	$msg = array_map(create_function('$str', 'return "<p class=\"error\">$str</p>";'), $msg);
	
	$location = ine($_POST['url']) ? $_POST['url'] : 'http://';
	
	$network_form = <<<NTW_FORM
<form accept-charset="utf-8" action="{$_SERVER['PHP_SELF']}" method="post" name="mform">
	<label>Your name:</label><br />
	<input name="by" type="text" size="48" maxlength="48" value="{$_POST['by']}" /><br />
	{$msg['by']}<br />
    
    <label>Work title:</label><br />
	<input name="name" type="text" size="48" maxlength="48" value="{$_POST['name']}" /><br />
	{$msg['name']}<br />
    
    <label>URL:</label><br />
	<input name="url" type="text" size="48" value="{$location}" /><br />
	{$msg['url']}
	<br />
    <br />
    <br />
    <br />
	
	<input type="image" src="/img/submit.gif" value="Submit" alt="Submit" /><input type="image" src="/img/preview.gif" name="preview" value="Preview" alt="Preview" />
</form>
NTW_FORM;

    $page->content($title . $preview . $network_form);
    echo $page->out();
}
	

?>