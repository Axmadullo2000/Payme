package entity;

import java.util.ArrayList;

public class User {
    private String userName;
    private String password;
    private String phoneNumber;
    private ArrayList<Card> cards;

    public ArrayList<Card> getCards() {
        return cards;
    }

    public void setCards(ArrayList<Card> cards) {
        this.cards = cards;
    }

    public User(String userName, String password, String phoneNumber) {
        this.userName = userName;
        this.password = password;
        this.phoneNumber = phoneNumber;
    }

    public User(String userName, String password, String phoneNumber, ArrayList<Card> card) {
        this.userName = userName;
        this.password = password;
        this.phoneNumber = phoneNumber;
        this.cards = card;
    }

    public String getUserName() {
        return userName;
    }

    public String getPassword() {
        return password;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    @Override
    public String toString() {
        return """
                User info:
                    Name: %s,
                    Phone number: %s,
                    Card: %s
                """.formatted(userName, phoneNumber, cards);
    }
}
