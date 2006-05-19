<?

class RSS_feed
{
	var $xml;
	var $title;
	var $description;
	var $buildDate;
	var $items;
	
	var $attributes = array('title', 'description', 'buildDate', 'items');
	function RSS_feed($title, $description)
	{
		$this->xml = new xhtml_page(TEMPLATEDIR.'rss.xml');
		$this->title = $title;
		$this->description = $description;
		$this->buildDate = date('r');
	}
	
	function set_items($items)
	{
		$this->items = $items;
	}
	
	function out()
	{
		foreach ($this->attributes as $key) {
			$this->xml->set($key, $this->$key);
		}
		return $this->xml->out();
	}
}

?>