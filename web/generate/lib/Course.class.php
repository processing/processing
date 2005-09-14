<?

class Course
{
    var $date;
    var $institution;
    var $title;
    var $description;
    var $name;
    var $email;
    var $links = array();
    
    function Course(&$xml)
    {
        $this->date         = getValue($xml, 'date');
        $this->institution  = getValue($xml, 'institution');
        $this->title        = getValue($xml, 'title');
        $this->description  = innerHTML($xml, 'description');
        
        $contact = $xml->getElementsByTagName('contact');
        $contact = $contact->item(0);
        
        $this->name         = getValue($contact, 'name');
        $this->email        = getValue($contact, 'email');
        
        $links = $xml->getElementsByTagName('links');
        $links = $links->item(0);
        
        $this->links = getFragmentsAsArray($xml, 'link', array('title', 'url'));
    }
    
    function display()
    {
        $html  = '<div class="course-desc">'."\n";
        $html .= "\t<p class=\"date\">{$this->date}</p>\n";
        $html .= "\t<h3>{$this->institution}, {$this->title}</h3>\n";
        $html .= "\t<p>{$this->description}</p>\n";
        $email = str_replace(array('@', '.'), array(' at ', ' dot '), $this->email);
        $html .= "\t<p>Contact {$this->name} ($email)</p>\n";
        foreach ($this->links as $link) {
            if ($link['title'] != '' && $link['url'] != '') {
                $links[] = "<a href=\"$link[url]\">$link[title]</a>";
            }
        }
        if (!empty($links)) {
            $html .= "\t<p>Links: ";
            $html .= implode(', ', $links) . "</p>\n";
        }
        $html .= "</div>\n\n";
        return $html;
    }
    
    function display_short()
    {
        $html = "<dt>{$this->institution}: {$this->title}</dt>\n";
        foreach ($this->links as $link) {
            if ($link['url'] != '') {
                $html .= "\t<dd><a href=\"$link[url]\">$link[title]</a></dd>\n";
                break;
            }
        }
        return $html;
    }
    
    function has_link()
    {
        foreach ($this->links as $link) {
            if ($link['url'] != '') {
                return true;
            }
        }
        return false;
    }
}

?>