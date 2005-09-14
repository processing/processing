<?

class Happening
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
}

?>