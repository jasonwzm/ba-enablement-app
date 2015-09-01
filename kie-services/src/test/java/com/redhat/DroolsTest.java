package com.redhat;

import java.util.ArrayList;
import java.util.Collection;

import org.junit.Assert;
import org.junit.Test;

public class DroolsTest {

	private StatelessDecisionService service = BrmsHelper.newStatelessDecisionServiceBuilder().auditLogName("audit").build();

	@Test
	public void helloWorldTest() {
		// given
		Collection<Object> facts = new ArrayList<Object>();
		Business business = new Business();
		business.setName("test");
		facts.add(business);

		// when
		RuleResponse response = service.runRules(facts, "VerifySupplier", RuleResponse.class);

		// then
		Assert.assertNotNull(response);
		Assert.assertNotNull(response.getBusiness());
		Assert.assertEquals("test", response.getBusiness().getName());
	}
	
	@Test
	public void shouldFilterOutAllRequestsFromKansas(){
		// scenario: business from Kansas are handled by another system - filter them out
		// given a business from Kansas
		Collection<Object> facts = new ArrayList<Object>();
		Business business = new Business();
		business.setStateCode("KS");
		facts.add(business);
		// when I apply the filtering rules
		RuleResponse response = service.runRules(facts, "VerifySupplier", RuleResponse.class);
		// then the business should be filtered
		// and the reason message should be "business filtered from Kansas"
		Assert.assertNotNull(response);
		Assert.assertNotNull(response.getBusiness());
		Assert.assertEquals("filtered", response.getResponseCode());
		boolean found = false;
		for (Reason reason : response.getReasons()){
			if ( reason.getReasonMessage().equals( "business filtered from Kansas") ){
				found = true;
			}
		}
		Assert.assertTrue( "business filtered from Kansas", found );
	}
	
	@Test
	public void shouldProcessAllBusinessesNotFromKansas(){
		// scenario: we are responsible for all businesses not from Kansas
		// given a business from New York
		Collection<Object> facts = new ArrayList<Object>();
		Business business = new Business();
		business.setStateCode("NY");
		facts.add(business);
		// when I apply the filtering rules
		RuleResponse response = service.runRules(facts, "VerifySupplier", RuleResponse.class);
		// then the business should be not be filtered
		// and the validation rules should be applied to the business
		Assert.assertNotNull(response);
		Assert.assertNotNull(response.getBusiness());
		Assert.assertNull(response.getResponseCode());
	}
	
	@Test
	public void shouldCreateValidationErrorsForAnyFieldThatAreEmptyOrNull(){
		// scenario: all fields must have values. 
		// given a business 
		// and the business' zipcode is empty
		// and the business' address line 1 is null
		Collection<Object> facts = new ArrayList<Object>();
		Business business = new Business();
		business.setCity("New York City");
		business.setAddressLine2("Apt 1234");
		business.setFederalTaxId("1234");
		business.setName("nyBusiness");
		business.setPhoneNumber("2120000000");
		business.setStateCode("NY");
		facts.add(business);
		// when I apply the validation rules
		RuleResponse response = service.runRules(facts, "VerifySupplier", RuleResponse.class);
		// then the business should be return a validation error
		// and a message should say the zipcode is empty
		// and a message should say the address is nul
		Assert.assertNotNull(response);
		Assert.assertNotNull(response.getBusiness());
		boolean foundZip = false;
		boolean foundAdd1 = false;
		for (Reason reason : response.getReasons()){
			if ( reason.getReasonMessage().equals( "zipcode is required") ){
				foundZip = true;
			}
			if ( reason.getReasonMessage().equals( "addressline1 is required") ){
				foundAdd1 = true;
			}
		}
		Assert.assertTrue("zipcode is required", foundZip);
		Assert.assertTrue("addressline1 is required", foundAdd1);
	}
	
	@Test
	public void shouldEnrichTheTaxIdWithZipCode(){
		// scenario: we need to enrich the taxId with the zipcode for system XYZ
		// given a business 
		// and the business' zipcode is 10002
		// and the business' taxId is 98765
		Collection<Object> facts = new ArrayList<Object>();
		Business business = new Business();
		business.setZipCode("10002");
		business.setFederalTaxId("98765");
		facts.add(business);
		// when I apply the enrichment rules
		RuleResponse response = service.runRules(facts, "VerifySupplier", RuleResponse.class);
		// then the business' taxId should be enriched to 98765-10002
		Assert.assertEquals("98765-10002", response.getBusiness().getFederalTaxId());
	}
	
}
