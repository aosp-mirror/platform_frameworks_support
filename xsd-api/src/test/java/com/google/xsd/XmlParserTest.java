package com.google.xsd;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

import org.junit.Test;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

public class XmlParserTest {
    @Test
    public void testPurchaseSimple() throws Exception {
        TestCompilationResult result;
        try (InputStream in = this.getClass().getClassLoader().getResourceAsStream("purchase_simple.xsd")) {
            result = TestHelper.parseXsdAndCompile(in);
        }

        Class<?> xmlParser = result.loadClass("XmlParser");
        Class<?> purchaseOrderType = result.loadClass("PurchaseOrderType");
        Class<?> usAddress = result.loadClass("USAddress");

        Object instance;
        try (InputStream in = this.getClass().getClassLoader().getResourceAsStream("purchase_simple.xml")) {
            instance = xmlParser.getMethod("read", InputStream.class).invoke(null, in);
        }
        Object billTo = purchaseOrderType.getMethod("getBillTo").invoke(instance);
        List shipToList = (List)purchaseOrderType.getMethod("getShipTo").invoke(instance);

        String name = (String)usAddress.getMethod("getName").invoke(billTo);
        BigInteger zip = (BigInteger)usAddress.getMethod("getZip").invoke(billTo);
        String street = (String)usAddress.getMethod("getStreet").invoke(shipToList.get(0));
        BigInteger largeZip = (BigInteger)usAddress.getMethod("getZip").invoke(shipToList.get(1));
        XMLGregorianCalendar orderDate = (XMLGregorianCalendar)purchaseOrderType.getMethod("getOrderDate").invoke(instance);

        assertThat(name, is("billName"));
        assertThat(zip, is(new BigInteger("1")));
        assertThat(street, is("street1"));
        assertThat(largeZip, is(new BigInteger("-79228162514264337593543950335")));
        assertThat(orderDate, is(javax.xml.datatype.DatatypeFactory.newInstance().newXMLGregorianCalendar("1900-01-01")));
    }

    @Test
    public void testNestedType() throws Exception {
        TestCompilationResult result;
        try (InputStream in = this.getClass().getClassLoader().getResourceAsStream("nested_type.xsd")) {
            result = TestHelper.parseXsdAndCompile(in);
        }

        Class<?> xmlParser = result.loadClass("XmlParser");
        Class<?> employee = result.loadClass("Employee");
        Class<?> address = result.loadClass("Employee$Address");
        Class<?> extra = result.loadClass("Employee$Address$Extra");

        Object instance;
        try (InputStream in = this.getClass().getClassLoader().getResourceAsStream("nested_type.xml")) {
            instance = xmlParser.getMethod("read", InputStream.class).invoke(null, in);
        }

        String name = (String)employee.getMethod("getName").invoke(instance);
        Object addressInstance = employee.getMethod("getAddress").invoke(instance);
        String country = (String)address.getMethod("getCountry").invoke(addressInstance);
        Object extraInstance = address.getMethod("getExtra").invoke(addressInstance);
        String line2 = (String)extra.getMethod("getLine2").invoke(extraInstance);

        assertThat(name, is("Peter"));
        assertThat(country, is("US"));
        assertThat(line2, is("Good Street"));
    }

