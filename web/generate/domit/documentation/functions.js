var totalPages = 38;
var breakPoint = 14;
var pageDigits = 3;
var tutorialTitle = "DOMIT! v 0.99 Tutorial";

function generateMenu(currentPage) {
	var menu = "";
	var pagePrefix = "DOMIT_tutorial_";

	switch (currentPage) {
		case 0:
			break;
		case 1:
			menu += "<a href=\"" + pagePrefix + getFormattedPageNumber(0) + ".html\">index</a>&nbsp;&nbsp;&nbsp;\n";
			menu += "<a href=\"" + pagePrefix + getFormattedPageNumber(2) + ".html\">&gt;&gt;</a><br />\n";
			break;
		case totalPages:			
			menu += "<a href=\"" + pagePrefix + getFormattedPageNumber((totalPages - 1)) + ".html\">&lt;&lt;</a>\n";
			menu += "&nbsp;&nbsp;&nbsp;<a href=\"" + pagePrefix + getFormattedPageNumber(0) + ".html\">index</a><br />\n";
			break;
		default:
			menu += "<a href=\"" + pagePrefix + getFormattedPageNumber((currentPage - 1)) + ".html\">&lt;&lt;</a>\n";
			menu += "&nbsp;&nbsp;&nbsp;<a href=\"" + pagePrefix + getFormattedPageNumber(0) + ".html\">index</a>&nbsp;&nbsp;&nbsp;\n";
			menu += "<a href=\"" + pagePrefix + getFormattedPageNumber((currentPage + 1)) + ".html\">&gt;&gt;</a><br />\n";

	}

	for (var i = 1; i <= totalPages; i++) {
		if (i != currentPage) {
			menu += "<a href=\"" + pagePrefix + 
				getFormattedPageNumber(i) + 
				".html\">" + i + "</a>&nbsp;&nbsp;&nbsp;\n";
		}
		else {
			menu += i + "&nbsp;&nbsp\n";

		}

		if (i % breakPoint == 0) {
			menu += "<br />\n";
		}
	}
	
	return menu;
} //generateMenu


function getFormattedPageNumber(pageNum) {
	var pageString = ("" + pageNum);
	var total = pageDigits - pageString.length;

	for (var i = 0; i < total; i++) {
		pageString = "0" + pageString;
	}

	return pageString;
} //getFormattedPageNumber


function getTutorialTitle() {
	return tutorialTitle;
} //getTutorialTitle


