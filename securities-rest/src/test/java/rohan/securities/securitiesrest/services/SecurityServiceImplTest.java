package rohan.securities.securitiesrest.services;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.springframework.test.context.junit4.SpringRunner;

public class SecurityServiceImplTest {
	
	@Test
	public void testGetSecurities() throws ParseException{
		NumberFormat f = NumberFormat.getCurrencyInstance(Locale.US);
		
		Object o = f.parse("31-Dec-13 ( $ m )");
		
		assert true;
	}
}
