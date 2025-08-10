package dto;

public record AddCardRecord(String cardNumber, String expireDate) {
    public AddCardRecord {
        for (int i = 0; i < cardNumber.length(); i++) {
            if (!Character.isDigit(cardNumber.charAt(i))) {
                throw new RuntimeException("Enter only number values!");
            }
        }
    }
}
