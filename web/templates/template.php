<?

require(TEMPLATEDIR.'template.nav.php');

define('HEADER', '<img src="/img/processing_cover.gif" alt="Processing cover" />');
define('HEADER_LINK', '<a href="http://processing.org/"><img src="/img/processing.gif" alt="Processing cover" title="Back to the cover." /></a>');

class Page
{
    var $xhtml;
    var $lang;
    var $subtemplate = false;
	var $section;
    
    function Page($title = '', $section = '', $bodyid = '')
    {
		$this->xhtml = new xhtml_page(TEMPLATEDIR.'template.html');
		$this->xhtml->set('header', $section == 'Cover' ? HEADER : HEADER_LINK);
		$this->section = $section;
		$this->xhtml->set('bodyid', ($bodyid == '') ? $section : $bodyid);
		$title = ($title == '') ? 'Processing.org' : $title . ' \ Processing.org';
		$this->xhtml->set('title', $title);
		$this->xhtml->set('navigation', navigation($section));
    }
    
    function set($key, $value)
    {
        $this->xhtml->set($key, $value);
    }
    
    function set_array($array)
    {
        foreach ($array as $key => $value) {
            $this->xhtml->set($key, $value);
        }
    }
    
    function subtemplate($file)
    {
        $piece = new xhtml_piece(TEMPLATEDIR.$file);
        $this->xhtml->set('content_for_layout', $piece->out());
        $this->subtemplate = true;
    }
    
    function content($content)
    {
        if (!$this->subtemplate) {
            $this->xhtml->set('content_for_layout', $content);
        } else {
            $this->xhtml->set('content', $content);
        }
    }
    
    function language($lang)
    {
        global $LANGUAGES;
        $this->lang = $lang;
        $this->xhtml->set('charset', $LANGUAGES[$lang][1]);
        $this->xhtml->set('lang', $lang);
	  if ($lang != 'en') {
            #$this->xhtml->set('navigation', navigation_tr($this->section));
            $this->xhtml->set('navigation', navigation($this->section));
	  }
    }
    
    function out()
    {
        if (!$this->lang) { $this->language('en'); }
        return $this->xhtml->out();
    }

	function set_rel_path($path = '') 
	{
		$this->xhtml->set('relpath', $path);
	}
}

class ReferencePage
{
    var $xhtml;
    var $lang;
    var $filepath;
    
    function ReferencePage(&$ref, $translation, $lang = 'en')
    {
        global $LANGUAGES;
        
        $this->filepath = 'reference/' . ($lang == 'en' ? '' : "$lang/") . $ref->name();
        $title = $ref->title() . ($lang == 'en' ? '' : " \ {$LANGUAGES[$lang][0]}") .' \ Language (API) \ Processing 1.0';
        
        $xhtml = new xhtml_page(TEMPLATEDIR.'template.translation.html');
        $xhtml->set('header', HEADER_LINK);
        $xhtml->set('title', $title);
        $xhtml->set('bodyid', 'Langauge-'.$lang);
        
        $xhtml->set('navigation', ($lang == 'en') ? navigation('Language') : navigation('Language'));

        $piece = new xhtml_piece(TEMPLATEDIR.'template.reference.item.html');
        $xhtml->set('content_for_layout', $piece->out());
        
        $xhtml->set('reference_nav', reference_nav());
        $xhtml->set('language_nav', language_nav($lang));
        
        $xhtml->set('content', $ref->display());
        foreach ($translation->attributes as $key => $value) {
            $xhtml->set($key, $value);
        }
        
        foreach ($translation->meta as $key => $value) {
            $xhtml->set($key, $value);
        }

		$xhtml->set('updated', date('F d, Y h:i:sa T', filemtime(CONTENTDIR.'/'.$ref->filepath)));
        
        $this->xhtml = $xhtml;
        $this->language($lang);
    }
    
    function language($lang)
    {
        global $LANGUAGES;
        $this->lang = $lang;
        $this->xhtml->set('charset', $LANGUAGES[$lang][1]);
        $this->xhtml->set('lang', $lang);
    }

    function out()
    {
        return $this->xhtml->out();
    }
    
    function write()
    {
        writeFile($this->filepath, $this->xhtml->out());
    }   
}

