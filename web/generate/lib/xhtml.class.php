<?php
/*---------------------------
 Florian Jenett
 14.03.2005
-----------------------------
 template system:
 xhtml_plugin
 xhtml_page,
 xhtml_str,
 xhtml_tag
 
 */
 
/**
 *		A plugin-based template system.
 *
 *		xhtml.class.php
 *		
 *		the generic xhtml_plugin takes data, processes and outputs it.
 *		each plugin extending xhtml_plugin should override process() to
 *		work upon the data(s) it receives upon contruction.
 *		xhtml_plugin uses a str_replace-approach to replace "tags" in
 *		the source data with a replacement-data, which again can be
 *		another xhtml_plugin, allowing for a very modular setup.
 *
 *		
 *		----------------------------------------------------------------
 *
 *		Sample xhtml_plugin's
 *
 *		- xhtml_page 	representing a xhtml-page.
 *
 *		- xhtml_str 	to prepare text ( -> utf8 -> ) for display in xhtml.
 *
 *		- xhtml_tag 	creates a tag from name, attribute-array, cdata.
 *
 *		- xhtml_piece 	read a piece of xhtml embedded in a template file.
 *
 *
 *		----------------------------------------------------------------
 *
 *
 *		@author 		Florian Jenett - mail@florianjenett.de
 *
 *		created:		14.03.2005 - 10:05 Uhr
 *		modified:		30.04.2005 - 12:27 Uhr by Florian Jenett
 *
 *		@since 			0.3
 *		@version 		0.3
 *
 *
 */


define( 'PRO_EXHIBIT', 'ALL_GOOD' );


class xhtml_plugin
{
	var $data_raw;
	var $data_fine;
	var $dirty = true;
	var $elements = array();
	
	var $markup_start = '<!--*-->'; var $markup_end   = '<!--*-->';
	var $insert = '<!--xhtml_plugin_insert-->';
	
	function xhtml_plugin ( $data="xhtml_plugin: Plugin test." ) {
		$this->data_raw = $data;
	}
	
	function set ( $kv, $value=NULL )
	{
		if ( !is_array($this->elements) )
		{
			$this->elements = array();
		}
		
		if (empty($value) && is_array($kv) )
		{
			foreach ( $kv as $k => $v )
			{
				$this->elements[$k] = $v;
			}
			
		} else {
		
			$this->elements[$kv] = $value;
		}
		
		$this->dirty = true;
	}
	
	function process ()
	{
		if ( !$this->dirty ) return;
		
		if ( is_array($this->elements) )
		{
			foreach( $this->elements as $k=>$v )
			{				
				if ( is_array( $v ) )
				{
					$v = $this->process_array( $v );
				}
				
				$v_class 	= get_class( $v );
				/*$v_type 	= gettype( $v ); $v_type !== 'object' && */
					
				if ( $v_class === FALSE ) {
				
					$this->data_raw = str_replace( $this->markup($k) , $this->process_input($v) , $this->data_raw );
				
				} elseif ( is_subclass_of($v,'xhtml_plugin') || $v_class == 'xhtml_plugin' ) {
				
					$this->data_raw = str_replace( $this->markup($k) , $this->process_input($v->out()) , $this->data_raw );
				}
			}
		}
		
		$this->data_fine = str_replace( $this->insert, '', $this->data_raw );
		$this->dirty = false;
	}
	
	function process_array ($arr)
	{
		$arr_size = sizeof($arr);
		
		for( $i=0 ; $i < $arr_size ; $i++ )
		{
			if ( is_subclass_of($arr[$i], 'xhtml_plugin') || get_class($arr[$i])=='xhtml_plugin' ) {
			
				$arr[$i] = $arr[$i]->out();
			}
		}
		
		return implode( '', $arr );
	}
	
	function process_input ($input)
	{
		return $input;
	}
	
	function set_replace ( $kv, $value=NULL )
	{
		$this->set($kv, $value);
		$this->process();
	}
	
	function out () {
		if ($this->dirty) $this->process();
		$this->dirty = false;
		return $this->data_fine;
	}
	
	function markup ( $key )
	{
		return $this->markup_start.$key.$this->markup_end;
	}
	
	function uniqueid ()
	{
		return dechex(crc32(microtime()));
	}
}









