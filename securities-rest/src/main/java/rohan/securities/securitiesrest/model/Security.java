package rohan.securities.securitiesrest.model;

import java.math.BigDecimal;
import java.util.Collection;

public class Security {

	private String code;
	
	// Used by http://www.londonstockexchange.com to identify security.
	private String fourWayKey;
	
	private BigDecimal price;
	
	private Collection<Fundamental> fundamentals;
	
	private BigDecimal marketCapitalization;

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getFourWayKey() {
		return fourWayKey;
	}

	public void setFourWayKey(String fourWayKey) {
		this.fourWayKey = fourWayKey;
	}

	public BigDecimal getPrice() {
		return price;
	}

	public void setPrice(BigDecimal price) {
		this.price = price;
	}

	public Collection<Fundamental> getFundamentals() {
		return fundamentals;
	}

	public void setFundamentals(Collection<Fundamental> fundamentals) {
		this.fundamentals = fundamentals;
	}

	public BigDecimal getMarketCapitalization() {
		return marketCapitalization;
	}

	public void setMarketCapitalization(BigDecimal marketCapitalization) {
		this.marketCapitalization = marketCapitalization;
	}
	
}
