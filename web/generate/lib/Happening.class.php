<?

class Happening
{
    var $date;
    var $description;
	var $id;
    
    function Happening(&$xml)
    {
        $this->date = getValue($xml, 'date');
        $this->description = innerHTML($xml, 'description');
		$this->id = 'happening'.preg_replace("/\W/", '', $this->date);
    }
    
    function display()
    {
        $html = "<dt id=\"{$this->id}\">{$this->date}</dt>\n";
        $html .= "\t<dd>{$this->description}</dd>\n";
        return $html;
    }

	function display_rss()
	{
		$shortdesc = htmlspecialchars(substr(strip_tags($this->description), 0, 256));
		$link = "http://processing.org/happenings.html#{$this->id}";
		$longdate = $this->date;
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

/*class Happening
{
    var $startdate;
    var $enddate;
    var $description;
    
    function Happening(&$xml)
    {
        $startdate = getValue($xml, 'startdate');
        $this->startdate = strtotime($startdate);
        $enddate = getValue($xml, 'enddate');
        if ($enddate != '') {
            $this->enddate = strtotime($enddate);
        }
        $this->description = innerHTML($xml, 'description');
    }
    
    function display()
    {
        if ($this->enddate != '') {
            $sday       = date('j', $this->startdate);
            $eday       = date('j', $this->enddate);
            $smonth     = date('M', $this->startdate);
            $emonth     = date('M', $this->enddate);
            $syear      = date('Y', $this->startdate);
            $eyear      = date('Y', $this->enddate);
            if ($syear != $eyear) {
                $date = "$sday $smonth $syear &ndash; $eday $emonth $eyear";
            } else if ($smonth != $emonth) {
                $date = "$sday $smonth &ndash; $eday $emonth $syear";
            } else {
                $date = "$sday &ndash; $eday $smonth $syear";
            }
        } else {
            $date = date('j M Y', $this->startdate);
        }
        $html = "<dt>$date</dt>\n";
        $html .= "\t<dd>{$this->description}</dd>\n";
        return $html;
    }
}*/

?>