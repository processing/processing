<?

class Curated
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
    
    function Curated($xml)
    {
		$this->name 	= getAttribute($xml, 'name');
		$this->by		= getAttribute($xml, 'by');
		$this->scroll	= getAttribute($xml, 'scroll');
		$this->resize	= getAttribute($xml, 'resize');
		$this->width	= getAttribute($xml, 'width');
		$this->height	= getAttribute($xml, 'height');
        
        $this->image    = getValue($xml, 'image');
        $this->description = innerHTML($xml, 'description');
        $this->location = getValue($xml, 'location');
        
		$links			= $xml->getElementsByTagName('link');
		$links			= $links->toArray();
		foreach($links as $link) {
			$this->links[] = array('href' => $link->getAttribute('href'), 'title' => $link->getText());
		}
    }
    
    function display()
    {
        $html = $this->display_piece();
        $html .= "\t<p>{$this->description}</p><p>Links: \n";
	$linkcount = count($links);
	$ii = 0;
	foreach ($this->links as $link) {
           if ($ii > 0) {
              $html .= sprintf(", ");
            } 
            $html .= sprintf("<a href=\"%s\">%s</a>", $link['href'], $link['title']);
	    $ii++;
        }
        $html .= "</p></div>\n\n";
        return $html;
    }
    
    function display_short()
    {
        $html = $this->display_piece();
        $html .= "</div>\n\n";
        return $html;
    }
    
    function display_piece()
    {
        //$link = sprintf("<a href=\"%s\" onclick=\"javascript:MM_openBrWindow(this.href,'%s','menubar=yes,location=yes,
        //                 status=yes,scrollbars=%s,resizable=%s,width=%d,height=%d');return false;\">",
        //                $this->location, $this->name, $this->scroll, $this->resize, $this->width, $this->height);
        $link = sprintf("<a href=\"%s\" onclick=\"javascript:MM_openBrWindow(this.href,'%s',''toolbar=yes,
        				 status=yes,
        				 scrollbars=yes,
        				 menubar=yes,
        				 location=yes,
                         status=yes,
                         resizable=yes,
                         width=%d,height=%d');return false;\">",
                         $this->location, $this->name, $this->width, $this->height);
                         
        $html  = "<div class=\"curated-item\">\n";
        $html .= "\t$link<img src=\"/exhibition/{$this->image}\" width=\"223\" height=\"72\" alt=\"preview image\" title=\"{$this->name} by {$this->by}\" /></a>\n";
        $html .= "\t<p><br />$link{$this->name}</a><br />\n";
        $html .= "\tby {$this->by}</p>\n";
        return $html;
    }
}

?>