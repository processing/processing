<?php



$PAGE_TITLE = "Processing Mobile 1.0 _ALPHA_ >> Language (API)";
$PAGE_SHOWBACKINDEX = true;

require '../header.inc.php';

$dom = @domxml_open_file("API/{$_GET['name']}.xml");
$root = $dom->document_element();
$value = array();
$child = $root->first_child();

while ($child) {
    $tag = $child->node_name();
    if (($tag == 'example') || ($tag == 'parameter')) {
        if (!isset($value[$tag])) {
            $value[$tag] = array();
        }
        $subvalue = array();
        $gchild = $child->first_child();
        while ($gchild) {
            $gtag = $gchild->node_name();
            $content = trim($gchild->get_content());
            if ($content != "") {
                $subvalue[$gtag] = $content;
            }

            $gchild = $gchild->next_sibling();
        }
        $value[$tag][] = $subvalue;
    } else if ($tag[0] == '#') {
        //// skip
    } else {
        $content = trim($child->get_content());
        if ($content != "") {
            $value[$tag] = $content;
        }
    }

    $child = $child->next_sibling();
}
?>
    

<table border="0" cellspacing="0" cellpadding="0">
<?php if (isset($value['name'])) { ?>
  <tr>
    <td class="reffieldheader">Name</td>
    <td class="reffield">
      <h3><?php echo $value['name'] ?></h3>
    </td>
  </tr>
<?php } ?>
<?php if (isset($value['example'])) { ?>
  <tr>
    <td class="reffieldheader">Examples</td>
    <td class="reffield">
       <?php foreach ($value['example'] as $e) { ?>
               <table border="0" cellspacing="0" cellpadding="0">
                 <tr>
       <?php     if (isset($e['image'])) { ?>
                   <td valign="top">
       <?php         if (strstr($e['image'], '.jar') !== false) { ?>
                       <applet code="<?php echo substr($e['image'], 0, strlen($e['image']) - 4) ?>"
                               archive="API/media/<?php echo $e['image'] ?>"
                               width="100" height="100">
                       </applet>
       <?php         } else { ?>
                       <img src="API/media/<?php echo $e['image'] ?>">
       <?php         } ?>
                   </td>
                   <td width="20">&nbsp;</td>
       <?php     } ?>
                 <td valign="top"><pre><?php echo $e['code'] ?></pre></td>
                 </tr>
               </table>
               <br>
       <?php } ?>
    </td>
  </tr>
<?php } ?>
<?php if (isset($value['description'])) { ?>
  <tr>
    <td class="reffieldheader">Description</td>
    <td class="reffield">
      <?php echo $value['description'] ?>
    </td>
  </tr>
<?php } ?>
<?php if (isset($value['syntax'])) { ?>
  <tr>
    <td class="reffieldheader">Syntax</td>
    <td class="reffield">
      <pre><?php echo $value['syntax'] ?></pre>
    </td>
  </tr>
<?php } ?>
<?php if (isset($value['parameter'])) { ?>
  <tr>
    <td class="reffieldheader">Parameters</td>
    <td class="reffield">
      <table border="0" cellspacing="0" cellpadding="0">
       <?php foreach ($value['parameter'] as $p) { ?>
                 <tr>
                   <td width="70"><?php echo $p['label'] ?></td>
                   <td width="20">&nbsp;</td>
                   <td><?php echo $p['description'] ?></td>
                 </tr>
       <?php } ?>
      </table>
    </td>
  </tr>
<?php } ?>
<?php if (isset($value['usage'])) { ?>
  <tr>
    <td class="reffieldheader">Usage</td>
    <td class="reffield">
      <?php echo $value['usage'] ?>
    </td>
  </tr>
<?php } ?>
</table>
<?php
require '../footer.inc.php';
?>
