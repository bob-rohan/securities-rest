package rohan.securities.securitiesrest.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Fundamental implements Comparable {

	private LocalDate periodEnd;

	private BigDecimal basicEarningsPerShare;

	private BigDecimal dividendPerShare;

	private BigDecimal totalEquity;

	public LocalDate getPeriodEnd() {
		return periodEnd;
	}

	public void setPeriodEnd(LocalDate periodEnd) {
		this.periodEnd = periodEnd;
	}

	public BigDecimal getBasicEarningsPerShare() {
		return basicEarningsPerShare;
	}

	public void setBasicEarningsPerShare(BigDecimal basicEarningsPerShare) {
		this.basicEarningsPerShare = basicEarningsPerShare;
	}

	public BigDecimal getDividendPerShare() {
		return dividendPerShare;
	}

	public void setDividendPerShare(BigDecimal dividendPerShare) {
		this.dividendPerShare = dividendPerShare;
	}

	public BigDecimal getTotalEquity() {
		return totalEquity;
	}

	public void setTotalEquity(BigDecimal totalEquity) {
		this.totalEquity = totalEquity;
	}

	@Override
	public int compareTo(Object arg0) {
		if (arg0 instanceof Fundamental) {
			return this.getPeriodEnd().compareTo(((Fundamental) arg0).getPeriodEnd());
		} else {
			return 0;
		}
	}

}
