package co.loystar.loystarbusiness.models.databinders;


import com.fasterxml.jackson.annotation.JsonProperty;

public class StaffWrapper{

	@JsonProperty("data")
	private Staff data;

	public void setData(Staff data){
		this.data = data;
	}

	public Staff getData(){
		return data;
	}

}