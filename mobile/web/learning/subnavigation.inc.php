<?php
$section = -1;
if (stristr($_SERVER['PHP_SELF'], SITE_ROOT .'learning/index.php') 
    !== false) {
    $section = 0;
} else if (stristr($_SERVER['PHP_SELF'], 
                   SITE_ROOT .'learning/tutorials/index.php') !== false) {
    $section = 1;
}
?>

<div id="subnavigation" style="padding-left: 80px">
    <img src="<?php echo SITE_ROOT?>images/nav_bottomarrow.png" align="absmiddle">

<?php if ($section == 0) { ?>
    Examples
<?php } else { ?>
    <a href="<?php echo SITE_ROOT ?>learning/index.php">Examples</a>
<?php } ?>

    <span class="backslash">\</span>
<?php if ($section == 1) { ?>
    Tutorials
<?php } else { ?>
    <a href="<?php echo SITE_ROOT ?>learning/tutorials/index.php">Tutorials</a> 
<?php } ?>

</div>
