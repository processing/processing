<?

class Update
{
    var $date;
    var $description;
    
    function Update(&$xml)
    {
        $date = getValue($xml, 'date');
        $this->date = strtotime($date);
        $this->description = innerHTML($xml, 'description');
    }
    
    function display()
    {
        $html = '<dt>'.date("j M Y", $this->date)."</dt>\n";
        $html .= "\t<dd>{$this->description}</dd>\n";
        return $html;
    }
}

?>