<?

class Update
{
    var $date;
    var $description;
	var $id;
    
    function Update(&$xml)
    {
        $date = getValue($xml, 'date');
        $this->date = strtotime($date);
        $this->description = innerHTML($xml, 'description');
		$this->id = 'update'.date('jMY', $this->date);
    }
    
    function display()
    {
        $html = "<dt id=\"{$this->$id}\">".date("j M Y", $this->date)."</dt>\n";
        $html .= "\t<dd>{$this->description}</dd>\n";
        return $html;
    }

	function display_rss()
	{
		$shortdesc = htmlspecialchars(substr(strip_tags($this->description), 0, 256));
		$link = "http://processing.org/updates.html#{$this->id}";
		$longdate = date('r', $this->date);
		return <<<ITEM
<item>
	<title>$shortdesc</title>
	<description><![CDATA[<p>{$this->description}</p>]]></description>
	<link>$link</link>
	<guid>$link</guid>
	<pubDate>$longdate</pubDate>
</item>	
ITEM;

	}
}

?>