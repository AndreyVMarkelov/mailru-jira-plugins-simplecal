// Created by Andrey Markelov 29-08-2012.
// Copyright Mail.Ru Group 2012. All rights reserved.

AJS.InlineDialog(AJS.$("#popupLink"), 1, "templates/inline-dialog-content.html");

jQuery(document).ready(function () {
    jQuery(document).ajaxStart(function () { showProgress() }).ajaxStop(function () { hideProgress() });
});
function showProgress() {
    jQuery('body').append('<div id="progress" class="progress"><p class="progress_p">Loading...</p></div>');
    jQuery('#progress').center();
}
function hideProgress() {
    jQuery('#progress').remove();
}
jQuery.fn.center = function () {
    this.css("position", "absolute");
    this.css("top", (jQuery(window).height() - this.height()) / 2 + jQuery(window).scrollTop() + "px");
    this.css("left", (jQuery(window).width() - this.width()) / 2 + jQuery(window).scrollLeft() + "px");
    return this;
}

function addMailRuCalendar(event, baseUrl) {
    event.preventDefault();

    var dialogBody = initAddCal(baseUrl);
    if (!dialogBody)
    {
        return;
    }

    var md = new AJS.Dialog({
        width:740,
        height:560,
        id:"add_calendar_dialog",
        closeOnOutsideClick: true
    });
    md.addHeader(AJS.I18n.getText("mailrucal.createcaltitle"));
    md.addPanel("load_panel", dialogBody);
    md.addButton(AJS.I18n.getText("mailrucal.addcalbtn"), function() {
        if (!AJS.$("#calname").val() ||
            !AJS.$("#calcolor :selected").val() ||
            !AJS.$("#mainsel :selected").val()) {
            alert(AJS.I18n.getText("mailrucal.addcalerror"));
        } else {
            if ((AJS.$("input[name='showfld']:checked").val() == "cdr" && (!AJS.$("#startpoint").val() || !AJS.$("#endpoint").val())) ||
                (AJS.$("input[name='showfld']:checked").val() == "cdp" && !AJS.$("#cdpinput").val())) {
                alert(AJS.I18n.getText("mailrucal.addcalerror"));
            }
            else {
                AJS.$("#addcalform").submit();
            }
        }
    });
    md.addCancel(AJS.I18n.getText("mailrucal.closebtn"), function() {
        AJS.$("#addcalform").remove();
        md.hide();
    });
    md.show();
}

function initAddCal(baseUrl)
{
    var res = "";
    jQuery.ajax({
        url: baseUrl + "/rest/mailrucalws/1.0/mailcalsrv/addcaldlg",
        type: "POST",
        dataType: "json",
        async: false,
        error: function(xhr, ajaxOptions, thrownError) {
            alert(xhr.responseText);
        },
        success: function(result) {
            res = result.html;
        }
    });

    return res;
}

function initCreateIssue(baseUrl, date)
{
    var res = "";
    jQuery.ajax({
        url: baseUrl + "/rest/mailrucalws/1.0/mailcalsrv/initcreatedlg",
        type: "POST",
        dataType: "json",
        data: {"date": date},
        async: false,
        error: function(xhr, ajaxOptions, thrownError) {
            alert(xhr.responseText);
        },
        success: function(result) {
            res = result.html;
        }
    });

    return res;
}

function changeCalMode(baseUrl, ctime) {
    var selector = "input[name='" + ctime  + "']";
    var mode = AJS.$(selector).attr("checked");
    jQuery.ajax({
        url: baseUrl + "/rest/mailrucalws/1.0/mailcalsrv/changecalmode",
        type: "POST",
        dataType: "json",
        async: false,
        data: {"mode": mode, "ctime": ctime},
        error: function(xhr, ajaxOptions, thrownError) {
            alert(xhr.responseText);
        },
        success: function(result) {
            AJS.$('#calendar').fullCalendar('refetchEvents');
        }
    });
}

function deleteMailRuCalendar(event, baseUrl, ctime) {
    if(confirm(AJS.I18n.getText("mailrucal.confirmdelete"))) {
        jQuery.ajax({
            url: baseUrl + "/rest/mailrucalws/1.0/mailcalsrv/deletecalendar",
            type: "POST",
            dataType: "json",
            async: false,
            data: {"calctime": ctime},
            error: function(xhr, ajaxOptions, thrownError) {
                alert(xhr.responseText);
            },
            success: function(result) {
                window.location.reload();
            }
        });
    }
}

