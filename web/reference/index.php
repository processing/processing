<?php

$PAGE_TITLE = "Language (API) \ Mobile Processing";
require_once '../header.inc.php';

//// it is not necessary to specify all the strings here, unless
//// you wish to control the order in which they are displayed
$categories = array(
    'Structure' => array(),
    'Environment' => array(),
    'Data' => array('Primitive' => array(),
                    'Composite' => array(),
                    'Conversion' => array(),
                    'String Functions' => array(),
                    'Array Functions' => array()),
    'Control' => array('Relational Operators' => array(),
                       'Iteration' => array(),
                       'Conditionals' => array(),
                       'Logical Operators' => array()), 
    'Shape' => array('2D Primitives' => array(),
                     'Curves' => array(),
                     '3D Primitives' => array(),
                     'Stroke attributes' => array(),
                     'Vertex' => array()),
    'Input/Output' => array('Keyboard' => array(),
                            'Touchscreen' => array(),
                            'Text Input' => array(),
                            'Text Output' => array(),
                            'Time & Date' => array(),
                            'Files' => array(),
                            'Web' => array()),
    'Transform' => array(),
    'Color' => array('Setting' => array(),
                     'Creating & Reading' => array()),
    'Image' => array('' => array(),
                     'Loading & Displaying' => array(),
                     'Pixels' => array()),
    'Typography' => array('' => array(),
                          'Loading & Displaying' => array()),
    'Math' => array('Operators' => array(),
                    'Bitwise Operators' => array(),
                    'Calculation' => array(),
                    'Trigonometry' => array(),
                    'Random' => array()),
    'Net' => array(),
    'Constants' => array()
);

$columns = array('Shape', 'Image');

	 
$fullpath = dirname(__FILE__); 	 

$dir = opendir('API'); 	 
$filename = readdir($dir); 	 
while ($filename !== false) { 	 
    $filename = readdir($dir); 	 
    if ((strstr($filename, '.xml') !== false) && 	 
        (strstr($filename, '.xml~') === false)) { 	 
        $pos = strpos($filename, "_"); 	 
        if ($pos !== false) { 	 
            if (file_exists('API/'. substr($filename, 0, $pos) .'.xml')) { 	 
                continue; 	 
            } 	 
        } 	 
        $dom = domxml_open_file($fullpath ."/API/".$filename); 	 
        
        $shortname = substr($filename, 0, strlen($filename) - 4); 	 
        
        $name = $dom->get_elements_by_tagname('name'); 	 
        $name = trim($name[0]->get_content()); 	 
        
        $cat = $dom->get_elements_by_tagname('category'); 	 
        $cat = trim($cat[0]->get_content()); 	 
        
        $subcat = $dom->get_elements_by_tagname('subcategory'); 	 
        $subcat = trim($subcat[0]->get_content()); 	 
        
        $categories[$cat][$subcat][] = $shortname."\n".$name; 	 
    } 	 
}
?>
<img src="images/header.png"><br>
<br>
<br>
<div class="column">
<?php 
foreach ($categories as $cat => $entry) { 
    if (array_search($cat, $columns) !== false) {
?>
</div>
<div class="column">
<?php     
} 
?>
    <img src="images/<?php echo strtolower(str_replace('/', '', $cat)) ?>.gif"><br>
<br>
<?php     
    foreach ($entry as $subcat => $e) {
        if (count($e) > 0) {
            asort($e);
            if ($subcat != '') {
?>
    <i><?php echo $subcat ?></i><br>
<?php
            }
            foreach ($e as $se) {
                $filename = strtok($se, "\n");
                $displayname = strtok("\n");
?>
    <a href="<?php echo reflink($filename) ?>"><?php echo $displayname ?></a><br>
<?php
            }
?>
    <br>
<?php
        }
    }
?>
    <br>
    <br>
<?php
} 
?>
</div>
<?php
require '../footer.inc.php';
?>
