package co.loystar.loystarbusiness.models.databinders;

import com.google.gson.annotations.SerializedName;

public class PaymentMessage{

//	@SerializedName("updated_at")
//	private String updatedAt;
//
//	@SerializedName("created_at")
//	private String createdAt;
//
//	@SerializedName("merchant_id")
//	private int merchantId;
//
	@SerializedName("id")
	private int id;

	@SerializedName("message")
	private String message;

//	public void setUpdatedAt(String updatedAt){
//		this.updatedAt = updatedAt;
//	}
//
//	public String getUpdatedAt(){
//		return updatedAt;
//	}
//
//	public void setCreatedAt(String createdAt){
//		this.createdAt = createdAt;
//	}
//
//	public String getCreatedAt(){
//		return createdAt;
//	}
//
//	public void setMerchantId(int merchantId){
//		this.merchantId = merchantId;
//	}
//
//	public int getMerchantId(){
//		return merchantId;
//	}
//
	public void setId(int id){
		this.id = id;
	}

	public int getId(){
		return id;
	}

	public void setMessage(String message){
		this.message = message;
	}

	public String getMessage(){
		return message;
	}

//	@Override
// 	public String toString(){
//		return
//			"PaymentMessage{" +
//			"updated_at = '" + updatedAt + '\'' +
//			",created_at = '" + createdAt + '\'' +
//			",merchant_id = '" + merchantId + '\'' +
//			",id = '" + id + '\'' +
//			",message = '" + message + '\'' +
//			"}";
//		}
}