function createIssue(baseUrl, date) {
    var dialogBody = initCreateIssue(baseUrl, date);
    if (!dialogBody)
    {
        return;
    }

    var md = new AJS.Dialog({
        width:480,
        height:200,
        id:"init_create_dialog",
        closeOnOutsideClick: true
    });
    md.addHeader(AJS.I18n.getText("mailrucal.createissuetitle"));
    md.addPanel("load_panel", dialogBody);
    md.addButton(AJS.I18n.getText("mailrucal.createbtn"), function() {
        var user = jQuery("#user").val();
        var date = jQuery("#date").val();
        var prId = jQuery("#prId").val();
        var trg = jQuery("#trgName").val();
        var it = jQuery("#its").val();

        var ctx = "?pid=" + prId + "&issuetype=" + it + "&" + trg + "=" + date + "&reporter=" + user;
        window.location = baseUrl + "/secure/CreateIssueDetails!init.jspa" + ctx;
    });
    md.addCancel(AJS.I18n.getText("mailrucal.closebtn"), function() {
        AJS.$("#createissueform").remove();
        md.hide();
    });
    md.show();
}

function changeCalItem(event, baseUrl) {
    var id = jQuery("#projs").val();
    jQuery("#prName").val(jQuery("#pr" + id).val());
    jQuery("#prId").val(jQuery("#prid" + id).val());
    jQuery("#trgName").val(jQuery("#trg" + id).val());
    jQuery("#its").html(jQuery("#it" + id).html());
}

function actMailRuCalendar(event, baseUrl, ctime) {
    event.preventDefault();

    var dialogBody = initInfoCal(baseUrl, ctime);
    if (!dialogBody)
    {
        return;
    }

    if (dialogBody == "NO_PROJECT") {
        var errDlg = new AJS.Dialog({
            width:300,
            height:200,
            id:"error_calendar_dialog",
            closeOnOutsideClick: true
        });
        errDlg.addHeader(AJS.I18n.getText("mailrucal.errorcaltitle"));
        errDlg.addPanel("load_panel", "<p>" + AJS.I18n.getText("mailrucal.noproject") + "</p>");
        errDlg.addCancel(AJS.I18n.getText("mailrucal.closebtn"), function() {
            errDlg.hide();
        });
        errDlg.show();
        return;
    }

    if (dialogBody == "NO_FILTER") {
        var errDlg = new AJS.Dialog({
            width:300,
            height:200,
            id:"error_calendar_dialog",
            closeOnOutsideClick: true
        });
        errDlg.addHeader(AJS.I18n.getText("mailrucal.errorcaltitle"));
        errDlg.addPanel("load_panel", "<p>" + AJS.I18n.getText("mailrucal.nofilter") + "</p>");
        errDlg.addCancel(AJS.I18n.getText("mailrucal.closebtn"), function() {
            errDlg.hide();
        });
        errDlg.show();
        return;
    }

    if (dialogBody == "NO_CALENDAR") {
        var errDlg = new AJS.Dialog({
            width:300,
            height:200,
            id:"error_calendar_dialog",
            closeOnOutsideClick: true
        });
        errDlg.addHeader(AJS.I18n.getText("mailrucal.errorcaltitle"));
        errDlg.addPanel("load_panel", "<p>" + AJS.I18n.getText("mailrucal.nocalendar") + "</p>");
        errDlg.addCancel(AJS.I18n.getText("mailrucal.closebtn"), function() {
            errDlg.hide();
            window.location.reload();
        });
        errDlg.show();
        return;
    }

    var md = new AJS.Dialog({
        width:680,
        height:560,
        id:"info_calendar_dialog",
        closeOnOutsideClick: true
    });
    md.addHeader(AJS.I18n.getText("mailrucal.infocaltitle"));
    md.addPanel("load_panel", dialogBody);
    md.addButton(AJS.I18n.getText("mailrucal.updatecalbtn"), function() {
        if (!AJS.$("#calname").val() || !AJS.$("#calcolor :selected").val()) {
            alert(AJS.I18n.getText("mailrucal.addcalerror"));
        } else {
            AJS.$("#deletecalform").submit();
        }
    });
    md.addCancel(AJS.I18n.getText("mailrucal.closebtn"), function() {
        AJS.$("#deletecalform").remove();
        md.hide();
    });
    md.show();
}

function initInfoCal(baseUrl, ctime)
{
    var res = "";
    jQuery.ajax({
        url: baseUrl + "/rest/mailrucalws/1.0/mailcalsrv/infocaldlg",
        type: "POST",
        dataType: "json",
        async: false,
        data: {"ctime": ctime},
        error: function(xhr, ajaxOptions, thrownError) {
            alert(xhr.responseText);
        },
        success: function(result) {
            res = result.html;
        }
    });

    return res;
}

