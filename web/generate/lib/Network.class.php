<?

class Network
{
	var $name;
	var $by;
	var $location;
    var $date;
    
    function Network($xml)
    {
		$this->name 	= getAttribute($xml, 'name');
		$this->by		= getAttribute($xml, 'by');
        $this->location = getValue($xml, 'location');
        $this->date     = getValue($xml, 'date');
    }
    
    function display()
    {
        $html  = "<dt><a href=\"{$this->location}\">{$this->name}</a></dt>\n";
        $html .= "\t<dd>{$this->by}</dd>\n";
        $html .= "\t<dd class=\"date\">{$this->date}</dd>\n";
        return $html;
    }
    
    function display_cell()
    {
        $html  = "<p class=\"network\"><a href=\"{$this->location}\">{$this->name}</a><br />\n";
        $html .= "\t<span>{$this->by}</span><br />\n";
        $html .= "\t<span class=\"date\">{$this->date}</span></p>\n";
        return $html;
    }
}

?>