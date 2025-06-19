package com.fixme;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class MessageValidatorTest {

    private static final char SOH = '\u0001'; // FIX delimiter

    @Test
    @DisplayName("Should parse valid FIX message correctly")
    void testParseValidMessage() {
        // Given
        String message = "49=SENDER001" + SOH + "54=1" + SOH + "55=AAPL" + SOH +
                "207=NASDAQ" + SOH + "38=100" + SOH + "44=150.25" + SOH + "10=063" + SOH;

        // When
        MessageValidator.FIXMessage fixMessage = MessageValidator.parse(message);

        // Then
        assertNotNull(fixMessage);
        assertEquals("SENDER001", fixMessage.getSenderID());
        assertEquals("1", fixMessage.getOrderType());
        assertEquals("AAPL", fixMessage.getProduct());
        assertEquals("NASDAQ", fixMessage.getMarket());
        assertEquals("100", fixMessage.getQuantity());
        assertEquals("150.25", fixMessage.getPrice());
        assertEquals("063", fixMessage.getChecksum());
        assertTrue(fixMessage.isBuyOrder());
        assertFalse(fixMessage.isSellOrder());
    }

    @Test
    @DisplayName("Should parse sell order correctly")
    void testParseSellOrder() {
        // Given
        String message = "49=SELLER123" + SOH + "54=2" + SOH + "55=MSFT" + SOH +
                "207=NYSE" + SOH + "38=50" + SOH + "44=300.75" + SOH + "10=123" + SOH;

        // When
        MessageValidator.FIXMessage fixMessage = MessageValidator.parse(message);

        // Then
        assertEquals("2", fixMessage.getOrderType());
        assertTrue(fixMessage.isSellOrder());
        assertFalse(fixMessage.isBuyOrder());
    }

    @Test
    @DisplayName("Should throw exception for null message")
    void testParseNullMessage() {
        assertThrows(IllegalArgumentException.class, () -> {
            MessageValidator.parse(null);
        });
    }

    @Test
    @DisplayName("Should throw exception for empty message")
    void testParseEmptyMessage() {
        assertThrows(IllegalArgumentException.class, () -> {
            MessageValidator.parse("");
        });
    }

    @Test
    @DisplayName("Should throw exception for invalid field format")
    void testParseInvalidFieldFormat() {
        // Given - message without equals sign
        String invalidMessage = "49SENDER001" + SOH + "54=1" + SOH;

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            MessageValidator.parse(invalidMessage);
        });
    }

    @Test
    @DisplayName("Should calculate checksum correctly")
    void testCalculateChecksum() {
        // Given
        String messageWithoutChecksum = "49=SENDER001" + SOH + "54=1" + SOH + "55=AAPL" + SOH;

        // When
        String calculatedChecksum = MessageValidator.calculateChecksum(messageWithoutChecksum);

        // Then
        assertNotNull(calculatedChecksum);
        assertEquals(3, calculatedChecksum.length());
        assertTrue(calculatedChecksum.matches("\\d{3}"));
    }

    @Test
    @DisplayName("Should calculate checksum as 000 for null or empty message")
    void testCalculateChecksumForNullOrEmpty() {
        assertEquals("000", MessageValidator.calculateChecksum(null));
        assertEquals("000", MessageValidator.calculateChecksum(""));
    }

    @Test
    @DisplayName("Should validate message with correct checksum")
    void testValidateValidMessage() {
        // Given
        MessageValidator.FIXMessage fixMessage = new MessageValidator.FIXMessage();
        fixMessage.setField("49", "SENDER001");
        fixMessage.setField("54", "1");
        fixMessage.setField("55", "AAPL");
        fixMessage.setField("207", "NASDAQ");
        fixMessage.setField("38", "100");
        fixMessage.setField("44", "150.25");
        fixMessage.setField("10", "123");

        // When & Then
        assertTrue(MessageValidator.validate(fixMessage, "123"));
    }

    @Test
    @DisplayName("Should reject message with incorrect checksum")
    void testValidateIncorrectChecksum() {
        // Given
        MessageValidator.FIXMessage fixMessage = new MessageValidator.FIXMessage();
        fixMessage.setField("49", "SENDER001");
        fixMessage.setField("54", "1");
        fixMessage.setField("55", "AAPL");
        fixMessage.setField("207", "NASDAQ");
        fixMessage.setField("38", "100");
        fixMessage.setField("44", "150.25");
        fixMessage.setField("10", "999");

        // When & Then
        assertFalse(MessageValidator.validate(fixMessage, "123"));
    }

    @Test
    @DisplayName("Should reject message with missing sender ID")
    void testValidateMissingSenderID() {
        // Given
        MessageValidator.FIXMessage fixMessage = new MessageValidator.FIXMessage();
        fixMessage.setField("54", "1");
        fixMessage.setField("55", "AAPL");
        fixMessage.setField("207", "NASDAQ");
        fixMessage.setField("38", "100");
        fixMessage.setField("44", "150.25");
        fixMessage.setField("10", "123");

        // When & Then
        assertFalse(MessageValidator.validate(fixMessage, "123"));
    }

    @Test
    @DisplayName("Should reject message with empty sender ID")
    void testValidateEmptySenderID() {
        // Given
        MessageValidator.FIXMessage fixMessage = new MessageValidator.FIXMessage();
        fixMessage.setField("49", "");
        fixMessage.setField("54", "1");
        fixMessage.setField("55", "AAPL");
        fixMessage.setField("207", "NASDAQ");
        fixMessage.setField("38", "100");
        fixMessage.setField("44", "150.25");
        fixMessage.setField("10", "123");

        // When & Then
        assertFalse(MessageValidator.validate(fixMessage, "123"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "invalid", "3", "0"})
    @DisplayName("Should reject invalid order types")
    void testValidateInvalidOrderTypes(String invalidOrderType) {
        // Given
        MessageValidator.FIXMessage fixMessage = new MessageValidator.FIXMessage();
        fixMessage.setField("49", "SENDER001");
        fixMessage.setField("54", invalidOrderType);
        fixMessage.setField("55", "AAPL");
        fixMessage.setField("207", "NASDAQ");
        fixMessage.setField("38", "100");
        fixMessage.setField("44", "150.25");
        fixMessage.setField("10", "123");

        // When & Then
        assertFalse(MessageValidator.validate(fixMessage, "123"));
    }

    @Test
    @DisplayName("Should reject message with missing product")
    void testValidateMissingProduct() {
        // Given
        MessageValidator.FIXMessage fixMessage = new MessageValidator.FIXMessage();
        fixMessage.setField("49", "SENDER001");
        fixMessage.setField("54", "1");
        fixMessage.setField("207", "NASDAQ");
        fixMessage.setField("38", "100");
        fixMessage.setField("44", "150.25");
        fixMessage.setField("10", "123");

        // When & Then
        assertFalse(MessageValidator.validate(fixMessage, "123"));
    }

    @Test
    @DisplayName("Should reject message with missing market")
    void testValidateMissingMarket() {
        // Given
        MessageValidator.FIXMessage fixMessage = new MessageValidator.FIXMessage();
        fixMessage.setField("49", "SENDER001");
        fixMessage.setField("54", "1");
        fixMessage.setField("55", "AAPL");
        fixMessage.setField("38", "100");
        fixMessage.setField("44", "150.25");
        fixMessage.setField("10", "123");

        // When & Then
        assertFalse(MessageValidator.validate(fixMessage, "123"));
    }

    @Test
    @DisplayName("Should reject message with invalid quantity")
    void testValidateInvalidQuantity() {
        // Given
        MessageValidator.FIXMessage fixMessage = new MessageValidator.FIXMessage();
        fixMessage.setField("49", "SENDER001");
        fixMessage.setField("54", "1");
        fixMessage.setField("55", "AAPL");
        fixMessage.setField("207", "NASDAQ");
        fixMessage.setField("38", "invalid_quantity");
        fixMessage.setField("44", "150.25");
        fixMessage.setField("10", "123");

        // When & Then
        assertFalse(MessageValidator.validate(fixMessage, "123"));
    }

    @Test
    @DisplayName("Should reject message with invalid price")
    void testValidateInvalidPrice() {
        // Given
        MessageValidator.FIXMessage fixMessage = new MessageValidator.FIXMessage();
        fixMessage.setField("49", "SENDER001");
        fixMessage.setField("54", "1");
        fixMessage.setField("55", "AAPL");
        fixMessage.setField("207", "NASDAQ");
        fixMessage.setField("38", "100");
        fixMessage.setField("44", "invalid_price");
        fixMessage.setField("10", "123");

        // When & Then
        assertFalse(MessageValidator.validate(fixMessage, "123"));
    }

    @Test
    @DisplayName("Should reject message with invalid checksum format")
    void testValidateInvalidChecksumFormat() {
        // Given
        MessageValidator.FIXMessage fixMessage = new MessageValidator.FIXMessage();
        fixMessage.setField("49", "SENDER001");
        fixMessage.setField("54", "1");
        fixMessage.setField("55", "AAPL");
        fixMessage.setField("207", "NASDAQ");
        fixMessage.setField("38", "100");
        fixMessage.setField("44", "150.25");
        fixMessage.setField("10", "invalid_checksum");

        // When & Then
        assertFalse(MessageValidator.validate(fixMessage, "invalid_checksum"));
    }

    @Test
    @DisplayName("Should handle fields with empty values")
    void testParseFieldsWithEmptyValues() {
        // Given
        String message = "49=" + SOH + "54=1" + SOH + "55=AAPL" + SOH + "10=123" + SOH;

        // When
        MessageValidator.FIXMessage fixMessage = MessageValidator.parse(message);

        // Then
        assertEquals("", fixMessage.getSenderID());
        assertEquals("1", fixMessage.getOrderType());
        assertEquals("AAPL", fixMessage.getProduct());
    }

    @Test
    @DisplayName("Should validate both buy and sell orders correctly")
    void testValidateBothOrderTypes() {
        // Given - Buy order
        MessageValidator.FIXMessage buyOrder = new MessageValidator.FIXMessage();
        buyOrder.setField("49", "BUYER001");
        buyOrder.setField("54", "1");
        buyOrder.setField("55", "AAPL");
        buyOrder.setField("207", "NASDAQ");
        buyOrder.setField("38", "100");
        buyOrder.setField("44", "150.25");
        buyOrder.setField("10", "123");

        // Given - Sell order
        MessageValidator.FIXMessage sellOrder = new MessageValidator.FIXMessage();
        sellOrder.setField("49", "SELLER001");
        sellOrder.setField("54", "2");
        sellOrder.setField("55", "MSFT");
        sellOrder.setField("207", "NYSE");
        sellOrder.setField("38", "50");
        sellOrder.setField("44", "300.75");
        sellOrder.setField("10", "456");

        // When & Then
        assertTrue(MessageValidator.validate(buyOrder, "123"));
        assertTrue(MessageValidator.validate(sellOrder, "456"));
        assertTrue(buyOrder.isBuyOrder());
        assertTrue(sellOrder.isSellOrder());
    }
}