    @Test
    public void testSimpleComplexContent() throws Exception {
        TestCompilationResult result;
        try (InputStream in = this.getClass().getClassLoader().getResourceAsStream("simple_complex_content.xsd")) {
            result = TestHelper.parseXsdAndCompile(in);
        }

        Class<?> xmlParser = result.loadClass("XmlParser");
        Class<?> person = result.loadClass("Person");
        Class<?> shoeSize = result.loadClass("ShoeSize");
        Class<?> generalPrice = result.loadClass("GeneralPrice");
        Class<?> usAddress = result.loadClass("USAddress");
        Class<?> krAddress = result.loadClass("KRAddress");

        Object instance;
        try (InputStream in = this.getClass().getClassLoader().getResourceAsStream("simple_complex_content.xml")) {
            instance = xmlParser.getMethod("read", InputStream.class).invoke(null, in);
        }

        String name = (String)person.getMethod("getName").invoke(instance);
        Object shoeSizeInstance = person.getMethod("getShoeSize").invoke(instance);
        String sizing = (String)shoeSize.getMethod("getSizing").invoke(shoeSizeInstance);
        BigDecimal shoeSizeValue = (BigDecimal)shoeSize.getMethod("getValue").invoke(shoeSizeInstance);

        Object generalPriceInstance = person.getMethod("getGeneralPrice").invoke(instance);
        String currency = (String)generalPrice.getMethod("getCurrency").invoke(generalPriceInstance);
        BigDecimal generalPriceValue = (BigDecimal)generalPrice.getMethod("getValue").invoke(generalPriceInstance);

        Object usAddressInstance = person.getMethod("getUSAddress").invoke(instance);
        String usStreet = (String)usAddress.getMethod("getStreet").invoke(usAddressInstance);
        BigInteger usZipcode = (BigInteger)usAddress.getMethod("getZipcode").invoke(usAddressInstance);

        Object krAddressInstance = person.getMethod("getKRAddress").invoke(instance);
        String krStreet = (String)krAddress.getMethod("getStreet").invoke(krAddressInstance);

        assertThat(name, is("Petr"));
        assertThat(sizing, is("Korea"));
        assertThat(shoeSizeValue, is(new BigDecimal("265")));
        assertThat(currency, is("dollar"));
        assertThat(generalPriceValue, is(new BigDecimal("1234.5678")));
        assertThat(usStreet, is("street fighter"));
        assertThat(usZipcode, is(new BigInteger("3232323183298523436434")));
        assertThat(krStreet, is("Nokdu Street"));
    }