class xhtml_page
extends xhtml_plugin
{
	var $insert = '<!--content-->';

	function xhtml_page ( $file=NULL )
	{
		if ( !empty($file) && file_exists($file) && is_readable($file) && strstr($file,'.html') !== FALSE ) {
			$this->data_raw = file_get_contents($file);
		}
		else
			die('Unable to read template:<br />'.$file);
	}
	
	function validate ()
	{
		$this->elements['footer'] .= '<br /><a href="http://validator.w3.org/check?uri=www.quiz-archive.com/beta/index.php">XHTML</a><span class="devider"></span><a href="http://jigsaw.w3.org/css-validator/check/referer">CSS</a>';
	}
	
	function css ( $css )
	{
		$this->data_raw = str_replace('<!--style_sheets-->', $css."\n".'<!--style_sheets-->', $this->data_raw);
	}
	
	function warning ( $msg=NULL )
	{
		$this->message( $msg, 'warning' );
	}
	
	function message_admin ( $msg=NULL, $css_class='message' )
	{
		$this->message( $msg, $css_class='message', "admin" );
	}
	
	function message ( $msg=NULL, $css_class='message', $id="" )
	{
		$mtime = microtime();
		$m_id = 'message_'.md5($mtime);
		
		$this->set($m_id, $msg);
		
		$xhtml_message = '
		<div class="hline"></div>
		<div class="'.$css_class.'" '.(empty($id) ? '' : 'id="'.$id.'"').'>
			'.$this->markup($m_id).'
		</div>
		<div class="'.$css_class.'" '.(empty($id) ? '' : 'id="'.$id.'"').'></div>';
		
		$this->data_raw = str_replace( $this->insert, $xhtml_message."\n\n".$this->insert, $this->data_raw );
		
		return $m_id;
	}
	
	function submenu ( $right=NULL, $id="" )
	{
		$this->insert( new xhtml_tag( 'div', array( 'class'=>'submenu', 'id'=>$id ), $right ) );
	}
	
	function double_column_admin ( $left=NULL, $right=NULL, $width=NULL )
	{
		$this->double_column( $left, $right, $width, '', $id='admin' );
	}
	
	
	function double_column_mem ( $left=NULL, $right=NULL, $width=NULL )
	{
		$this->double_column( $left, $right, $width, '', $id='memory_column' );
	}
	
	function double_column_n ( $left=NULL, $right=NULL, $width=NULL, $css_style=NULL, $id="" )
	{
		$this->double_column( $left, $right, $width, $css_style, $id, false );
	}
	
	function double_column ( $left=NULL, $right=NULL, $width=NULL, $css_style=NULL, $id="", $hline=true )
	{
		$mtime 	= microtime();
		$l_id	= 'left_'.md5($mtime); $r_id	= 'right_'.md5($mtime);
		
		$this->set(array(
							$l_id => $left,
							$r_id => $right
						));
		
		if ( ine($width) || ine($css_style) )
		{
			$style_left = 'style="'.(empty($width)?'':'width:'.$width.'px;').$css_style.'"';
			
			$style_right = 'style="'.(empty($width)?'':'margin-left:'.$width.'px;').$css_style.'"';
		}
		
		if ($hline)
		{
			$xhtml_row .= '<div class="hline"></div>';
		}
		
		$xhtml_row .= '
		<div class="columns">
			<div class="leftcolumn" '.$style_left.' '.(empty($id) ? '' : 'id="'.$id.'"').'>
				<div class="leftpadding">'.$this->markup($l_id).'</div>
			</div>
			<div class="centercolumn" '.$style_right.' '.(empty($id) ? '' : 'id="'.$id.'"').'>
				<div class="centerpadding">'.$this->markup($r_id).'</div>
			</div>
		</div>';
		
		$this->data_raw = str_replace( $this->insert, $xhtml_row."\n\n".$this->insert, $this->data_raw );
		
		return array($l_id, $r_id);
	}
	
	function hr ( )
	{
		$this->data_raw = str_replace( 	$this->insert, 
										'<hr style="margin:0px 0px -2px 0px;border-top:solid 1px #000000" />'.$this->insert, 
										$this->data_raw );
	}
	
	function insert ( $value )
	{
		$value_id = 'insert_'.md5(microtime());
		
		$this->set($value_id, $value);
		 
		$this->data_raw = str_replace( 	
										$this->insert, 
										$this->markup($value_id).$this->insert, 
										$this->data_raw
									 );
		
		$this->dirty = true;
		
		return $value_id;
	}
}









