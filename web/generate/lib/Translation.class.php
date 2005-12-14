<?

class Translation
{
    var $navigation = array();
    var $attributes = array();
    var $categories = array();
    var $cat_tr     = array();
    var $meta       = array();
    
    function Translation($lang)
    {
        $file = "api_$lang/translation/translation.xml";
        $xml =& openXML($file);
        $this->parse($xml);
    }
    
    function parse($xml)
    {
        $nav = $xml->getElementsByTagName('navigation');
        $nav = $nav->item(0);
        foreach ($nav->childNodes as $child) {
            $this->navigation[$child->nodeName] = $child->getText();
        }
        
        $attr = $xml->getElementsByTagName('attributes');
        $attr = $attr->item(0);
        foreach ($attr->childNodes as $child) {
            $this->attributes[$child->nodeName] = $child->getText();
        }
        
        $cats = $xml->getElementsByTagName('categories');
        $cats = $cats->item(0);
        foreach ($cats->childNodes as $c) {
            $name = htmlspecialchars($c->getAttribute('name'));
            $this->categories[$name] = array('' => array());
            $this->cat_tr[$name] = htmlspecialchars(trim($c->firstChild->nodeValue));
            
            if ($c->childCount > 0) {
                foreach ($c->childNodes as $s) {
                    if ($s->nodeType == 1) {
                        $this->categories[$name][$s->getAttribute('name')] = array();
                        $this->cat_tr[$s->getAttribute('name')] = trim($s->firstChild->nodeValue);
                    }
                }
            }
        }
        
        $meta = $xml->getElementsByTagName('meta');
        $meta = $meta->item(0);
        foreach ($meta->childNodes as $child) {
            $nodeName = $child->nodeName;
            eregi("<$nodeName>(.*)<\/$nodeName>", $child->toString(), $matches);
            $this->meta[$nodeName] = $matches[1];
        }
    }
    
    function test()
    {
        echo '<pre>';
        print_r($this->navigation);
        print_r($this->attributes);
        print_r($this->categories);
        print_r($this->cat_tr);
        print_r($this->meta);
    }
}

?>