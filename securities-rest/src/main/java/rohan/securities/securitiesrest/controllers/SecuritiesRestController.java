package rohan.securities.securitiesrest.controllers;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import rohan.securities.securitiesrest.model.Security;
import rohan.securities.securitiesrest.services.SecurityService;

@RestController
public class SecuritiesRestController {

	@Autowired
	private SecurityService securityService;
	
	@GetMapping("/getSecurities")
	public Collection<Security> getSecurities(){
		return securityService.getSecurities();
	}
	
}
