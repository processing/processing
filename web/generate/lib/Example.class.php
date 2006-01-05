<?

class Example
{
	var $name;
	var $cat;
	var $file;
	var $applet;
	var $doc;
	var $code;
	
	function Example($name, $cat)
	{
		$this->name = $name;
		$this->cat = $cat;
		$this->file = file_get_contents(CONTENTDIR.'examples/'.$cat.'/'.$name.'/'.$name.'.pde');
		$this->applet = CONTENTDIR.'examples/'.$cat.'/'.$name.'/applet/'.$name.'.jar';
		$this->split_file();
	}
	
	function split_file()
	{
		$lines = explode("\n", $this->file);
		$doc_lines = array();
		$code_lines = array();
		$doc = true;
		foreach ($lines as $line) {
			if (!preg_match("/^\W/", $line) && $doc) {
				$doc = false;
			}
			if ($doc) {
				$doc_lines[] = $line;
			} else {
				$code_lines[] = $line;
			}
		}
		$this->doc = implode("\n", $doc_lines);
		$this->code = implode("\n", $code_lines);
	}
	
	function display()
	{
		$html = "\n<div class=\"example\">";
		if (file_exists($this->applet)) {
			$html .= "\n<div class=\"applet\">\n\t";
			$html .= '<applet code="'.$this->name.'" archive="media/'.$this->name.'.jar" width="200" height="200"></applet>';
			$html .= "\n</div>";
			
			$html .= "\n<p class=\"doc-float\">";
		} else {
			$html .= "\n<p class=\"doc\">";
		}

		$html .= nl2br($this->doc);
		$html .= "</p>\n";
		
		$html .= "\n<p class=\"code\"><pre>";
		$html .= $this->code;
		$html .= "</pre></p>\n\n";
		
		$html .= "\n</div>\n";
		return $html;
	}
	
	function output_file()
	{
		$page = new Page($this->name . ' \ Examples', 'Examples');
		$page->subtemplate('template.example.html');
		$page->content($this->display());
		writeFile("learning/examples/".strtolower($this->name).".html", $page->out());
		$this->copy_media();
	}
	
	function copy_media()
	{
		if (file_exists($this->applet)) {
			if (!copy($this->applet, EXAMPLESDIR.'media/'.$this->name.'.jar')) {
				echo "Could not copy {$this->applet} to .";
			}
		}
	}
}

?>