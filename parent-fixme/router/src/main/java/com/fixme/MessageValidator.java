// Validate the message received, ensuring it respects the FIX format and expected values

/*

FIX messages are structured as a series of tag-value pairs separated by delimiters. Each field follows the format:
tag=value|

| Sender ID      | 49 | SenderCompID - Unique sender identifier | `49=SENDER001 |
| Order Side     | 54 | Side - Buy ('1') or Sell ('2')          | `54=1 |
| Product/Symbol | 55 | Symbol - Product identifier             | `55=AAPL |
| Market         | 207| SecurityExchange - Market identifier    | `207=NASDAQ |
| Quantity       | 38 | OrderQty - Order quantity               | `38=100 |
| Price          | 44 | Price - Order price                     | `44=150.25 |
| Checksum       | 10 | CheckSum - Message checksum             | `10=123

Example :

49=SENDER001|54=1|55=AAPL|207=NASDAQ|38=100|44=150.25|10=123|

 */

package com.fixme;

import java.util.Map;
import java.util.HashMap;

public class MessageValidator {

    private static final char SOH = '\u0001'; // Standard FIX delimiter

    public static class FIXMessage {
        public Map<String, String> fields = new HashMap<>();

        public String getField(String tag) {
            return fields.get(tag);
        }

        public void setField(String tag, String value) {
            fields.put(tag, value);
        }

        public String getSenderID() { return getField("49"); }
        public String getOrderType() { return getField("54"); }
        public String getProduct() { return getField("55"); }
        public String getMarket() { return getField("207"); }
        public String getQuantity() { return getField("38"); }
        public String getPrice() { return getField("44"); }
        public String getChecksum() { return getField("10"); }

        public boolean isBuyOrder() { return getOrderType().equals("1"); }
        public boolean isSellOrder() { return getOrderType().equals("2"); }
    }

    public static FIXMessage parse(String message) throws IllegalArgumentException {
        if (message == null || message.isEmpty()) {
            throw new IllegalArgumentException("Message cannot be null or empty");
        }

        FIXMessage fixMessage = new FIXMessage();
        String[] fields = message.split(String.valueOf(SOH));

        for (String field : fields) {

            if (field.isEmpty()) { continue; }

            int equalIndex = field.indexOf('=');
            if (equalIndex == -1) {
                throw new IllegalArgumentException("Invalid field format: " + field);
            }

            String tag = field.substring(0, equalIndex);
            String value = field.substring(equalIndex + 1);

            fixMessage.setField(tag, value);
        }

        return fixMessage;
    }

    public static String calculateChecksum(String message) {
        if (message == null || message.isEmpty()) {
            return "000";
        }

        int checksumIndex = message.lastIndexOf("10=");
        if (checksumIndex == -1) {
            checksumIndex = message.length();
        }

        int sum = 0;
        for (int i = 0; i < checksumIndex; i++) {
            sum += (int) message.charAt(i);
        }

        int checksum = sum % 256;

        // Format as 3-digit zero-padded string
        return String.format("%03d", checksum);
    }


    public static boolean validate(FIXMessage message, String expectedChecksum) {

        if (message.getSenderID() == null || message.getSenderID().isEmpty()) {
            return false;
        }

        if (message.getOrderType() == null ||
                (!message.getOrderType().equals("1") && !message.getOrderType().equals("2"))) {
            return false;
        }

        if (message.getProduct() == null || message.getProduct().isEmpty()) {
            return false;
        }

        if (message.getMarket() == null || message.getMarket().isEmpty()) {
            return false;
        }

        if (message.getQuantity() == null) {
            return false;
        }

        if (message.getPrice() == null) {
            return false;
        }

        if (message.getChecksum() == null
        || !message.getChecksum().equals(expectedChecksum)) {
            return false;
        }

        // Validate numeric fields
        try {
            Double.parseDouble(message.getQuantity());
            Double.parseDouble(message.getPrice());
            Integer.parseInt(message.getChecksum());
        } catch (NumberFormatException e) {
            return false;
        }

        return true;
    }
}