class LibReferencePage extends ReferencePage
{
    function LibReferencePage(&$ref, $lib, $translation, $lang = 'en')
    {
        global $LANGUAGES;
        
        $this->langdir = 'reference/' . ($lang == 'en' ? '' : "$lang");
        $this->libsdir = $this->langdir . '/libraries';
        $this->libdir = $this->libsdir . "/$lib";
        $this->filepath = $this->libdir . '/' . $ref->name();
        
        $title = $ref->title() . ($lang == 'en' ? '' : " \ {$LANGUAGES[$lang][0]}") .' \ Language (API) \ Processing 1.0';
        
        $xhtml = new xhtml_page(TEMPLATEDIR.'template.translation.html');
        $xhtml->set('header', HEADER_LINK);
        $xhtml->set('title', $title);
        $xhtml->set('bodyid', 'Library-ref');
        if ($lang == 'en') {
            $xhtml->set('navigation', navigation('Libraries'));
        } else {
            $xhtml->set('navigation', navigation_tr('Libraries'));
        }
        
        $piece = new xhtml_piece(TEMPLATEDIR.'template.reference.item.html');
        $xhtml->set('content_for_layout', $piece->out());
        
        $xhtml->set('reference_nav', library_nav($lib));
        $xhtml->set('language_nav', language_nav($lang));
        
        $xhtml->set('content', $ref->display());
        
        foreach ($translation->attributes as $key => $value) {
            $xhtml->set($key, $value);
        }
        
        foreach ($translation->meta as $key => $value) {
            $xhtml->set($key, $value);
        }
   	
		$xhtml->set('updated', date('F d, Y h:i:sa T', filemtime(CONTENTDIR.'/'.$ref->filepath)));
			
        $this->xhtml = $xhtml;
        $this->language($lang);
    }
}

class LocalPage extends Page
{
    var $xhtml;
    var $lang = 'en';
    var $subtemplate = false;
    
    function LocalPage($title = '', $section = '', $bodyid = '', $rel_path = '')
    {
        $this->xhtml = new xhtml_page(TEMPLATEDIR.'template.local.html');
        $this->xhtml->set('header', '<a href="http://processing.org/"><img src="'.$rel_path.'img/processing_cover.gif" alt="Processing cover" title="Go to Processing.org" /></a>');
        $title = ($title == '') ? 'Processing 1.0' : $title . ' \ Processing 1.0';
        $this->xhtml->set('title', $title);
        $this->xhtml->set('navigation', local_nav($section, $rel_path));
		$this->set('relpath', $rel_path);
		$this->language('en');
		$this->xhtml->set('bodyid', ($bodyid == '') ? $section : $bodyid);
    }
}

class LocalReferencePage extends ReferencePage
{
    var $xhtml;
    var $lang = 'en';
    var $filepath;
    
    function LocalReferencePage(&$ref, $translation, $lang = 'en', $rel_path = '')
    {        
        $this->filepath = 'distribution/' . $ref->name();
        $title = $ref->title() .' \ Language (API) \ Processing 1.0';
        
        $xhtml = new xhtml_page(TEMPLATEDIR.'template.local.html');
        $xhtml->set('header', '<a href="http://processing.org/"><img src="img/processing.gif" alt="Processing cover" title="Back to the reference index." /></a>');
        $xhtml->set('title', $title);
        $xhtml->set('bodyid', 'Langauge');
        $xhtml->set('navigation', local_nav('Language'));
        
        $piece = new xhtml_piece(TEMPLATEDIR.'template.reference.item.html');
        $xhtml->set('content_for_layout', $piece->out());
        
        $xhtml->set('reference_nav', reference_nav());
        $xhtml->set('language_nav', language_nav($lang));
        
        $xhtml->set('content', $ref->display());
        foreach ($translation->attributes as $key => $value) {
            $xhtml->set($key, $value);
        }
        
        foreach ($translation->meta as $key => $value) {
            $xhtml->set($key, $value);
        }

		$xhtml->set('relpath', $rel_path);
		$xhtml->set('updated', date('F d, Y h:i:sa T', filemtime(CONTENTDIR.'/'.$ref->filepath)));
        
        $this->xhtml = $xhtml;
        $this->language($lang);
    }
}

class LocalLibReferencePage extends ReferencePage
{
    function LocalLibReferencePage(&$ref, $lib, $translation, $rel_path = '../../')
    {
        global $LANGUAGES;
		$lang = 'en';
        
        $this->filepath = "distribution/libraries/$lib/" . $ref->name();
        
        $title = $ref->title() . "\\ $lib \\ Language (API) \\ Processing 1.0";
        
        $xhtml = new xhtml_page(TEMPLATEDIR.'template.local.html');
        $xhtml->set('header', '<a href="http://processing.org/"><img src="'.$rel_path.'img/processing.gif" alt="Processing.org" title="Back to the reference index." /></a>');
        $xhtml->set('title', $title);
        $xhtml->set('bodyid', 'Library-ref');
        
        $xhtml->set('navigation', local_nav('Libraries', $rel_path));
        
        $piece = new xhtml_piece(TEMPLATEDIR.'template.reference.item.html');
        $xhtml->set('content_for_layout', $piece->out());
        
        $xhtml->set('reference_nav', library_nav($lib));
        $xhtml->set('language_nav', language_nav($lang));

        foreach ($translation->attributes as $key => $value) {
            $xhtml->set($key, $value);
        }
        
        foreach ($translation->meta as $key => $value) {
            $xhtml->set($key, $value);
        }

        $xhtml->set('content', $ref->display());
   	
		$xhtml->set('updated', date('F d, Y h:i:sa T', filemtime(CONTENTDIR.'/'.$ref->filepath)));
		$xhtml->set('relpath', $rel_path);
        $this->xhtml = $xhtml;
    }
}

?>
