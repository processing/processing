<? require('../config.php'); ?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">

<html xmlns="http://www.w3.org/1999/xhtml">
<head>
	<meta name="generator" content="HTML Tidy for Mac OS X (vers 1st December 2004), see www.w3.org" />

	<title>Reference Generator</title>
	<script src="/javascript/prototype.js" type="text/javascript" language="javascript" charset="utf-8"></script>
    <script language="javascript" type="text/javascript">

function remote_link(href, params)
{
    return new Ajax.Updater('status', href, 
		{
			asynchronous: true, 
			onLoading: showloading, 
			parameters: params
		});
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
img { display: block; }

#body { margin-left: 60px; width: 690px; }

ul, li { margin: 0; padding: 0; list-style: none; }
li { margin-bottom: 1em; }

#status-container { display: none; background: #efefff; border: 1px solid #c8c8ff; padding: 5px; width: 95%; overflow-x: hidden; }
#status-container h3 { margin: 0; }
#status { font-size: .7em; }

.inline { display: inline; }
    </style>
</head>

<body>
	<h1><img src="processing_beta_cover.gif" alt="Processing (BETA)" /></h1>

	<div id="body">
		<h2>Generate Site Files</h2>
		
		<ul>
			<li>Generate Reference:<br />
					<form action="#" method="post" 
						onsubmit="new Ajax.Updater('status', 
								'../generate/reference.php', 
					    { 
							asynchronous: true, 
							parameters: Form.serialize(this), 
							onLoading: showloading 
						}); return false;">
						
						<select name="lang">
						<? language_menu() ?>
						</select>
						<input type="submit" value="Generate" />
					</form>
		    </li>
			<li>Generate One Reference file: 
				<form action="#" method="post" 
					onsubmit="new Ajax.Updater('status', 
						'../generate/reference_one.php', 
				    { 
						asynchronous: true, 
						parameters: Form.serialize(this), 
						onLoading: showloading 
					}); return false;">
					
					<select name="lang">
					<? language_menu() ?>
					</select> / 
					<label><input type="text" name="file" size="35" value="abs.xml" /></label>
					<input type="submit" value="Generate" />
				</form>
			</li>
		    <li>Generate Reference Indices:<br />
					<form action="#" method="post" 
					onsubmit="new Ajax.Updater('status', 
							'../generate/reference_index.php', 
					    { 
							asynchronous: true, 
							parameters: Form.serialize(this), 
							onLoading: showloading 
						}); return false;">
						
						<select name="lang">
						<? language_menu() ?>
						</select>
						<input type="submit" value="Generate" />
					</form>
		    </li>
			<li>Generate Library References:<br />
				<form action="#" method="post" 
				onsubmit="new Ajax.Updater('status', 
						'../generate/libraries.php', 
				    { 
						asynchronous: true, 
						parameters: Form.serialize(this), 
						onLoading: showloading 
					}); return false;">
					
					<select name="lang">
					<? language_menu() ?>
					</select>
					<input type="submit" value="Generate" />
				</form>
			</li>
		</ul>
		
		<div id="status-container">
		    <h3>Status</h3>
		    <div id="status"></div>
		</div>
	</div>
</body>
</html>
<?

function language_menu()
{
	global $LANGUAGES;
	foreach ($LANGUAGES as $code => $array) {
		$sel = $_GET['lang'] == $code ? ' selected="selected"' : '';
		echo "\t\t\t\t<option value=\"$code\"$sel>$array[0]</option>\n";
	}
}

?>
