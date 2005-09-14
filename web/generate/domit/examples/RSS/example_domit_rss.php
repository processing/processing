<?php	
	//get rss xml from feed "Top Stories from The AT&T Worldnet FeedRoom"
	$myUrl = "http://www.feedroom.com/rssout/att_rss_1ebaad7be9f5b75e7783f8b495e59bd0f58380b9.xml";
	$rss = file_get_contents($myUrl );
	
	//create new DOM Document
	require_once("xml_domit_parser.php");
	$rssDoc = new DOMIT_Document();
	
	//parse RSS XML
	$rssDoc->parseXML($rss, true);
	
	$numChannels = count($rssDoc->documentElement->childNodes);
	
	echo ("<html>\n<head>\n<title>Sample RSS 0.9x Feed Display</title>\n</head>\n\n<body>\n");

	for ($i = 0; $i < $numChannels; $i++) {
		$currentChannel =& $rssDoc->documentElement->childNodes[$i];
		$channelTitle = $currentChannel->childNodes[0]->firstChild->nodeValue;
		$channelDesc = $currentChannel->childNodes[3]->firstChild->nodeValue;
		$channelPubDate = $currentChannel->childNodes[4]->firstChild->nodeValue;
		
		echo ("<h2>$channelTitle</h2>\n<h4>($channelDesc - $channelPubDate)</h4>\n");		
		
		$numChannelNodes = count($currentChannel->childNodes);
		
		//parse out items data
		for ($j = 5; $j < $numChannelNodes; $j++) {
			$currentItem = $currentChannel->childNodes[$j];
			
			$itemTitle = $currentItem->childNodes[0]->firstChild->nodeValue;
			$itemDesc = $currentItem->childNodes[1]->firstChild->nodeValue;
			$itemLink = $currentItem->childNodes[2]->firstChild->nodeValue;
			
			echo ("<p><a href=\"$itemLink\" target=\"_child\">$itemTitle</a> - $itemDesc</p>\n\n");
		}
	}
	
	echo ("</body>\n</html>");
?>