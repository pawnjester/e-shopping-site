package co.loystar.loystarbusiness.models.databinders;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import org.joda.time.DateTime;

public class Employer{

	@JsonProperty("digits_user_id")
	private Object digitsUserId;

	@JsonProperty("mobile_money_invoice_number")
	private Object mobileMoneyInvoiceNumber;

	@JsonProperty("total_sms_credits")
	private int totalSmsCredits;

	@JsonProperty("created_at")
	private String createdAt;

	@JsonProperty("uid")
	private String uid;

	@JsonProperty("mobile_money_payment_duration")
	private Object mobileMoneyPaymentDuration;

	@JsonProperty("turn_on_loyalty_programs")
	private boolean turnOnLoyaltyPrograms;

	@JsonProperty("sync_frequency")
	private int syncFrequency;

	@JsonProperty("accounteer_access_token")
	private Object accounteerAccessToken;

	@JsonProperty("address_line2")
	private String addressLine2;

	@JsonProperty("updated_at")
	private String updatedAt;

	@JsonProperty("accounteer_refresh_token")
	private Object accounteerRefreshToken;

	@JsonProperty("provider")
	private String provider;

	@JsonProperty("address_line1")
	private String addressLine1;

	@JsonProperty("role_id")
	private int roleId;

	@JsonProperty("business_type")
	private String businessType;

	@JsonProperty("nickname")
	private Object nickname;

	@JsonProperty("digits_token")
	private DigitsToken digitsToken;

	@JsonProperty("currency")
	private String currency;

	@JsonProperty("id")
	private int id;

	@JsonProperty("first_name")
	private String firstName;

	@JsonProperty("email")
	private String email;

	@JsonProperty("subscription_expires_on")
	private DateTime subscriptionExpiresOn;

	@JsonProperty("image")
	private Object image;

	@JsonProperty("business_name")
	private String businessName;

	@JsonProperty("merchant_devices")
	private List<String> merchantDevices;

	@JsonProperty("last_name")
	private String lastName;

	@JsonProperty("contact_number")
	private String contactNumber;

	@JsonProperty("subscription_plan")
	private String subscriptionPlan;

	@JsonProperty("turn_on_point_of_sale")
	private boolean turnOnPointOfSale;

	@JsonProperty("show_one_month_subscription_option")
	private boolean showOneMonthSubscriptionOption;

	@JsonProperty("name")
	private Object name;

	@JsonProperty("mobile_money_payment_token")
	private Object mobileMoneyPaymentToken;

	@JsonProperty("pos_active_payment_method_id")
	private Object posActivePaymentMethodId;

	@JsonProperty("business_type_id")
	private int businessTypeId;

	@JsonProperty("enable_bluetooth_printing")
	private boolean enableBluetoothPrinting;

	public void setDigitsUserId(Object digitsUserId){
		this.digitsUserId = digitsUserId;
	}

	public Object getDigitsUserId(){
		return digitsUserId;
	}

	public void setMobileMoneyInvoiceNumber(Object mobileMoneyInvoiceNumber){
		this.mobileMoneyInvoiceNumber = mobileMoneyInvoiceNumber;
	}

	public Object getMobileMoneyInvoiceNumber(){
		return mobileMoneyInvoiceNumber;
	}

	public void setTotalSmsCredits(int totalSmsCredits){
		this.totalSmsCredits = totalSmsCredits;
	}

	public int getTotalSmsCredits(){
		return totalSmsCredits;
	}

	public void setCreatedAt(String createdAt){
		this.createdAt = createdAt;
	}

	public String getCreatedAt(){
		return createdAt;
	}

	public void setUid(String uid){
		this.uid = uid;
	}

	public String getUid(){
		return uid;
	}

	public void setMobileMoneyPaymentDuration(Object mobileMoneyPaymentDuration){
		this.mobileMoneyPaymentDuration = mobileMoneyPaymentDuration;
	}