function fillProj() {
    jQuery("#mainsel").html(AJS.$("#projsel").html());
}

function fillJcl() {
    jQuery("#mainsel").html(jQuery("#jclsel").html());
}

function changeColor() {
    var color = jQuery("#calcolor :selected").val();
    jQuery("#calcolor").css("background-color", color);
}

AJS.$(document).ready(function() {
    jQuery(window).bind('beforeunload', function() {
        return null;
    });
});

function setRange() {
    jQuery("#startpoint").enable(true);
    jQuery("#endpoint").enable(true);
    jQuery("#cdpinput").enable(false);
}

function setCustomDate() {
    jQuery("#startpoint").enable(false);
    jQuery("#endpoint").enable(false);
    jQuery("#cdpinput").enable(true);
}

function setIssueDueDate() {
    jQuery("#startpoint").enable(false);
    jQuery("#endpoint").enable(false);
    jQuery("#cdpinput").enable(false);
}

function setUserPrefView(baseUrl, view) {
    jQuery.ajax({
        url: baseUrl + "/rest/mailrucalws/1.0/mailcalsrv/setuserprefview",
        type: "POST",
        dataType: "json",
        data: {"view": view},
        error: function(xhr, ajaxOptions, thrownError) {
            alert(xhr.responseText);
        }
    });
}

//--> share selectors
function setShareGroup() {
    jQuery("#share_group").show();
    jQuery("#share_project").hide();
}

function setShareProject() {
    jQuery("#share_group").hide();
    jQuery("#share_project").show();
}
//<--

function addGroup() {
    var sharesObj = jQuery.evalJSON(jQuery("#shares_data").val());

    var group = jQuery("#groupShare :selected");
    var grId = "group" + jQuery(group).val();

    for (var objId in sharesObj) {
        if (sharesObj[objId]["id"] == grId) {
            jQuery("#" + grId).animate({backgroundColor: "red"}, 500, function() { jQuery("#" + grId).animate({backgroundColor: "white"}, 500);});
            return;
        }
    }

    var itemObj = new Object();
    itemObj["id"] = grId;
    itemObj["type"] = "G";
    itemObj["group"] = jQuery(group).val();
    sharesObj.push(itemObj);
    jQuery("#shares_data").val(jQuery.toJSON(sharesObj));

    var newElem = "<div id='" + grId + "'><span>" + AJS.format(AJS.I18n.getText("mailrucal.share_group"), jQuery(group).text()) + "</span></div>";
    jQuery("#share_display_div").append(newElem);
    jQuery("#share_trash_sh").clone().show().appendTo("#" + grId);
}

function addProject() {
    var sharesObj = jQuery.evalJSON(jQuery("#shares_data").val());

    var proj = jQuery("#projectShare-project :selected");
    var role = jQuery("#projectShare-role :selected");
    var prId = "project" + jQuery(proj).val() + "role" + jQuery(role).val();

    for (var objId in sharesObj) {
        if (sharesObj[objId]["id"] == prId) {
            jQuery("#" + prId).animate({backgroundColor: "red"}, 500, function() { jQuery("#" + prId).animate({backgroundColor: "white"}, 500);});
            return;
        }
    }

    var itemObj = new Object();
    itemObj["id"] = prId;
    itemObj["type"] = "P";
    itemObj["proj"] = jQuery(proj).val();
    itemObj["role"] = jQuery(role).val();
    sharesObj.push(itemObj);
    jQuery("#shares_data").val(jQuery.toJSON(sharesObj));

    var textVal;
    if (jQuery(role).val()) {
        textVal = AJS.format(AJS.I18n.getText("mailrucal.share_project_role"), jQuery(proj).text(), jQuery(role).text());
    } else {
        textVal = AJS.format(AJS.I18n.getText("mailrucal.share_project"), jQuery(proj).text());
    }

    var newElem = "<div id='" + prId + "'><span>" + textVal + "</span></div>";
    jQuery("#share_display_div").append(newElem);
    jQuery("#share_trash_sh").clone().show().appendTo("#" + prId);
}

function removeGroup(event) {
    var source = event.target || event.srcElement;
    var parent = jQuery(source).parent();
    var parentId = jQuery(parent).attr("id");

    var sharesObj = jQuery.evalJSON(jQuery("#shares_data").val());
    for (var objId in sharesObj) {
        if (sharesObj[objId]["id"] == parentId) {
            sharesObj.splice(objId, 1);
        }
    }
    jQuery("#shares_data").val(jQuery.toJSON(sharesObj));
    jQuery(parent).remove();
}
