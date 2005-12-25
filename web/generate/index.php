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
body { margin: 0; font: small Helvetica, Arial, sans-serif; }

h1 { margin: 0; width: 750px; background: #000; }

#body { margin-left: 60px; width: 690px; }

ul, li { margin: 0; padding: 0; list-style: none; }
li { margin-bottom: 1em; }

#status-container { display: none; background: #efefff; border: 1px solid #c8c8ff; padding: 5px; width: 95%; overflow-x: hidden; }
#status-container h3 { margin: 0; }
#status { font-size: .7em; }
    </style>
</head>

<body>
<h1><img src="img/processing_beta_cover.gif" alt="Processing (BETA)" /></h1>

<div id="body">

<h2>Generate Site Files</h2>
<ul>
    <li>Generate <a href="#" onclick="remote_link('cover.php');return false;">Cover</a></li>
    <li>Generate Reference:<br />
        <a href="#" onclick="remote_link('reference.php', 'lang=en');return false;">English</a>, 
        <a href="#" onclick="remote_link('reference.php', 'lang=tr'); return false;">Turkish</a>, 
        <a href="#" onclick="remote_link('reference.php', 'lang=zh'); return false;">Chinese Traditional</a>
    </li>
	<li>Generate One Reference file: 
		<form action="#" method="post" onsubmit="new Ajax.Updater('status', 'reference_one.php', 
		    { asynchronous: true, parameters: Form.serialize(this), onLoading: showloading }); return false;">
			<select name="lang">
<?
	foreach ($LANGUAGES as $code => $array) {
		echo "\t\t\t\t<option value=\"$code\">$array[0]</option>\n";
	}
?>
			</select> / 
			<label><input type="text" name="file" size="35" value="abs.xml" /></label>
			<input type="submit" value="Generate" />
		</form>
	</li>
    <li>Generate Reference Indices:<br />
        <a href="#" onclick="remote_link('reference_index.php', 'lang=en'); return false;">English</a>, 
        <a href="#" onclick="remote_link('reference_index.php', 'lang=tr'); return false;">Turkish</a>, 
        <a href="#" onclick="remote_link('reference_index.php', 'lang=zh'); return false;">Chinese Traditional</a>
    </li>
    <li>Copy <a href="#" onclick="remote_link('reference_media.php'); return false;">Reference Media files to public directory</a></li>
	<li>Generate Library References:<br />
		<a href="#" onclick="remote_link('libraries.php', 'lang=en'); return false;">English</a>
		<a href="#" onclick="remote_link('libraries.php', 'lang=tr'); return false;">Turkish</a>
	</li>
    <li>Generate <a href="#" onclick="remote_link('exhibition.php'); return false;">Exhibition and archives</a></li>
    <li>Generate <a href="#" onclick="remote_link('courses.php'); return false;">Courses.html</a>
                <a href="#" onclick="remote_link('happenings.php'); return false;">Happenings.html</a>
                <a href="#" onclick="remote_link('updated.php'); return false;">Updates.html</a>
    </li>
</ul>

<h2>Generate Template</h2>
<form action="#" method="post" onsubmit="new Ajax.Updater('status', 'template.php', 
    { asynchronous: true, parameters: Form.serialize(this), onLoading: showloading }); return false;">
    <label>Title: <input type="text" name="title" size="50" /></label><br />
    <label>Section: <input type="text" name="section" size="30" /></label><br />
    <input type="submit" value="Generate" />
</form>

<div id="status-container">
    <h3>Status</h3>
    <div id="status"></div>
</div>

</div>

</body>
</html>