	public Object getMobileMoneyPaymentDuration(){
		return mobileMoneyPaymentDuration;
	}

	public void setTurnOnLoyaltyPrograms(boolean turnOnLoyaltyPrograms){
		this.turnOnLoyaltyPrograms = turnOnLoyaltyPrograms;
	}

	public boolean isTurnOnLoyaltyPrograms(){
		return turnOnLoyaltyPrograms;
	}

	public void setSyncFrequency(int syncFrequency){
		this.syncFrequency = syncFrequency;
	}

	public int getSyncFrequency(){
		return syncFrequency;
	}

	public void setAccounteerAccessToken(Object accounteerAccessToken){
		this.accounteerAccessToken = accounteerAccessToken;
	}

	public Object getAccounteerAccessToken(){
		return accounteerAccessToken;
	}

	public void setAddressLine2(String addressLine2){
		this.addressLine2 = addressLine2;
	}

	public String getAddressLine2(){
		return addressLine2;
	}

	public void setUpdatedAt(String updatedAt){
		this.updatedAt = updatedAt;
	}

	public String getUpdatedAt(){
		return updatedAt;
	}

	public void setAccounteerRefreshToken(Object accounteerRefreshToken){
		this.accounteerRefreshToken = accounteerRefreshToken;
	}

	public Object getAccounteerRefreshToken(){
		return accounteerRefreshToken;
	}

	public void setProvider(String provider){
		this.provider = provider;
	}

	public String getProvider(){
		return provider;
	}

	public void setAddressLine1(String addressLine1){
		this.addressLine1 = addressLine1;
	}

	public String getAddressLine1(){
		return addressLine1;
	}

	public void setRoleId(int roleId){
		this.roleId = roleId;
	}

	public int getRoleId(){
		return roleId;
	}

	public void setBusinessType(String businessType){
		this.businessType = businessType;
	}

	public String getBusinessType(){
		return businessType;
	}

	public void setNickname(Object nickname){
		this.nickname = nickname;
	}

	public Object getNickname(){
		return nickname;
	}

	public void setDigitsToken(DigitsToken digitsToken){
		this.digitsToken = digitsToken;
	}

	public DigitsToken getDigitsToken(){
		return digitsToken;
	}

	public void setCurrency(String currency){
		this.currency = currency;
	}

	public String getCurrency(){
		return currency;
	}

	public void setId(int id){
		this.id = id;
	}

	public int getId(){
		return id;
	}

	public void setFirstName(String firstName){
		this.firstName = firstName;
	}

	public String getFirstName(){
		return firstName;
	}

	public void setEmail(String email){
		this.email = email;
	}

	public String getEmail(){
		return email;
	}

	public void setSubscriptionExpiresOn(DateTime subscriptionExpiresOn){
		this.subscriptionExpiresOn = subscriptionExpiresOn;
	}

	public DateTime getSubscriptionExpiresOn(){
		return subscriptionExpiresOn;
	}

	public void setImage(Object image){
		this.image = image;
	}

	public Object getImage(){
		return image;
	}

	public void setBusinessName(String businessName){
		this.businessName = businessName;
	}

	public String getBusinessName(){
		return businessName;
	}

	public void setMerchantDevices(List<String> merchantDevices){
		this.merchantDevices = merchantDevices;
	}

	public List<String> getMerchantDevices(){
		return merchantDevices;
	}

	public void setLastName(String lastName){
		this.lastName = lastName;
	}

	public String getLastName(){
		return lastName;
	}

	public void setContactNumber(String contactNumber){
		this.contactNumber = contactNumber;
	}

	public String getContactNumber(){
		return contactNumber;
	}

	public void setSubscriptionPlan(String subscriptionPlan){
		this.subscriptionPlan = subscriptionPlan;
	}

	public String getSubscriptionPlan(){
		return subscriptionPlan;
	}

	public void setTurnOnPointOfSale(boolean turnOnPointOfSale){
		this.turnOnPointOfSale = turnOnPointOfSale;
	}

