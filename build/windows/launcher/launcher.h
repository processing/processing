// launcher.h : main header file for the LAUNCHER application
//

#if !defined(AFX_IMAGER_H__12C4E5A4_27C5_11D2_BB54_006008DC2F94__INCLUDED_)
#define AFX_IMAGER_H__12C4E5A4_27C5_11D2_BB54_006008DC2F94__INCLUDED_

#if _MSC_VER >= 1000
#pragma once
#endif // _MSC_VER >= 1000

#ifndef __AFXWIN_H__
	#error include 'stdafx.h' before including this file for PCH
#endif

#include "resource.h"		// main symbols

extern "C" {
	int java_main(int argc, char *argv[]);
}

/////////////////////////////////////////////////////////////////////////////
// CImagerApp:
// See Imager.cpp for the implementation of this class
//

class CImagerApp : public CWinApp
{
public:
	CImagerApp();
	
// Overrides
	// ClassWizard generated virtual function overrides
	//{{AFX_VIRTUAL(CImagerApp)
	public:
	virtual BOOL InitInstance();
	//}}AFX_VIRTUAL

	void CImagerApp::buildClassPath(char*);

// Implementation

	//{{AFX_MSG(CImagerApp)
		// NOTE - the ClassWizard will add and remove member functions here.
		//    DO NOT EDIT what you see in these blocks of generated code !
	//}}AFX_MSG
	DECLARE_MESSAGE_MAP()
};


/////////////////////////////////////////////////////////////////////////////

//{{AFX_INSERT_LOCATION}}
// Microsoft Developer Studio will insert additional declarations immediately before the previous line.

#endif // !defined(AFX_IMAGER_H__12C4E5A4_27C5_11D2_BB54_006008DC2F94__INCLUDED_)
