package rohan.securities.securitiesrest.services;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriTemplate;

import rohan.securities.securitiesrest.model.Fundamental;
import rohan.securities.securitiesrest.model.Security;

@Service
public class SecurityServiceImpl implements SecurityService {

	private final static Logger LOG = LoggerFactory.getLogger(SecurityServiceImpl.class);

	private final static String SECURITIES_DATA_URL = "http://www.londonstockexchange.com/exchange/prices-and-markets/stocks/indices/summary/summary-indices-constituents.html?index=UKX&page={pageNumber}";

	private final static String FUNDAMENTALS_DATA_URL = "http://www.londonstockexchange.com/exchange/prices/stocks/summary/fundamentals.html?fourWayKey={fourWayKey}";
	
	private final static BigDecimal CONVERSION_RATE = new BigDecimal(1.39d);

	private static Collection<Security> securities;
	
	@Autowired
	private Environment environment;

	@Autowired
	private RestTemplate restTemplate;

	public Collection<Security> getSecurities() {
		if (securities == null || environment.acceptsProfiles("dev")) {
			/**
			 * get constituents
			 */
			securities = getConstituents();

			/**
			 * get fundamentals for constituent
			 */
			getFundamentals(securities);

		}

		return securities;
	}

	private Collection<Security> getFundamentals(Collection<Security> constituents) {

		constituents.forEach(constituent -> {
			// get HTML
			Map<String, Object> uriVariables = new HashMap<>();
			uriVariables.put("fourWayKey", constituent.getFourWayKey());

			RequestEntity request = RequestEntity.get(new UriTemplate(FUNDAMENTALS_DATA_URL).expand(uriVariables))
					.header("User-Agent", "Mozilla/5.0").build();

			ResponseEntity<String> response = restTemplate.exchange(request, String.class);

			String contituentsHtml = response.getBody();

			LOG.trace(contituentsHtml);

			// parse HTML
			try {
				Document document = Jsoup.parse(contituentsHtml);

				Elements tables = document.getElementsByClass("table_dati");

				for (Element table : tables) {
					if (table.attr("summary").equalsIgnoreCase("Company Information")) {
						parseCompanyInformation(constituent, table);
					}
				}

				parseFundamentals(constituent, tables);

			} catch (Exception e) {
				// TODO: handle exception
			}
		});

		return constituents;
	}

	private Security parseFundamentals(Security security, Elements tables) {
		
		LOG.info("Parse security: {}", security.getCode());
		
		security.setFundamentals(new TreeSet<>());

		Element incomeStatementTable = tables.get(0);
		parseIncomeStatement(security, incomeStatementTable);

		Element balanceSheetTable = tables.get(1);
		parseBalanceSheet(security, balanceSheetTable);

		return security;
	}

	private Security parseBalanceSheet(Security security, Element table) {
		Collection<Fundamental> fundamentals = security.getFundamentals();

		Element tableBody = table.child(1);

		Iterator<Element> tableBodyRows = tableBody.children().iterator();

		// loop data
		while (tableBodyRows.hasNext()) {

			Element tableBodyRow = tableBodyRows.next();

			if (tableBodyRow.child(0).text().contains("Total Equity")) {

				Iterator<Element> tableBodyRowDatas = tableBodyRow.children().iterator();
				Iterator<Fundamental> f = fundamentals.iterator();

				// skip title column
				tableBodyRowDatas.next();

				while (tableBodyRowDatas.hasNext()) {

					Element tableBodyData = tableBodyRowDatas.next();

					String totalEquity = tableBodyData.text();

					f.next().setTotalEquity(new BigDecimal(totalEquity));
				}

			}
		}

		security.setFundamentals(fundamentals);

		return security;
	}

