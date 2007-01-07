<?php
$section = -1;
if (stristr($_SERVER['PHP_SELF'], 
            SITE_ROOT .'reference/index.php') !== false) {
    $section = 0;
} else if (stristr($_SERVER['PHP_SELF'], 
                   SITE_ROOT .'reference/environment/index.php') !== false) {
    $section = 1;
} else if (stristr($_SERVER['PHP_SELF'], 
                   SITE_ROOT .'reference/libraries/index.php') !== false) {
    $section = 2;
}
?>
<?php if ($offline) { ?>
<div id="subnavigation" style="padding-left: 30px">
<?php } else { ?>
<div id="subnavigation" style="padding-left: 208px">
<?php } ?>
    <img src="<?php echo SITE_ROOT?>images/nav_bottomarrow.png" align="absmiddle">

<?php if ($section == 0) { ?>
    Language
<?php } else { ?>
    <a href="<?php echo SITE_ROOT ?>reference/index.<?php echo $ext ?>">Language</a>
<?php } ?>

    <span class="backslash">\</span>
<?php if ($section == 1) { ?>
    Environment
<?php } else { ?>
    <a href="<?php echo SITE_ROOT ?>reference/environment/index.<?php echo $ext ?>">Environment</a> 
<?php } ?>

    <span class="backslash">\</span>
<?php if ($section == 2) { ?>
    Libraries
<?php } else { ?>
    <a href="<?php echo SITE_ROOT ?>reference/libraries/index.<?php echo $ext ?>">Libraries</a>
<?php } ?>

</div>
