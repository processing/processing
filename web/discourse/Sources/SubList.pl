###############################################################################
# YaBB.pl                                                                     #
###############################################################################
#                                                                             #
# YaBB: Yet another Bulletin Board                                            #
# Open-Source Community Software for Webmasters                               #
#                                                                             #
# Version:        YaBB 1 Gold - SP 1.4                                        #
# Released:       December 2001; Updated November 25, 2004                    #
# Distributed by: http://www.yabbforum.com                                    #
#                                                                             #
# =========================================================================== #
#                                                                             #
# Copyright (c) 2000-2004 YaBB (www.yabbforum.com) - All Rights Reserved.     #
#                                                                             #
# Software by:  The YaBB Development Team                                     #
#               with assistance from the YaBB community.                      #
# Sponsored by: Xnull Internet Media, Inc. - http://www.ximinc.com            #
#               Your source for web hosting, web design, and domains.         #
#                                                                             #
###############################################################################

$sublistplver = "1 Gold - SP 1.4";

%director=(
'login',"LogInOut.pl&Login",
'login2',"LogInOut.pl&Login2",
'logout',"LogInOut.pl&Logout",
'lock',"LockThread.pl&LockThread",
'display',"Display.pl&Display",
'detailedversion',"Admin.pl&ver_detail",
'deletemultimembers',"Admin.pl&DeleteMultiMembers",
'messageindex',"MessageIndex.pl&MessageIndex",
'modify',"ModifyMessage.pl&ModifyMessage",
'modify2',"ModifyMessage.pl&ModifyMessage2",
'modtemp',"AdminEdit.pl&ModifyTemplate",
'modtemp2',"AdminEdit.pl&ModifyTemplate2",
'modagreement',"AdminEdit.pl&ModifyAgreement",
'modagreement2',"AdminEdit.pl&ModifyAgreement2",
'modsettings',"AdminEdit.pl&ModifySettings",
'modsettings2',"AdminEdit.pl&ModifySettings2",
'modmemgr',"AdminEdit.pl&EditMemberGroups",
'modmemgr2',"AdminEdit.pl&EditMemberGroups2",
'movethread',"MoveThread.pl&MoveThread",
'movethread2',"MoveThread.pl&MoveThread2",
'modifycatorder',"ManageCats.pl&ReorderCats",
'modifycat',"ManageCats.pl&ModifyCat",
'modifyboard',"ManageBoards.pl&ModifyBoard",
'markasread',"MessageIndex.pl&MarkRead",
'markallasread',"BoardIndex.pl&MarkAllRead",
'managecats',"ManageCats.pl&ManageCats",
'mailing',"Admin.pl&MailingList",
'membershiprecount',"Admin.pl&AdminMembershipRecount",
'mlall',"Memberlist.pl&MLAll",
'mlletter',"Memberlist.pl&MLByLetter",
'mltop',"Memberlist.pl&MLTop",
'manageboards',"ManageBoards.pl&ManageBoards",
'ml',"Admin.pl&ml",
'post',"Post.pl&Post",
'post2',"Post.pl&Post2",
'print',"Printpage.pl&Print",
'profile',"Profile.pl&ModifyProfile",
'profile2',"Profile.pl&ModifyProfile2",
'register',"Register.pl&Register",
'register2',"Register.pl&Register2",
'reminder',"LogInOut.pl&Reminder",
'reminder2',"LogInOut.pl&Reminder2",
'removethread',"RemoveThread.pl&RemoveThread",
'recent',"Recent.pl&RecentPosts",
'removeoldthreads',"RemoveOldThreads.pl&RemoveOldThreads",
'reorderboards',"ManageBoards.pl&ReorderBoards",
'reorderboards2',"ManageBoards.pl&ReorderBoards2",
'rebuildmemlist',"Admin.pl&RebuildMemList",
'im',"InstantMessage.pl&IMIndex",
'imprefs',"InstantMessage.pl&IMPreferences",
'imprefs2',"InstantMessage.pl&IMPreferences2",
'imoutbox',"InstantMessage.pl&IMOutbox",
'imremove',"InstantMessage.pl&IMRemove",
'imsend',"InstantMessage.pl&IMPost",
'imsend2',"InstantMessage.pl&IMPost2",
'imremoveall',"InstantMessage.pl&KillAll",
'icqpager',"ICQPager.pl&IcqPager",
'ipban',"Admin.pl&ipban",
'ipban2',"Admin.pl&ipban2",
'createcat',"ManageCats.pl&CreateCat",
'clean_log',"Admin.pl&clean_log",
'notify',"Notify.pl&Notify",
'notify2',"Notify.pl&Notify2",
'notify3',"Notify.pl&Notify3",
'notify4',"Notify.pl&Notify4",
'sendtopic',"SendTopic.pl&SendTopic",
'sendtopic2',"SendTopic.pl&SendTopic2",
'setcensor',"AdminEdit.pl&SetCensor",
'setcensor2',"AdminEdit.pl&SetCensor2",
'search',"Search.pl&plushSearch1",
'search2',"Search.pl&plushSearch2",
'setreserve',"AdminEdit.pl&SetReserve",
'setreserve2',"AdminEdit.pl&SetReserve2",
'showclicks',"Admin.pl&ShowClickLog",
'shownotify',"Notify.pl&ShowNotifications",
'stats',"Admin.pl&FullStats",
'sticky',"Subs.pl&Sticky",
'viewprofile',"Profile.pl&ViewProfile",
'viewmembers',"Admin.pl&ViewMembers",
'addboard',"ManageBoards.pl&CreateBoard",
'admin',"Admin.pl&Admin",
'boardrecount',"Admin.pl&AdminBoardRecount",
'editnews',"AdminEdit.pl&EditNews",
'editnews2',"AdminEdit.pl&EditNews2",
'usersrecentposts',"Profile.pl&usersrecentposts");

1;