<?PHP
/*---------------------------
 Florian Jenett
 01.05.2005
-----------------------------
 files ...
 
 
 
 
 
 */
 
/**
 *		(Short description - used in indexlists)
 *
 *		files.inc.php
 *
 *		(Multiple line detailed description.)
 *		(The handling of line breaks and HTML is up to the renderer.)
 *		(Order: short description - detailed description - doc tags.)
 *
 *
 *		@author 		Florian Jenett - mail@florianjenett.de
 *
 *		created:		01.05.2005 - 15:12 Uhr
 *		modified:		-last-modified-
 *
 *		@since 			-since-version-
 *		@version 		-current-version-
 *
 *		@see			-extends-
 *
 */
 

if ( !defined( 'PRO_EXHIBIT' ) ) die( 'No external access.' );
 
 
 
/**
 *		Template system.
 */


$TEMPLATES = array 	(
						'cover' 		=> '../'.'template.cover.html',
						'curated_page'	=> '../'.'template.curated_page.html',
						'curated_row'	=> '../'.'template.curated_row.html',
						'curated_item'	=> '../'.'template.curated_item.html',
						'network_page'	=> '../'.'template.network_page.html',
						'submit'		=> 'template.submit.html',
						'generate'		=> 'template.generate.html'
					);
					
$title_links = 'Links:<br />';
					
					
					
					
					

/**
 *		XML Parser & files.
 */



$XMLS = array	(
					'curated_index'		=> 'curatedsoftware.xml',
					'network_index'		=> 'networksoftware.xml'
				);
					
					
					
					

/**
 *		Cached files to generate
 */
 
$CACHE_DIR = '../';

$CACHE_FILES = array (
						'cover'				=> 'index.html',
						'curated'			=> 'curated_page_%d.html',
						'network'			=> 'network_page_%d.html'
					);


// in relation to index.html ...

$WORKS_DIR	= 'works/';
					
					
					
					

/**
 *		IP - date protection.
 */
 

$IPDATE_FILE = 'ip_block.txt';

if (!file_exists( $IPDATE_FILE ) )
{
$ip_f_handle = fopen( $IPDATE_FILE, 'w' );
fwrite( $ip_f_handle , <<<IP
000.00.00.00	0000000000
000.00.00.00	0000000000
000.00.00.00	0000000000
000.00.00.00	0000000000
000.00.00.00	0000000000
000.00.00.00	0000000000
000.00.00.00	0000000000
000.00.00.00	0000000000
000.00.00.00	0000000000
000.00.00.00	0000000000
000.00.00.00	0000000000
000.00.00.00	0000000000

IP
);
fclose( $ip_f_handle );
}



?>