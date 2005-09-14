<?

require_once('../config.php');

$minutes_blocked    = 5;
$preg_url = 	'¤^(http[s]{0,1}\://.+\..+(\..+)*/)¤';
$preg_email = 	'¤^[a-zA-Z][\w\.-]*[a-zA-Z0-9]@[a-zA-Z0-9][\w\.-]*[a-zA-Z0-9]\.[a-zA-Z][a-zA-Z\.]*[a-zA-Z]$¤i';

$page = new Page('Add Course', 'Courses &amp; Workshops');
$pagetitle = "<h2><img src=\"/img/cover/courses.gif\" alt=\"Courses &amp; Workshops\" /></h2>";

if (!$_POST['preview'] && test_input()) { // valid input

    // let's first check back if he / she just added something and
	// politely ask to come back a little later ...

	$ip_date = @file('courses.txt');
	foreach ($ip_date as $line) {
		if (strpos($line, $_SERVER['REMOTE_ADDR']) !== FALSE) {
			$tuple = 	explode( "\t", $line );
			$tuple[1] = str_replace( "\r", "", $tuple[1] );
			
			if ((date('U') - $tuple[1]) < (60*$minutes_blocked)){
				$page->content( $pagetitle . 'Please come back and try again in '.$minutes_blocked.' minutes.' );
				echo $page->out();
				exit;
			}
		}
	}
	
	unset( $ip_date[0] );
		
	$ip_date[] = $_SERVER['REMOTE_ADDR']."\t".date('U')."\r";
		
	$ip_file = fopen('courses.txt', 'w');
	fwrite($ip_file, implode('', $ip_date));
	fclose($ip_file);
    
    // open and parse xml
    $doc =& new DOMIT_Document();
    if ($doc->loadXML(CONTENTDIR.'courses.xml')) {
        $xml =& $doc->documentElement;
    } else {
        echo('Could not open or read XML file: '. $file . '<br />' . $doc->getErrorString());
    }
    
    $newxml = <<<XML
<course>
    <date></date>
    <institution></institution>
    <title></title>
    <description></description>
    <contact>
        <name></name>
        <email></email>
    </contact>
    <links>
        <link>
            <title></title>
            <url></url>
        </link>
        <link>
            <title></title>
            <url></url>
        </link>
        <link>
            <title></title>
            <url></url>
        </link>
        <link>
            <title></title>
            <url></url>
        </link>
        <link>
            <title></title>
            <url></url>
        </link>
    </links>
</course>
XML;

    $doc2 =& new DOMIT_Document();
    $doc2->parseXML($newxml);
    $xml2 = $doc2->documentElement;
    
    $xml2->childNodes[0]->setText(trim(chars($_POST['date'])));
    $xml2->childNodes[1]->setText(trim(chars($_POST['inst'])));
    $xml2->childNodes[2]->setText(trim(chars($_POST['title'])));
    $xml2->childNodes[3]->setText(trim(chars($_POST['descr'])));
    
    $contact =& $xml2->childNodes[4];
    $contact->childNodes[0]->setText(trim(chars($_POST['name'])));
    $contact->childNodes[1]->setText(trim(chars(str_replace(array('.', '@'), array(' dot ', ' at '), $_POST['email']))));
    
    $links =& $xml2->childNodes[5];
    $link1 =& $links->childNodes[0];
    $link2 =& $links->childNodes[1];
    $link3 =& $links->childNodes[2];
    $link4 =& $links->childNodes[3];
    $link5 =& $links->childNodes[4];
    $link1->childNodes[0]->setText(trim($_POST['l1_name']));
    $link1->childNodes[1]->setText(trim($_POST['l1']));
    $link2->childNodes[0]->setText(trim($_POST['l2_name']));
    $link2->childNodes[1]->setText(trim($_POST['l2']));
    $link3->childNodes[0]->setText(trim($_POST['l3_name']));
    $link3->childNodes[1]->setText(trim($_POST['l3']));
    $link4->childNodes[0]->setText(trim($_POST['l4_name']));
    $link4->childNodes[1]->setText(trim($_POST['l4']));
    $link5->childNodes[0]->setText(trim($_POST['l5_name']));
    $link5->childNodes[1]->setText(trim($_POST['l5']));
    
    $xml->insertBefore($xml2, $xml->firstChild);
    
    $doc->saveXML(CONTENTDIR.'courses.xml', true);
    
    // output page
    $page->content($pagetitle . 'Thanks! Your course has been added to the <a href="/courses.html">Courses &amp; Workshops page</a>.');
    echo $page->out();
    
    DEFINE('SUBMIT', true);
	ob_start();
	include(GENERATEDIR.'happenings.php');
	include(GENERATEDIR.'courses.php');
	include(GENERATEDIR.'cover.php');
	ob_end_clean();

} else { // preview or invalid or missing input
    
    // preview
    if ($_POST['preview']) {
        $date = 	chars( $_POST['date'] );
		$inst = 	chars( $_POST['inst'] );
		$title = 	ine($_POST['title']) ? ', '.chars( $_POST['title'] ):'';
		$descr = 	chars( $_POST['descr'] );
		$email = 	ine($_POST['email']) ? 
            '('.str_replace(array('.', '@'), array(' dot ', ' at '), $_POST['email']).')' : '';
		$contact = 	ine($_POST['name']) && ine($email) ? 
            '<p>Contact: '.chars( $_POST['name'] ).' '.$email.'</p>':'';
        
        $l1 = ine($_POST['l1']) ? '<a href="'.$_POST['l1'].'" title="'.$_POST['l1_name'].'">'.$_POST['l1_name'].'</a>':'';
		$l2 = ine($_POST['l2']) ? ', <a href="'.$_POST['l2'].'" title="'.$_POST['l2_name'].'">'.$_POST['l2_name'].'</a>':'';
		$l3 = ine($_POST['l3']) ? ', <a href="'.$_POST['l3'].'" title="'.$_POST['l3_name'].'">'.$_POST['l3_name'].'</a>':'';
		$l4 = ine($_POST['l4']) ? ', <a href="'.$_POST['l4'].'" title="'.$_POST['l4_name'].'">'.$_POST['l4_name'].'</a>':'';
		$l5 = ine($_POST['l5']) ? ', <a href="'.$_POST['l5'].'" title="'.$_POST['l5_name'].'">'.$_POST['l5_name'].'</a>':'';
        
        $links = '<p>Links: '.$l1.$l2.$l3.$l4.$l5.'</p>';
        $links = $links == '<p>Links: </p>' ? '' : $links;
        
        $preview = <<<PREV
<div class="course-desc">
    <p class="date">$date</p>
    <h3>$inst$title</h3>
    <p>$descr</p>
    $contact $links
</div>
PREV;
    }
    
    // error messages
    $msg = array();
	if ( count($_POST) !== 0 ) {
		if ( !ine($_POST['date']) ) {
			$msg['date'] = 'Please specify when the course or workshop will take place.';	
		}
		
		if ( !ine($_POST['inst']) ) {
			$msg['inst'] = 'What\'s the name of the institution at which the course or workshop is happening?';	
		}
		
		if ( !ine($_POST['title']) ) {
			$msg['title'] = 'What is the title of the course or workshop?';	
		}
		
		if ( !ine($_POST['descr']) ) {
			$msg['descr'] = 'Please describe the course or workshop.';	
		}
		
		if ( !ine($_POST['email']) || !preg_match( $preg_email, $_POST['email']) ) {
			$msg['email'] = 'Please give a valid email adress (@ and dots will be automatically replaced).';
		}

		for ( $il=0; $il<=5; $il++ ) {
			if ( ine($_POST['l'.$il]) && !preg_match( $preg_url, $_POST['l'.$il]) )
			{
				$msg['l'.$il] = 'URL is not well formatted. http://somedomain.abc/ (Don\'t forget the trailing slash)';	
			}
		}
	}
    
    $msg = array_map(create_function('$str', 'return "<p class=\"error\">$str</p>";'), $msg);
    
    // form
    $form = <<<FORM
<form accept-charset="utf-8" action="{$_SERVER['PHP_SELF']}" method="post" name="mform">
	<p>Date:</p>
	<input name="date" type="text" size="48" maxlength="255" value="{$_POST['date']}" /> {$msg['date']}
	
	<p>Institution:</p>
	<input name="inst" type="text" size="48" maxlength="255" value="{$_POST['inst']}" /> {$msg['inst']}
    
	<p>Title:</p>
	<input name="title" type="text" size="48" maxlength="255" value="{$_POST['title']}" /> {$msg['title']}
    
	<p>Description:</p>
	<textarea name="descr" type="text" rows="10" cols="46" >{$_POST['descr']}</textarea> {$msg['descr']}
    
    <p>Contact (Name | Email):</p>
	<input name="name" type="text" size="23" maxlength="125" value="{$_POST['name']}" /> <input name="email" type="text" size="23" maxlength="64" value="{$_POST['email']}" /> {$msg['email']}
    
	<p>Links (Title | URL):</p>
	<input name="l1_name" type="text" size="23" maxlength="64" value="{$_POST['l1_name']}" /> <input name="l1" type="text" size="23" value="{$_POST['l1']}" /><br /> {$msg['l1']}
	<input name="l2_name" type="text" size="23" maxlength="64" value="{$_POST['l2_name']}" /> <input name="l2" type="text" size="23" value="{$_POST['l2']}" /><br /> {$msg['l2']}
	<input name="l3_name" type="text" size="23" maxlength="64" value="{$_POST['l3_name']}" /> <input name="l3" type="text" size="23" value="{$_POST['l3']}" /><br /> {$msg['l3']}
	<input name="l4_name" type="text" size="23" maxlength="64" value="{$_POST['l4_name']}" /> <input name="l4" type="text" size="23" value="{$_POST['l4']}" /><br /> {$msg['l4']}
	<input name="l5_name" type="text" size="23" maxlength="64" value="{$_POST['l5_name']}" /> <input name="l5" type="text" size="23" value="{$_POST['l5']}" /><br /> {$msg['l5']}
    
	<br />
    <br />
    <br />
    <br />
	
	<input type="image" src="../img/submit.gif" value="Submit" alt="Submit" /><input type="image" src="../img/preview.gif" name="preview" value="Preview" alt="Preview" />
</form>
FORM;

    // output page
    $page->content($pagetitle . $preview . $form);
    echo $page->out();
}

function test_input()
{
    global $preg_email;
    $bool = ( ine($_POST['date']) &&
        ine($_POST['title']) &&
        ine($_POST['descr']) &&
        ine($_POST['email']) &&
        preg_match($preg_email, $_POST['email']) );
    $links = (check_link($_POST['l1'], $_POST['l1_name']) &&
            check_link($_POST['l2'], $_POST['l2_name']) &&
            check_link($_POST['l3'], $_POST['l3_name']) &&
            check_link($_POST['l4'], $_POST['l4_name']) &&
            check_link($_POST['l5'], $_POST['l5_name']));
    return $bool && $links;
    return $bool;
}

function check_link($url, $name, $allow_empty = true)
{
    global $preg_url;
    if (!ine($name) && !ine($url) && $allow_empty) { return true; }
    if ((ine($name) && !ine($url)) || (!ine($name) && ine($url))) { return false; }
    if (!preg_match($preg_url, $url) || $url == '') { return false; }
    return true;
}
?>