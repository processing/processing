<?

include('domit/xml_domit_include.php');
$doc =& new DOMIT_Document();
$success = $doc->loadXML('content/test.xml');
$curated = $doc->documentElement;
$softwares = $curated->childNodes;
$softwareObjs = array();

foreach ($softwares as $software)
{
	$s = new Software($software);
	$s->display();
}

class Software
{
	var $name;
	var $by;
	var $scroll;
	var $resize;
	var $width;
	var $height;
	var $image;
	var $description;
	var $location;
	var $links = array();
	
	function Software($xml)
	{
		$this->name 	= $xml->getAttribute('name');
		$this->by		= $xml->getAttribute('by');
		$this->scroll	= $xml->getAttribute('scroll');
		$this->width	= $xml->getAttribute('width');
		$this->height	= $xml->getAttribute('height');
		$image			= $xml->getElementsByTagName('image'); $image = $image->toArray();
		$this->image	= $image[0]->getText();
		$description	= $xml->getElementsByTagName('description');
		$description	= $description->toString();
		$description 	= eregi("<description>(.*)<\/description>", $description, $matches);
		$this->description = $matches[1];
		$location		= $xml->getElementsByTagName('location');
		$location		= $location->item(0);
		$this->location = $location->getText();
		$links			= $xml->getElementsByTagName('link');
		$links			= $links->toArray();
		foreach($links as $link) {
			$this->links[] = '<a href="'. $link->getAttribute('href') . '">' . $link->getText() . '</a>';
		}
	}
	
	function display()
	{
		echo <<<EOD
<h2>{$this->name}</h2>
<p>by {$this->by}</p>
<p>{$this->description}</p>
<p>Links: </p>
EOD;
		foreach ($this->links as $link) {
			echo $link . '<br />';
		}
	}
}

?>