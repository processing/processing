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
				$doc_lines[] = str_replace('// ', '', $line);
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
	
	function output_file(&$menu_array)
	{
		$page = new Page($this->name . ' \ Examples', 'Examples');
		$page->subtemplate('template.example.html');
		$page->content($this->display());
		$page->set('examples_nav', $this->make_nav($menu_array));
		writeFile("learning/examples/".strtolower($this->name).".html", $page->out());
		$this->copy_media();
	}
	
	function make_nav(&$array) {
		$html = "\n<table id=\"examples-nav\">\n<tr>";
		
		$store = array();
		$prev = array();
		$next = array();
		$get_next = false;
		
		$select = "\n<select name=\"nav\" size=\"1\" class=\"inputnav\" onChange=\"javascript:gogo(this)\">\n";
		foreach ($array as $cat => $exs) {
			$select .= "\t<optgroup label=\"$cat\">\n";
			foreach ($exs as $file => $name) {
				if ($get_next) {
					$next = array($file, $name);
					$get_next = false;
				}
				if ($file == $this->name.'.html') {
					$sel = ' selected="selected"';
					$prev = $store;
					$get_next = true;
				} else {
					$sel = '';
				}
				$select .= "\t\t<option value=\"".strtolower($file)."\"$sel>$name</option>\n";
				$store = array($file, $name);
			}
			$select .= "\t</optgroup>\n";
		}
		$select .= "</select>\n\n";
		
		if (count($prev) > 0) {
			$html .= '<td><a href="'.strtolower($prev[0]) .'">
				<img src="/img/back_off.gif" alt="'.$prev[1].'" /></a></td>';
		}
		
		$html .= '<td>'.$select.'</td>';
		
		if (count($next) > 0) {
			$html .= '<td><a class="next" href="'.strtolower($next[0]) .'">
				<img src="/img/next_off.gif" alt="'.$next[1].'" /></a></td>';
		}
		return $html . '</tr></table>';
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