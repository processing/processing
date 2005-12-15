<?

require(TEMPLATEDIR.'template.nav.php');

class Page
{
    var $xhtml;
    var $lang;
    var $subtemplate = false;
    
    function Page($title = '', $section = '', $bodyid = '')
    {
        $bodyid = ($bodyid == '') ? $section : $bodyid;
        $this->xhtml = new xhtml_page(TEMPLATEDIR.'template.html');
        if ($section == 'Cover') {
            $this->xhtml->set('header', '<img src="/img/processing_beta_cover.gif" alt="Processing cover" />');
        } else {
            $this->xhtml->set('header', '<a href="http://processing.org/"><img src="/img/processing_beta.gif" alt="Processing cover" title="Back to the cover." /></a>');
        }
        $this->xhtml->set('bodyid', $bodyid);
        $title = ($title == '') ? 'Processing 1.0 (BETA)' : $title . ' \ Processing 1.0 (BETA)';
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
        if ($lang != 'en')
            $this->xhtml->set('navigation', navigation_tr($section));
    }
    
    function out()
    {
        if (!$this->lang) { $this->language('en'); }
        return $this->xhtml->out();
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
        $title = $ref->title() . ($lang == 'en' ? '' : " \ {$LANGUAGES[$lang][0]}") .' \ Language (API) \ Processing 1.0 (BETA)';
        
        $xhtml = new xhtml_page(TEMPLATEDIR.'template.translation.html');
        $xhtml->set('header', '<a href="http://processing.org/"><img src="/img/processing_beta.gif" alt="Processing cover" title="Back to the cover." /></a>');
        $xhtml->set('title', $title);
        $xhtml->set('bodyid', 'Langauge-'.$lang);
        if ($lang == 'en') {
            $xhtml->set('navigation', navigation('Language'));
        } else {
            $xhtml->set('navigation', navigation_tr('Language'));
        }
        
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
        
        $title = $ref->title() . ($lang == 'en' ? '' : " \ {$LANGUAGES[$lang][0]}") .' \ Language (API) \ Processing 1.0 (BETA)';
        
        $xhtml = new xhtml_page(TEMPLATEDIR.'template.translation.html');
        $xhtml->set('header', '<a href="http://processing.org/"><img src="/img/processing_beta.gif" alt="Processing cover" title="Back to the cover." /></a>');
        $xhtml->set('title', $title);
        $xhtml->set('bodyid', 'Library-ref');
        if ($lang == 'en') {
            $xhtml->set('navigation', navigation('Language'));
        } else {
            $xhtml->set('navigation', navigation_tr('Language'));
        }
        
        $piece = new xhtml_piece(TEMPLATEDIR.'template.reference.item.html');
        $xhtml->set('content_for_layout', $piece->out());
        
        $xhtml->set('reference_nav', library_nav($libraries, $lib));
        $xhtml->set('language_nav', language_nav($lang));
        
        $xhtml->set('content', $ref->display());
        
        foreach ($translation->attributes as $key => $value) {
            $xhtml->set($key, $value);
        }
        
        foreach ($translation->meta as $key => $value) {
            $xhtml->set($key, $value);
        }
        
        $this->xhtml = $xhtml;
        $this->language($lang);
    }
}

class LocalPage extends Page
{
    var $xhtml;
    var $lang = 'en';
    var $subtemplate = false;
    
    function LocalPage($title = '', $section = '', $bodyid = '')
    {
        $this->xhtml = new xhtml_page(TEMPLATEDIR.'template.local.html');
        $this->xhtml->set('header', '<a href="http://processing.org/"><img src="img/processing_beta.gif" alt="Processing cover" title="Back to the cover." /></a>');
        $bodyid = ($bodyid == '') ? $section : $bodyid;
        $this->xhtml->set('bodyid', $bodyid);
        $title = ($title == '') ? 'Processing 1.0 (BETA)' : $title . ' \ Processing 1.0 (BETA)';
        $this->xhtml->set('title', $title);
        $this->xhtml->set('navigation', short_nav($section));
    }
}

class LocalReferencePage extends ReferencePage
{
    var $xhtml;
    var $lang = 'en';
    var $filepath;
    
    function LocalReferencePage(&$ref, $translation, $lang = 'en')
    {        
        $this->filepath = 'distribution/' . $ref->name();
        $title = $ref->title() .' \ Language (API) \ Processing 1.0 (BETA)';
        
        $xhtml = new xhtml_page(TEMPLATEDIR.'template.local.html');
        $xhtml->set('header', '<a href="http://processing.org/"><img src="img/processing_beta.gif" alt="Processing cover" title="Back to the cover." /></a>');
        $xhtml->set('title', $title);
        $xhtml->set('bodyid', 'Langauge');
        $xhtml->set('navigation', short_nav('Language'));
        
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
        
        $this->xhtml = $xhtml;
        $this->language($lang);
    }
}

?>