	private Security parseIncomeStatement(Security security, Element table) {

		Collection<Fundamental> fundamentals = security.getFundamentals();

		Element tableHead = table.child(0);
		Element tableBody = table.child(1);

		Element tableHeadRow = tableHead.child(0);

		Iterator<Element> tableHeadDatas = tableHeadRow.children().iterator();

		// skip title column
		tableHeadDatas.next();

		// setup periods
		boolean quotedGBP = false;
		
		while (tableHeadDatas.hasNext()) {

			Element tableHeadData = tableHeadDatas.next();

			Fundamental fundamental = new Fundamental();

			String period = tableHeadData.text().trim();

			fundamental.setPeriodEnd(parsePeriod(period));
			
			quotedGBP = period.contains("£");

			fundamentals.add(fundamental);
		}
		
		if(!quotedGBP){
			// TODO assumed US,+ hardcoded conversion rate
			security.setPrice(security.getPrice().multiply(CONVERSION_RATE));
			security.setMarketCapitalization(security.getMarketCapitalization().multiply(CONVERSION_RATE));
		}

		Iterator<Element> tableBodyRows = tableBody.children().iterator();

		// loop data
		while (tableBodyRows.hasNext()) {

			Element tableBodyRow = tableBodyRows.next();

			if (tableBodyRow.child(0).text().contains("Earnings per Share - Basic")) {

				Iterator<Element> tableBodyRowDatas = tableBodyRow.children().iterator();
				Iterator<Fundamental> f = fundamentals.iterator();

				// skip title column
				tableBodyRowDatas.next();

				while (tableBodyRowDatas.hasNext()) {

					Element tableBodyData = tableBodyRowDatas.next();

					String earningsPerShareBasic = tableBodyData.text();

					if(earningsPerShareBasic.equals("n/a")){
						f.next().setBasicEarningsPerShare(BigDecimal.ZERO);
					} else {
						earningsPerShareBasic = earningsPerShareBasic.substring(0, earningsPerShareBasic.length() - 1);

						f.next().setBasicEarningsPerShare(new BigDecimal(earningsPerShareBasic));
					}
				}

			}

			if (tableBodyRow.child(0).text().contains("Dividend per Share")) {
				Iterator<Element> tableBodyRowDatas = tableBodyRow.children().iterator();
				Iterator<Fundamental> f = fundamentals.iterator();

				// skip title column
				tableBodyRowDatas.next();

				while (tableBodyRowDatas.hasNext()) {

					Element tableBodyData = tableBodyRowDatas.next();

					String dividendPerShare = tableBodyData.text();

					if(dividendPerShare.equals("n/a")){
						f.next().setDividendPerShare(BigDecimal.ZERO);
					} else {
						dividendPerShare = dividendPerShare.substring(0, dividendPerShare.length() - 1);
						f.next().setDividendPerShare(new BigDecimal(dividendPerShare));
					}					
				}
			}
		}

		security.setFundamentals(fundamentals);

		return security;
	}

	private LocalDate parsePeriod(String period) {
		String[] periodTokens = period.split(" ");

		period = periodTokens[0];

		LocalDate localDatePeriod = LocalDate.parse(period, DateTimeFormatter.ofPattern("dd-MMM-yy"));

		return localDatePeriod;
	}

	private Security parseCompanyInformation(Security security, Element table) {
		Element tableBody = table.child(1);

		Element marketCapitalizationRow = tableBody.child(2);

		String marketCapitalization = marketCapitalizationRow.child(1).text();

		BigDecimal marketCapitalizationDecimal = parseMarketCapitalization(marketCapitalization);

		security.setMarketCapitalization(marketCapitalizationDecimal);

		return security;
	}

	private BigDecimal parseMarketCapitalization(String marketCapitalization) {
		marketCapitalization = marketCapitalization.replaceAll(",", "");

		Matcher matcher = Pattern.compile("\\d.*").matcher(marketCapitalization);
		matcher.find();

		return new BigDecimal(matcher.group());
	}

	private String getConstituentsPage(int pageNumber) {
		// get HTML
		Map<String, Object> uriVariables = new HashMap<>();
		uriVariables.put("pageNumber", pageNumber);

		RequestEntity request = RequestEntity.get(new UriTemplate(SECURITIES_DATA_URL).expand(uriVariables))
				.header("User-Agent", "Mozilla/5.0").build();

		ResponseEntity<String> response = restTemplate.exchange(request, String.class);

		String contituentsHtml = response.getBody();

		LOG.trace(contituentsHtml);

		return contituentsHtml;
	}

	private Collection<Security> getConstituents() {

		Collection<Security> securities = new ArrayList<>();
		int pageNumber = 0;

		// TODO: hardcoded max page num
		while (pageNumber < 7) {
			pageNumber++;
			String contituentsHtml = getConstituentsPage(pageNumber);
			parseConstituentsPage(securities, contituentsHtml);
		}

		return securities;

	}

	private Collection<Security> parseConstituentsPage(Collection<Security> securities, String contituentsHtml) {
		try {
			Document document = Jsoup.parse(contituentsHtml);

			Elements table = document.getElementsByClass("table_dati");

			Element tableBody = table.first().child(1);

			for (Element tableRow : tableBody.children()) {
				Security security = new Security();

				final String securityCode = tableRow.child(0).text();
				security.setCode(securityCode);

				final String securityUri = tableRow.child(1).child(0).attr("href");
				final String securityKey = getKeyFromSecurityUri(securityUri);
				security.setFourWayKey(securityKey);

				final String securityPrice = tableRow.child(3).text();
				final BigDecimal securityDecimalPrice = new BigDecimal(securityPrice.replaceAll(",", ""));
				security.setPrice(securityDecimalPrice);

				LOG.debug("Security {}", security);

				securities.add(security);
			}

		} catch (Exception e) {
			// TODO:
		}

		LOG.trace("Securities {}", securities);

		// loop pages

		return securities;
	}

	private String getKeyFromSecurityUri(final String securityUri) {
		String[] tokens = securityUri.split("/");

		String key = tokens[tokens.length - 1].replace(".html", "");

		return key;
	}

}
