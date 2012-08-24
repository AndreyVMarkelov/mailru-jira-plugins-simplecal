// Created by Andrey Markelov 29-08-2012.
// Copyright Mail.Ru Group 2012. All rights reserved.
function addMailRuCalendar(event, baseUrl) {
    event.preventDefault();

    var dialogBody = initAddCal(baseUrl);
    if (!dialogBody)
    {
        return;
    }

    var md = new AJS.Dialog({
        width:680,
        height:520,
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
        beforeSend: function() {
            block();
        },
        async: false,
        error: function(xhr, ajaxOptions, thrownError) {
            alert(xhr.responseText);
        },
        success: function(result) {
            res = result.html;
        },
        complete: function() {
            unblock();
        }
    });

    return res;
}

function block() {
    jQuery.blockUI({
        message: jQuery('#loading'),
        css: {
            width: '200px' 
        },
        fadeOut: 200,
        fadeIn: 200
    });
}

function unblock() {
    jQuery.unblockUI();
}

function changeCalMode(baseUrl, name, ctime) {
    var selector = "input[name='" + name + ctime  + "']";
    var mode = AJS.$(selector).attr("checked");
    jQuery.ajax({
        url: baseUrl + "/rest/mailrucalws/1.0/mailcalsrv/changecalmode",
        type: "POST",
        dataType: "json",
        data: {"mode": mode, "name": name, "ctime": ctime},
        error: function(xhr, ajaxOptions, thrownError) {
            alert(xhr.responseText);
        },
        success: function(result) {
            AJS.$('#calendar').fullCalendar('refetchEvents');
        }
    });
}

function actMailRuCalendar(event, baseUrl, name, ctime) {
    event.preventDefault();

    var dialogBody = initInfoCal(baseUrl, name, ctime);
    if (!dialogBody)
    {
        return;
    }

    var md = new AJS.Dialog({
        width:680,
        height:520,
        id:"info_calendar_dialog",
        closeOnOutsideClick: true
    });
    md.addHeader(AJS.I18n.getText("mailrucal.infocaltitle"));
    md.addPanel("load_panel", dialogBody);
    md.addButton(AJS.I18n.getText("mailrucal.updatecalbtn"), function() {
        if (!AJS.$("#calname").val() || !AJS.$("#calcolor :selected").val()) {
            alert(AJS.I18n.getText("mailrucal.addcalerror"));
        } else {
            AJS.$("#deletecalform").get(0).setAttribute('action', baseUrl + "/rest/mailrucalws/1.0/mailcalsrv/updatecalendar");
            AJS.$("#deletecalform").submit();
        }
    });
    md.addButton(AJS.I18n.getText("mailrucal.removecalbtn"), function() {
        if(confirm(AJS.I18n.getText("mailrucal.confirmdelete"))) {
            AJS.$("#deletecalform").submit();
        }
    });
    md.addCancel(AJS.I18n.getText("mailrucal.closebtn"), function() {
        AJS.$("#deletecalform").remove();
        md.hide();
    });
    md.show();
}

function initInfoCal(baseUrl, name, ctime)
{
    var res = "";
    JIRA.SmartAjax.makeRequest({
        url: baseUrl + "/rest/mailrucalws/1.0/mailcalsrv/infocaldlg",
        type: "POST",
        dataType: "json",
        async: false,
        data: {"name": name, "ctime": ctime},
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
    AJS.$("#mainsel").html(AJS.$("#projsel").html());
}

function fillJcl() {
    AJS.$("#mainsel").html(AJS.$("#jclsel").html());
}

function changeColor() {
    var color = AJS.$("#calcolor :selected").val();
    AJS.$("#calcolor").css("background-color", color);
}

AJS.$(document).ready(function() {
    AJS.$(window).bind('beforeunload', function() {
        return null;
    });
});

function setRange() {
    AJS.$("#startpoint").enable(true);
    AJS.$("#endpoint").enable(true);
    AJS.$("#cdpinput").enable(false);
}

function setCustomDate() {
    AJS.$("#startpoint").enable(false);
    AJS.$("#endpoint").enable(false);
    AJS.$("#cdpinput").enable(true);
}

function setIssueDueDate() {
    AJS.$("#startpoint").enable(false);
    AJS.$("#endpoint").enable(false);
    AJS.$("#cdpinput").enable(false);
}
