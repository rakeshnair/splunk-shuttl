$(document).ready(function() {

  // Load UI
  setSearchOrThawButtonToThaw();
  $('.loadingBig').hide();
  $('#thawedPage').hide();
  $('form input:visible:enabled:first').focus();
  
  // stretch
  $(top.document.body).css('overflow', 'hidden');
  parent.$('.IFrameInclude').height("100%");
  parent.$('iframe').css('height', '100%')
  function resize_height() { 
    top.$('.IFrameInclude').parent().height( $(top.window).height()-top.$('.appHeaderWrapper').height()-40 );
  };
  resize_height();
  $(window).resize( function() { resize_height(); });

  // Handler for events
  bindHandlers();

  // Datetime-picker
  $('.datetime-input').datepicker({
    dateFormat:"yy-mm-dd",
    showOn: "button",
    buttonImage: Splunk.util.make_url('static/app/', getAppName() ,'/images/calendar_blue.png'),
    buttonImageOnly: true
  });

  // Validator
  $('#search-thaw-buckets-form').validate({
    errorPlacement: function(error, element) {
      error.insertAfter( element.next() );
    }
  });
  jQuery.validator.addMethod(
    "archiverInput", 
    function(value, element) {
      var check = false;
      var re = /^\d{4}-\d{1,2}-\d{1,2}$/;
      if (re.test(value)) {
        var adate = value.split('-');
        var yyyy = parseInt(adate[0], 10);
        var mm = parseInt(adate[1], 10);
        var dd = parseInt(adate[2], 10);
        bdate = new Date(yyyy, mm-1, dd);
        check = ( (bdate.getFullYear() == yyyy) && 
                  (bdate.getMonth() == mm-1) && 
                  (bdate.getDate() == dd) );
      } else if (value == "") {
        check = true;
      }
      return check; // || this.optional(element)
    }, 
    "Please enter a date in the format yyyy-mm-dd"
  );
  jQuery.validator.addClassRules({
    'datetime-input': {
      "archiverInput": true
      // dateISO: true
    }
  });
  
});

function bindHandlers() {
    $('#search-thaw-button').bind('click', function(event){ searchOrThawBuckets(event); } );
    $('input').bind('keyup', function(event) { 
      if (event.keyCode == 13) {
        setSearchOrThawButtonToSearch();
        listBucketsPOST();
      }
    });
    $('#search-thaw-buckets-form').bind('change', function(event) { 
      $('#search-thaw-button').enable();
      setSearchOrThawButtonToSearch();
    });
}

function getPostArguments(form) {
  //remove attributes with no data
  var serializedData = form.serializeArray();
  return $.map(serializedData, function(obj, i){
    if ( obj['value']=="" ) {
      // return null;
    } else if( obj['name']=="index" && obj['value']=="*" ) {
      // return null;
    } else {
      return obj;
    }
  });
}
function listBucketsGET() {
  
  if (!isFormValid()) return;

  loading();
  $.ajax({
    url: 'list_buckets',
    type: 'GET',
    success: function(html) {
      $('#bucket-list').html(html);
    },
    complete: function() {
      loadingDone();
    }
  });
}
function listBucketsPOST() {

  if (!isFormValid()) return;

  var data = getPostArguments($('form'));
  console.log(data);
  
  loading();
  $.ajax({
    url: 'list_buckets',
    type: 'POST',
    data: data,
    success: function(html) {
      $('#bucket-list').html(html);
      setSearchOrThawButtonToThaw();
    },
    complete: function() {
      loadingDone();
      $('#bucket-list').show();
    }
  });
}
function thawBucketsPOST() {

  if (!isFormValid()) return;

  var data = getPostArguments($('form'));
  console.log(data)
  
  loading();
  $.ajax({
    url: 'thaw',
    type: 'POST',
    data: data,
    success: function(html) {
      $('#thawed-list').html(html);
    },
    complete: function() {
      loadingDone();
      $('#thawed-list').show();
    }
  });
}

function getAppName() {
  return $(top.document.body).attr("s:app") || 'UNKNOWN_APP_REALLY';
}

function isFormValid() {
  if($('#search-thaw-buckets-form').valid()) {
    return true;
  } else {
    return false;
  }
}

$.fn.enable = function() {
  $(this).removeAttr('disabled');
}
$.fn.disable = function() {
  $(this).attr('disabled', 'disabled');
}
$.fn.isEnabled = function() {
  return $(this).is(':enabled');
}

function loading() {
  var button = $('#search-thaw-button');
  
  button.disable();
  if(button.hasClass('search')) { 
    $('#bucket-list').hide();
    $('#bucket-list').prev('.loadingBig').show();
  } else if (button.hasClass('thaw')) {
    $('#thawedPage').show(); // not shown from tha beginning
    $('#thawed-list').hide();
    $('#thawed-list').prev('.loadingBig').show();
  }
}
function loadingDone() {
  var button = $('#search-thaw-button');
  
  button.enable();

  // Graphics
  button.removeClass('loading');
  $('.loadingBig').hide();
}

function setSearchOrThawButtonToThaw() {
  var button = $('#search-thaw-button');
  button.addClass('thaw');
  button.removeClass('search');
  button.val("Thaw buckets!");
}
function setSearchOrThawButtonToSearch() {
  var button = $('#search-thaw-button');
  button.addClass('search');
  button.removeClass('thaw');
  button.val("Search for buckets in range");
}
function searchOrThawBuckets(event) {
  var target = $(event.target);
  if (target.isEnabled()) {
    if (target.hasClass('search')) {
      listBucketsPOST();
    } else if (target.hasClass('thaw')) {
      console.log("thaw something!?");
      thawBucketsPOST();
    }
  }
}
