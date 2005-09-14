<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
     "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">

<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">

<head>
  <meta http-equiv="content-type" content="text/html;charset=UTF-8" />
 	<title>Processing.org Generator</title>
    <script language="javascript" type="text/javascript" src="/javascript/prototype.js"></script>
    <script language="javascript" type="text/javascript">
    
function showloading()
{
    $('status').innerHTML = 'Loading...';
}
    </script>
    
    <style>
#status { font-size: .7em; }
    </style>
</head>

<body>

<ul>
    <li><a href="#"
        onclick="new Ajax.Updater('status', 'cover.php', {asynchronous:true,
                onLoading:showloading}); return false;">Generate Cover</a>
    </li>
    <li>Generate Reference:<br />
        <a href="#"
        onclick="new Ajax.Updater('status', 'reference.php', {asynchronous:true, parameters:'lang=en', 
                onLoading:showloading}); return false;">English</a>, 
        <a href="#"
        onclick="new Ajax.Updater('status', 'reference.php', {asynchronous:true, parameters:'lang=tr', 
                onLoading:showloading}); return false;">Turkish</a>, 
        <a href="#"
        onclick="new Ajax.Updater('status', 'reference.php', {asynchronous:true, parameters:'lang=zh', 
                onLoading:showloading}); return false;">Chinese Traditional</a>
    </li>
    <li><a href="#"
        onclick="new Ajax.Updater('status', 'reference_media.php', {asynchronous:true,
                onLoading:showloading}); return false;">Copy Reference Media files to public directory</a>
    </li>
    <li><a href="#"
        onclick="new Ajax.Updater('status', 'exhibition.php', {asynchronous:true,
                onLoading:showloading}); return false;">Generate Exhibition and archives</a>
    </li>
    <li><a href="#"
        onclick="new Ajax.Updater('status', 'courses.php', {asynchronous:true,
                onLoading:showloading}); return false;">Generate Courses.html</a>
    </li>
    <li><a href="#"
        onclick="new Ajax.Updater('status', 'happenings.php', {asynchronous:true,
                onLoading:showloading}); return false;">Generate Happenings.html</a>
    </li>
    <li><a href="#"
        onclick="new Ajax.Updater('status', 'updates.php', {asynchronous:true,
                onLoading:showloading}); return false;">Generate Updates.html</a>
    </li>
</ul>

<form method="post" onsubmit="new Ajax.Updater('status', 'template.php', { asynchronous: true, parameters: Form.serialize(this), onLoading: showloading }); return false;">
    <label>Title: <input type="text" name="title" size="50" /></label><br />
    <label>Section: <input type="text" name="section" size="30" /></label><br />
    <input type="submit" value="Generate" />
</form>

<h3>Status</h3>
<div id="status"></div>

</body>
</html>