	public Boolean isTurnOnPointOfSale(){
		return turnOnPointOfSale;
	}

	public void setShowOneMonthSubscriptionOption(boolean showOneMonthSubscriptionOption){
		this.showOneMonthSubscriptionOption = showOneMonthSubscriptionOption;
	}

	public boolean isShowOneMonthSubscriptionOption(){
		return showOneMonthSubscriptionOption;
	}

	public void setName(Object name){
		this.name = name;
	}

	public Object getName(){
		return name;
	}

	public void setMobileMoneyPaymentToken(Object mobileMoneyPaymentToken){
		this.mobileMoneyPaymentToken = mobileMoneyPaymentToken;
	}

	public Object getMobileMoneyPaymentToken(){
		return mobileMoneyPaymentToken;
	}

	public void setPosActivePaymentMethodId(Object posActivePaymentMethodId){
		this.posActivePaymentMethodId = posActivePaymentMethodId;
	}

	public Object getPosActivePaymentMethodId(){
		return posActivePaymentMethodId;
	}

	public void setBusinessTypeId(int businessTypeId){
		this.businessTypeId = businessTypeId;
	}

	public int getBusinessTypeId(){
		return businessTypeId;
	}

	public void setEnableBluetoothPrinting(boolean enableBluetoothPrinting){
		this.enableBluetoothPrinting = enableBluetoothPrinting;
	}

	public Boolean isEnableBluetoothPrinting(){
		return enableBluetoothPrinting;
	}

	@Override
 	public String toString(){
		return 
			"Employer{" + 
			"digits_user_id = '" + digitsUserId + '\'' + 
			",mobile_money_invoice_number = '" + mobileMoneyInvoiceNumber + '\'' + 
			",total_sms_credits = '" + totalSmsCredits + '\'' + 
			",created_at = '" + createdAt + '\'' + 
			",uid = '" + uid + '\'' + 
			",mobile_money_payment_duration = '" + mobileMoneyPaymentDuration + '\'' + 
			",turn_on_loyalty_programs = '" + turnOnLoyaltyPrograms + '\'' + 
			",sync_frequency = '" + syncFrequency + '\'' + 
			",accounteer_access_token = '" + accounteerAccessToken + '\'' + 
			",address_line2 = '" + addressLine2 + '\'' + 
			",updated_at = '" + updatedAt + '\'' + 
			",accounteer_refresh_token = '" + accounteerRefreshToken + '\'' + 
			",provider = '" + provider + '\'' + 
			",address_line1 = '" + addressLine1 + '\'' + 
			",role_id = '" + roleId + '\'' + 
			",business_type = '" + businessType + '\'' + 
			",nickname = '" + nickname + '\'' + 
			",digits_token = '" + digitsToken + '\'' + 
			",currency = '" + currency + '\'' + 
			",id = '" + id + '\'' + 
			",first_name = '" + firstName + '\'' + 
			",email = '" + email + '\'' + 
			",subscription_expires_on = '" + subscriptionExpiresOn + '\'' + 
			",image = '" + image + '\'' + 
			",business_name = '" + businessName + '\'' + 
			",merchant_devices = '" + merchantDevices + '\'' + 
			",last_name = '" + lastName + '\'' + 
			",contact_number = '" + contactNumber + '\'' + 
			",subscription_plan = '" + subscriptionPlan + '\'' + 
			",turn_on_point_of_sale = '" + turnOnPointOfSale + '\'' + 
			",show_one_month_subscription_option = '" + showOneMonthSubscriptionOption + '\'' + 
			",name = '" + name + '\'' + 
			",mobile_money_payment_token = '" + mobileMoneyPaymentToken + '\'' + 
			",pos_active_payment_method_id = '" + posActivePaymentMethodId + '\'' + 
			",business_type_id = '" + businessTypeId + '\'' + 
			",enable_bluetooth_printing = '" + enableBluetoothPrinting + '\'' + 
			"}";
		}
}