class xhtml_str
extends xhtml_plugin
{
	var $endings;
	
	function xhtml_str ( $data , $endings=TRUE ) {
		$this->data_raw = $data;
		$this->endings = $endings;
	}
	
	function process ()
	{
		$this->data_fine = $this->chars($this->data_raw, $this->endings);
		$this->dirty = false;
	}
	
	function chars ( $plain , $endings=TRUE )
	{
		$plain = $this->seems_utf8($plain) ? utf8_decode($plain) : $plain;
		
		$trans = get_html_translation_table(HTML_ENTITIES, ENT_COMPAT);
		
		foreach ($trans as $key => $value)
			$trans[$key] = '&#'.ord($key).';';
		
		/*
		if ( $endings )
		{
			$trans["\n"] = '<br />';
			$trans["\r"] = '<br />';
			$trans["\t"] = '&nbsp;&nbsp;&nbsp;&nbsp;';
		}
		*/

		$plain = strtr($plain, $trans);
		
		if ($endings) $plain = preg_replace( '/\r\n|\r|\n/', '<br />', $plain);
		
		return $plain;
	}
	
	function seems_utf8($Str)
	{
		for ($i=0; $i<strlen($Str); $i++)
		{
			if (ord($Str[$i]) < 0x80) continue; 			# 0bbbbbbb
			elseif ((ord($Str[$i]) & 0xE0) == 0xC0) $n=1; 	# 110bbbbb
			elseif ((ord($Str[$i]) & 0xF0) == 0xE0) $n=2; 	# 1110bbbb
			elseif ((ord($Str[$i]) & 0xF8) == 0xF0) $n=3; 	# 11110bbb
			elseif ((ord($Str[$i]) & 0xFC) == 0xF8) $n=4; 	# 111110bb
			elseif ((ord($Str[$i]) & 0xFE) == 0xFC) $n=5; 	# 1111110b
			else return false; 								# Does not match any model
			
			for ($j=0; $j<$n; $j++)
			{ 												# n bytes matching 10bbbbbb follow ?
				if ((++$i == strlen($Str)) || ((ord($Str[$i]) & 0xC0) != 0x80))
					return false;
			}
		}
		return true;
	}
	
	function utf8($str)
	{
		$str = get_magic_quotes_gpc() ? stripslashes($str) : $str;
		return $this->seems_utf8($str) ? $str : utf8_encode($str);
	}
}









class xhtml_tag
extends xhtml_plugin
{
	var $insert 		= '<!--xhtml_tag_insert-->';
	var $insert_count 	= 0;
	var $name;
	var $has_child;
	
	function xhtml_tag ( $name, $attribs=NULL, $value=NULL )
	{
		$this->insert = '<!--'.get_class($this).'_'.$this->uniqueid().'_insert-->';
		
		$this->name = $name;
		
		$this->data_raw  = '<'.$name.' ';
		
		if ( isset($attribs) && is_array($attribs) )
		{	
			foreach ($attribs as $k => $v)
			{
				$this->data_raw .= $k.'="'.$v.'" ';	
			}
		}
		
		if ( isset( $value ) )
		{
			$this->data_raw .= '>' . $this->insert . '</'.$name.'>';
			
			$this->has_child = true;
			
			$this->insert( $value );
			
		} else {
		
			$this->data_raw .= '/>';
			
			$this->has_child = false;
		}
	}
	
	function insert ( $value )
	{
		$value_id = 'insert_'.($this->insert_count++).md5(microtime());
		
		$this->set($value_id, $value);
		
		if ( !$this->has_child )
		{
			// we have to change <name/> to <name></name> ?
		
			$this->data_raw  = substr( $this->data_raw, 0, strlen($this->data_raw)-2 );
			$this->data_raw .= '>'.$this->insert.'</'.$this->name.'>';
			$this->has_child = true;
		}
		 
		$this->data_raw = str_replace( 	
										$this->insert, 
										$this->markup($value_id).$this->insert, 
										$this->data_raw
									 );
		
		$this->dirty = true;
		
		return $value_id;
	}
}










class xhtml_piece
extends xhtml_plugin
{
	var $file;
	
	function xhtml_piece ($file)
	{
		$this->file = $file;
		
		if ( !empty($file) && file_exists($file) && is_readable($file) && strstr($file,'.html') !== FALSE ) {
			$this->data_raw = file_get_contents($file);
		}
		else
			die((__FILE__).': Unable to read template:<br />'.$file);
			
		if ( !empty($this->data_raw) )
		{
			$start = strpos( $this->data_raw, '<!--piece_start-->' ) + strlen('<!--piece_start-->');
			$end   = strpos( $this->data_raw, '<!--piece_end-->'   );
			
			if ( $start !== FALSE  &&  $end !== FALSE  &&  $start<$end )
			{
				$this->data_raw = substr($this->data_raw, $start, $end-$start);
			}
		}
	}
	
	
	function get_clipped ( $str, $max )
	{
		$str = preg_replace( '/[\s]+/', ' ', $str);
		
		$str = trim( $str );
		
		if ( strlen( $str ) > $max )
		{
			$str = substr( $str, 0, $max-4 );
			$str = explode( ' ', $str );
			$len = count($str);
			if ( $len > 1 )
			{
				unset( $str[$len-1] );
			}
			return ( implode( ' ', $str ) . ' ...');
		}
		return $str;
	}
	
	
	
	function get_clipped_url ( $str, $max )
	{
		$str = preg_replace( '/^(http|https)(\:[\/]{2})(.*)/i', '$3', $str );
		
		if ( strlen( $str ) > $max )
		{
			$str = substr( $str, 0, $max-4 );
			$str = explode( '/', $str );
			$len = count($str);
			if ( $len > 1 )
			{
				unset( $str[$len-1] );
			}
			return ( implode( '/', $str ) . ' ...');
		}
		return $str;
	}
}

?>