package entity;

public class Card {
    private String cardNumber;
    private String expireDate;
    private double amount;

    public String getExpireDate() {
        return expireDate;
    }

    public void setExpireDate(String expireDate) {
        this.expireDate = expireDate;
    }

    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    public Card(String cardNumber, String expireDate, double amount) {
        this.cardNumber = cardNumber;
        this.expireDate = expireDate;
        this.amount = amount;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    @Override
    public String toString() {
        return """
                Card details:
                    card number: %s,
                    expire date: %s,
                    amount: $ %.2f
                """.formatted(cardNumber, expireDate, amount);
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }
}
