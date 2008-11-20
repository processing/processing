<? require('../config.php'); ?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
     "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">

<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">

<head>
  <meta http-equiv="content-type" content="text/html;charset=UTF-8" />
 	<title>Processing.org Generator</title>
    <script language="javascript" type="text/javascript" src="/javascript/prototype.js"></script>
    <script language="javascript" type="text/javascript">

function remote_link(href, params)
{
    return new Ajax.Updater('status', href, {asynchronous: true, onLoading: showloading, parameters: params});
}

function showloading()
{
    $('status-container').style.display = 'block';
    $('status').innerHTML = 'Loading...';
}
    </script>
    
    <style type="text/css">
    body { margin: 0; font-family: Verdana, Geneva, Arial, Helvetica, sans-serif; font-size: 12px; }

h1 { margin: 0; width: 750px; background: #000; }

#body { margin-left: 60px; width: 690px; }

ul, li { margin: 0; padding: 0; list-style: none; }
li { margin-bottom: 1em; }

#status-container { display: none; background: #efefff; border: 1px solid #c8c8ff; padding: 5px; width: 95%; overflow-x: hidden; }
#status-container h3 { margin: 0; }
#status { font-size: 12px; }

.inline { display: inline; }
    </style>
</head>

<body>
<h1><img src="../img/processing_beta_cover.gif" alt="Processing" /></h1>

<div id="body">
<p>&nbsp;</p>
     <p><b>Generate reference files</b></p>
     <p><form action="#" method="post" onsubmit="new Ajax.Updater('status', 'svn_update.php',                      
                    { asynchronous: true, parameters: Form.serialize(this), onLoading: showloading }); return false;">
                        <select name="lang">
     <?
      foreach ($LANGUAGES as $code => $array) {
        echo "\t\t\t\t<option value=\"$code\">$array[0]</option>\n";
      }
      ?>
                        </select> 
                        <input type="submit" value="Update XML files from SVN" />
                </form>
        </p>
    <p>
			<form action="#" method="post" onsubmit="new Ajax.Updater('status', 'reference.php', 
			    { asynchronous: true, parameters: Form.serialize(this), onLoading: showloading }); return false;">
				<select name="lang">
				<?
					foreach ($LANGUAGES as $code => $array) {
						echo "\t\t\t\t<option value=\"$code\">$array[0]</option>\n";
					}
				?>
				</select>
				<input type="submit" value="Generate all reference files" />
			</form>
    </p>
	<p>	<form action="#" method="post" onsubmit="new Ajax.Updater('status', 'reference_one.php', 
		    { asynchronous: true, parameters: Form.serialize(this), onLoading: showloading }); return false;">
			<select name="lang">
<?
	foreach ($LANGUAGES as $code => $array) {
		echo "\t\t\t\t<option value=\"$code\">$array[0]</option>\n";
	}
?>
			</select> / 
			<label><input type="text" name="file" size="35" value="abs.xml" /></label>
			<input type="submit" value="Generate one reference file" />
		</form>
	</p>
    <p>	<form action="#" method="post" onsubmit="new Ajax.Updater('status', 'reference_index.php', 
			    { asynchronous: true, parameters: Form.serialize(this), onLoading: showloading }); return false;">
				<select name="lang">
				<?
					foreach ($LANGUAGES as $code => $array) {
						echo "\t\t\t\t<option value=\"$code\">$array[0]</option>\n";
					}
				?>
				</select>
				<input type="submit" value="Generate reference indices" />
			</form>
    </p>
				<p>&nbsp;</p>  
<p><a href="#" onclick="remote_link('reference_media.php'); return false;">Copy reference media to public directory</a></p>

<p>&nbsp;</p>
<strong>Generate non-English static pages (Libraries, Environment, Comparison, Troubleshooting )</strong><br />
<i>Coming soon...</i>
<p>&nbsp;</p>

