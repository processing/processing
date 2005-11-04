<?php
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
    'Input/Output' => array('Mouse' => array(),
                            'Keyboard' => array(),
                            'Time & Date' => array(),
                            'Text Output' => array(),
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
    'Constants' => array()
);

$PAGE_TITLE = "Mobile Processing &raquo; Language (API)";
require '../header.inc.php';

$total = 0;

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

        $total++;
    }
}

?>
<img src="images/header.png"><br>
<br>
<br>
<div class="column">
<?php $counter = 0; ?>
<?php foreach ($categories as $cat => $entry) { ?>
          <img src="images/<?php echo strtolower(str_replace('/', '', $cat)) ?>.gif"><br>
<br>
<?php     foreach ($entry as $subcat => $e) { ?>
<?php         if (count($e) > 0) { ?>
<?php             asort($e); ?>
<?php             if ($subcat != '') { ?>
                      <i><?php echo $subcat ?></i><br>
<?php             } ?>
<?php             foreach ($e as $se) { ?>
<?php                 $counter++; ?>
                      <a href="reference.php?name=<?php echo strtok($se, "\n") ?>"><?php echo strtok("\n") ?></a><br>
<?php             } ?>
                  <br>
<?php         } ?>
<?php     } ?>
          <br>
          <br>
<?php     if ($counter > ($total / 3)) { ?>
<?php         $counter = 0; ?>
              </div>
              <div class="column">
<?php     } ?>
<?php } ?>
</div>
<?php
 require '../footer.inc.php';
?>