    @Test
    public void testPredefinedTypes() throws Exception {
        TestCompilationResult result;
        try (InputStream in = this.getClass().getClassLoader().getResourceAsStream("predefined_types.xsd")) {
            result = TestHelper.parseXsdAndCompile(in);
        }

        Class<?> xmlParser = result.loadClass("XmlParser");
        Class<?> types = result.loadClass("Types");
        Class<?> stringTypes = result.loadClass("StringTypes");
        Class<?> dateTypes = result.loadClass("DateTypes");
        Class<?> numericTypes = result.loadClass("NumericTypes");
        Class<?> miscTypes = result.loadClass("MiscTypes");

        Object instance;
        try (InputStream in = this.getClass().getClassLoader().getResourceAsStream("predefined_types.xml")) {
            instance = xmlParser.getMethod("read", InputStream.class).invoke(null, in);
        }

        {
            Object stringTypesInstance = types.getMethod("getStringTypes").invoke(instance);
            String string = (String) stringTypes.getMethod("getString").invoke(stringTypesInstance);
            String token = (String) stringTypes.getMethod("getToken").invoke(stringTypesInstance);
            String normalizedString = (String) stringTypes.getMethod("getNormalizedString").invoke(stringTypesInstance);
            String language = (String) stringTypes.getMethod("getLanguage").invoke(stringTypesInstance);
            String entity = (String) stringTypes.getMethod("getEntity").invoke(stringTypesInstance);
            List entities = (List) stringTypes.getMethod("getEntities").invoke(stringTypesInstance);
            String id = (String) stringTypes.getMethod("getId").invoke(stringTypesInstance);
            String name = (String) stringTypes.getMethod("getName").invoke(stringTypesInstance);
            String ncName = (String) stringTypes.getMethod("getNcname").invoke(stringTypesInstance);
            String nmToken = (String) stringTypes.getMethod("getNmtoken").invoke(stringTypesInstance);
            List nmTokens = (List) stringTypes.getMethod("getNmtokens").invoke(stringTypesInstance);

            assertThat(string, is("abcd"));
            assertThat(token, is("abcd"));
            assertThat(normalizedString, is("abcd"));
            assertThat(language, is("abcd"));
            assertThat(entity, is("abcd"));
            assertThat(entities, is(Arrays.asList("a", "b", "c", "d")));
            assertThat(id, is("abcd"));
            assertThat(name, is("abcd"));
            assertThat(ncName, is("abcd"));
            assertThat(nmToken, is("abcd"));
            assertThat(nmTokens, is(Arrays.asList("a", "b", "c", "d")));
        }

        {
            Object dateTypesInstance = types.getMethod("getDateTypes").invoke(instance);
            XMLGregorianCalendar date = (XMLGregorianCalendar) dateTypes.getMethod("getDate").invoke(dateTypesInstance);
            XMLGregorianCalendar dateTime = (XMLGregorianCalendar) dateTypes.getMethod("getDateTime").invoke(dateTypesInstance);
            Duration duration = (Duration) dateTypes.getMethod("getDuration").invoke(dateTypesInstance);
            XMLGregorianCalendar gDay = (XMLGregorianCalendar) dateTypes.getMethod("getGDay").invoke(dateTypesInstance);
            XMLGregorianCalendar gMonth = (XMLGregorianCalendar) dateTypes.getMethod("getGMonth").invoke(dateTypesInstance);
            XMLGregorianCalendar gMonthDay = (XMLGregorianCalendar) dateTypes.getMethod("getGMonthDay").invoke(dateTypesInstance);
            XMLGregorianCalendar gYear = (XMLGregorianCalendar) dateTypes.getMethod("getGYear").invoke(dateTypesInstance);
            XMLGregorianCalendar gYearMonth = (XMLGregorianCalendar) dateTypes.getMethod("getGYearMonth").invoke(dateTypesInstance);
            XMLGregorianCalendar time = (XMLGregorianCalendar) dateTypes.getMethod("getTime").invoke(dateTypesInstance);

            DatatypeFactory datatypeFactory = DatatypeFactory.newInstance();
            assertThat(date, is(datatypeFactory.newXMLGregorianCalendar("2018-06-18")));
            assertThat(dateTime, is(datatypeFactory.newXMLGregorianCalendar("2018-06-18T21:32:52")));
            assertThat(duration, is(datatypeFactory.newDuration("P3M")));
            assertThat(gDay, is(datatypeFactory.newXMLGregorianCalendar("---18")));
            assertThat(gMonth, is(datatypeFactory.newXMLGregorianCalendar("--06")));
            assertThat(gMonthDay, is(datatypeFactory.newXMLGregorianCalendar("--06-18")));
            assertThat(gYear, is(datatypeFactory.newXMLGregorianCalendar("2018")));
            assertThat(gYearMonth, is(datatypeFactory.newXMLGregorianCalendar("2018-06")));
            assertThat(time, is(datatypeFactory.newXMLGregorianCalendar("21:32:52")));
        }

        {
            Object numericTypesInstance = types.getMethod("getNumericTypes").invoke(instance);
            BigDecimal decimal = (BigDecimal)numericTypes.getMethod("getDecimal").invoke(numericTypesInstance);
            BigInteger integer = (BigInteger)numericTypes.getMethod("getInteger").invoke(numericTypesInstance);
            long _long = (long)numericTypes.getMethod("get_long").invoke(numericTypesInstance);
            int _int = (int)numericTypes.getMethod("get_int").invoke(numericTypesInstance);
            short _short = (short)numericTypes.getMethod("get_short").invoke(numericTypesInstance);
            byte _byte = (byte)numericTypes.getMethod("get_byte").invoke(numericTypesInstance);
            BigInteger negativeInteger = (BigInteger)numericTypes.getMethod("getNegativeInteger").invoke(numericTypesInstance);
            BigInteger nonNegativeInteger = (BigInteger)numericTypes.getMethod("getNonNegativeInteger").invoke(numericTypesInstance);
            BigInteger positiveInteger = (BigInteger)numericTypes.getMethod("getPositiveInteger").invoke(numericTypesInstance);
            BigInteger nonPositiveInteger = (BigInteger)numericTypes.getMethod("getNonPositiveInteger").invoke(numericTypesInstance);
            BigInteger unsignedLong = (BigInteger)numericTypes.getMethod("getUnsignedLong").invoke(numericTypesInstance);
            long unsignedInt = (long)numericTypes.getMethod("getUnsignedInt").invoke(numericTypesInstance);
            int unsignedShort = (int)numericTypes.getMethod("getUnsignedShort").invoke(numericTypesInstance);
            short unsignedByte = (short)numericTypes.getMethod("getUnsignedByte").invoke(numericTypesInstance);

            assertThat(decimal, is(new BigDecimal("1234.5678")));
            assertThat(integer, is(new BigInteger("123456789012345678901234567890")));
            assertThat(_long, is(9223372036854775807L));
            assertThat(_int, is(2147483647));
            assertThat(_short, is((short)32767));
            assertThat(_byte, is((byte)127));
            assertThat(negativeInteger, is(new BigInteger("-1234")));
            assertThat(nonNegativeInteger, is(new BigInteger("1234")));
            assertThat(positiveInteger, is(new BigInteger("1234")));
            assertThat(nonPositiveInteger, is(new BigInteger("-1234")));
            assertThat(unsignedLong, is(new BigInteger("1234")));
            assertThat(unsignedInt, is(1234L));
            assertThat(unsignedShort, is(1234));
            assertThat(unsignedByte, is((short)255));
        }

        {
            Object miscTypesInstance = types.getMethod("getMiscTypes").invoke(instance);
            double _double = (double)miscTypes.getMethod("get_double").invoke(miscTypesInstance);
            float _float = (float)miscTypes.getMethod("get_float").invoke(miscTypesInstance);
            String anyURI = (String)miscTypes.getMethod("getAnyURI").invoke(miscTypesInstance);
            byte[] base64Binary = (byte[])miscTypes.getMethod("getBase64Binary").invoke(miscTypesInstance);
            boolean _boolean = (boolean)miscTypes.getMethod("get_boolean").invoke(miscTypesInstance);
            BigInteger hexBinary = (BigInteger) miscTypes.getMethod("getHexBinary").invoke(miscTypesInstance);
            String qName = (String)miscTypes.getMethod("getQName").invoke(miscTypesInstance);
            String iDREF = (String)miscTypes.getMethod("getIDREF").invoke(miscTypesInstance);
            List iDREFS = (List)miscTypes.getMethod("getIDREFS").invoke(miscTypesInstance);
            String anyType = (String)miscTypes.getMethod("getAnyType").invoke(miscTypesInstance);

            assertThat(_double, is(1234.5678));
            assertThat(_float, is(123.456f));
            assertThat(anyURI, is("https://www.google.com"));
            assertThat(base64Binary, is(Base64.getDecoder().decode("Z29vZ2xl")));
            assertThat(_boolean, is(true));
            assertThat(hexBinary, is(new BigInteger("516a75cb56d7e7", 16)));
            assertThat(qName, is("abcd"));
            assertThat(iDREF, is("abcd"));
            assertThat(iDREFS, is(Arrays.asList("abcd", "abcd")));
            assertThat(anyType, is("abcd"));
        }
    }

    @Test
    public void testSimpleType() throws Exception {
        TestCompilationResult result;
        try (InputStream in = this.getClass().getClassLoader().getResourceAsStream("simple_type.xsd")) {
            result = TestHelper.parseXsdAndCompile(in);
        }

        Class<?> xmlParser = result.loadClass("XmlParser");
        Class<?> simpleTypes = result.loadClass("Simpletypes");

        Object instance;
        try (InputStream in = this.getClass().getClassLoader().getResourceAsStream("simple_type.xml")) {
            instance = xmlParser.getMethod("read", InputStream.class).invoke(null, in);
        }

        List listInt = (List)simpleTypes.getMethod("getListInt").invoke(instance);
        List uniontest = (List)simpleTypes.getMethod("getUniontest").invoke(instance);

        assertThat(listInt, is(Arrays.asList(1, 2, 3, 4, 5)));
        assertThat(uniontest, is(Arrays.asList("100")));
    }
}