<strong>Generate English site files</strong>
<p>	
    <a href="#" onclick="remote_link('cover.php');return false;">Cover</a> \ 
	<a href="#" onclick="remote_link('exhibition.php'); return false;">Exhibition and archives</a> \ 
	<a href="#" onclick="remote_link('examples.php'); return false;">Examples</a> \ 
	<a href="#" onclick="remote_link('tutorials.php'); return false;">Tutorials</a> \ 
	<a href="#" onclick="remote_link('libraries.php', 'lang=en'); return false;">Libraries</a> \ 
	<a href="#" onclick="remote_link('tools.php', 'lang=en'); return false;">Tools</a> \ 
	<a href="#" onclick="remote_link('environment.php'); return false;">Environment and Troubleshooting</a> \ 
	<a href="#" onclick="remote_link('compare.php'); return false;">Compare</a> \ 
    <a href="#" onclick="remote_link('courses.php'); return false;">Courses</a> \ 
  	<a href="#" onclick="remote_link('happenings.php'); return false;">Happenings</a> \ 
    <a href="#" onclick="remote_link('updates.php'); return false;">Updates</a> \ 
	<a href="#" onclick="remote_link('staticpages.php'); return false;">Static Pages (FAQ, Copyright, Contribute, People)</a>
	</p>

<p>&nbsp;</p>

<p>
<strong>Generate English files for distribution</strong>
</p>
<p>		<a href="#" onclick="remote_link('reference_local.php'); return false;">Reference</a> \ 
		<a href="#" onclick="remote_link('libraries_local.php'); return false;">Libraries</a> \ 
		<a href="#" onclick="remote_link('tools_local.php'); return false;">Tools</a> \ 
		<a href="#" onclick="remote_link('environment_local.php'); return false;">Environment and Troubleshooting</a> \ 
		<a href="#" onclick="remote_link('compare_local.php'); return false;">Compare</a> \ <br />
		<a href="#" onclick="remote_link('staticpages_local.php'); return false;">Static Pages (FAQ, Copyright, People)</a>
</p>

<p>&nbsp;</p>


<!--
	<p>Generate Library References:
		<a href="#" onclick="remote_link('libraries.php', 'lang=en'); return false;">English</a>
		<a href="#" onclick="remote_link('libraries.php', 'lang=tr'); return false;">Turkish</a>
	</p>
-->

<!--
	<p>Environment pages in 
		<form class="inline" action="#" method="post" onsubmit="new Ajax.Updater('status', 'environment.php', 
	    { asynchronous: true, parameters: Form.serialize(this), onLoading: showloading }); return false;">
		<select name="lang">
<?
foreach ($LANGUAGES as $code => $array) {
	echo "\t\t\t\t<option value=\"$code\">$array[0]</option>\n";
}
?>
		</select>
		<input type="submit" value="Generate" /></form>
	</p>
	
	<p>Comparison pages in 
		<form class="inline" action="#" method="post" onsubmit="new Ajax.Updater('status', 'compare.php', 
		    { asynchronous: true, parameters: Form.serialize(this), onLoading: showloading }); return false;">
			<select name="lang">
	<?
	foreach ($LANGUAGES as $code => $array) {
		echo "\t\t\t\t<option value=\"$code\">$array[0]</option>\n";
	}
	?>
			</select>
			<input type="submit" value="Generate" /></form>
		</p>
-->

<!--
<p>&nbsp;</p>
<p>
<strong>Generate Template</strong>
<form action="#" method="post" onsubmit="new Ajax.Updater('status', 'template.php', 
    { asynchronous: true, parameters: Form.serialize(this), onLoading: showloading }); return false;">
    <label>Title: <input type="text" name="title" size="50" /></label><br />
    <label>Section: <input type="text" name="section" size="30" /></label><br />
    <input type="submit" value="Generate" />
</form>
</p>

-->

<div id="status-container">
    <h3>Status</h3>
    <div id="status"></div>
</div>

</div>

</body>
</html>