package co.loystar.loystarbusiness.models.databinders;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DownloadInvoice{

	@JsonProperty("message")
	private String message;

	public void setMessage(String message){
		this.message = message;
	}

	public String getMessage(){
		return message;
	}

	@Override
 	public String toString(){
		return 
			"DownloadInvoice{" + 
			"message = '" + message + '\'' + 
			"}";
		}
}