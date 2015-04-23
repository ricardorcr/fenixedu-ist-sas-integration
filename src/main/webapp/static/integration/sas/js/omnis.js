function messageAlert(title, message)
{
	bootbox.dialog({
	  title: title,
	  message: message,
	  buttons: {
		    success: {
		      label: "Ok",
		      className: "btn-primary",
		    }
	}
});
}
