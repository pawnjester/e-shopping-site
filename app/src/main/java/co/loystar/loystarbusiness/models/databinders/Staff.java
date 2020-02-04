package co.loystar.loystarbusiness.models.databinders;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;

public class Staff{

	@JsonProperty("role")
	private Role role;

	@JsonProperty("updated_at")
	private String updatedAt;

	@JsonProperty("business_branch")
	private BusinessBranch businessBranch;

	@JsonProperty("employer")
	private Employer employer;

	@JsonProperty("created_at")
	private String createdAt;

	@JsonProperty("id")
	private int id;

	@JsonProperty("email")
	private String email;

	@JsonProperty("username")
	private String username;

	public void setRole(Role role){
		this.role = role;
	}

	public Role getRole(){
		return role;
	}

	public void setUpdatedAt(String updatedAt){
		this.updatedAt = updatedAt;
	}

	public String getUpdatedAt(){
		return updatedAt;
	}

	public void setBusinessBranch(BusinessBranch businessBranch){
		this.businessBranch = businessBranch;
	}

	public BusinessBranch getBusinessBranch(){
		return businessBranch;
	}

	public void setEmployer(Employer employer){
		this.employer = employer;
	}

	public Employer getEmployer(){
		return employer;
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

	public void setEmail(String email){
		this.email = email;
	}

	public String getEmail(){
		return email;
	}

	public void setUsername(String username){
		this.username = username;
	}

	public String getUsername(){
		return username;
	}

	@Override
 	public String toString(){
		return 
			"Data{" + 
			"role = '" + role + '\'' + 
			",updated_at = '" + updatedAt + '\'' + 
			",business_branch = '" + businessBranch + '\'' + 
			",employer = '" + employer + '\'' + 
			",created_at = '" + createdAt + '\'' + 
			",id = '" + id + '\'' + 
			",email = '" + email + '\'' + 
			",username = '" + username + '\'' + 
			"}";
		}
}