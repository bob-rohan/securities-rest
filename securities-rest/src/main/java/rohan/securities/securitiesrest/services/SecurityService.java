package rohan.securities.securitiesrest.services;

import java.util.Collection;

import rohan.securities.securitiesrest.model.Security;

public interface SecurityService {

	Collection<Security> getSecurities();
}
