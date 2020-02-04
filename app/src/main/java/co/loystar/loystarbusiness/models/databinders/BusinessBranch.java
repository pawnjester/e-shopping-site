package co.loystar.loystarbusiness.models.databinders;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;

public class BusinessBranch{

	@JsonProperty("updated_at")
	private String updatedAt;

	@JsonProperty("address_line2")
	private String addressLine2;

	@JsonProperty("address_line1")
	private String addressLine1;

	@JsonProperty("name")
	private String name;

	@JsonProperty("created_at")
	private String createdAt;

	@JsonProperty("id")
	private int id;

	@JsonProperty("merchant_id")
	private int merchantId;

	@JsonProperty("home")
	private boolean home;

	public void setUpdatedAt(String updatedAt){
		this.updatedAt = updatedAt;
	}

	public String getUpdatedAt(){
		return updatedAt;
	}

	public void setAddressLine2(String addressLine2){
		this.addressLine2 = addressLine2;
	}

	public String getAddressLine2(){
		return addressLine2;
	}

	public void setAddressLine1(String addressLine1){
		this.addressLine1 = addressLine1;
	}

	public String getAddressLine1(){
		return addressLine1;
	}

	public void setName(String name){
		this.name = name;
	}

	public String getName(){
		return name;
	}

	public void setCreatedAt(String createdAt){
		this.createdAt = createdAt;
	}

	public String getCreatedAt(){
		return createdAt;
	}

	public void setId(int id){
		this.id = id;
	}

	public int getId(){
		return id;
	}

	public void setMerchantId(int merchantId){
		this.merchantId = merchantId;
	}

	public int getMerchantId(){
		return merchantId;
	}

	public void setHome(boolean home){
		this.home = home;
	}

	public boolean isHome(){
		return home;
	}

	@Override
 	public String toString(){
		return 
			"BusinessBranch{" + 
			"updated_at = '" + updatedAt + '\'' + 
			",address_line2 = '" + addressLine2 + '\'' + 
			",address_line1 = '" + addressLine1 + '\'' + 
			",name = '" + name + '\'' + 
			",created_at = '" + createdAt + '\'' + 
			",id = '" + id + '\'' + 
			",merchant_id = '" + merchantId + '\'' + 
			",home = '" + home + '\'' + 
			"}";
		}
}