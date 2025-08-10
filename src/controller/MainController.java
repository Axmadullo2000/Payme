package controller;

import dto.AddCardRecord;
import dto.LoginRecord;
import dto.RegisterRecord;
import entity.Card;
import entity.User;
import service.MainService;
import util.Util;

import java.util.ArrayList;
import java.util.Optional;


public class MainController {
    MainService mainService = new MainService();
    private int loginAttempts = 0;
    private long lockUntilMillis = 0;

    public void start(){

        while (true) {
            System.out.println("""
                1. Login
                2. Registration
                0. Exit
                """);
            int option = Util.getNumber("Choose an option");

            switch (option) {
                case 1 -> {
                    myLogin();
                }
                case 2 -> {
                    myRegistration();
                }
                case 0 -> {
                    mainService.switchOff();
                    System.out.println("Good bye!");
                    return;
                }
            }
        }
    }

    private void myRegistration(){
        String userName = Util.getText("Enter your name");
        String phoneNumber = Util.getText("Enter your phone number");
        String password = Util.getText("Enter your password");

        RegisterRecord registerRecord = new RegisterRecord(userName, password, phoneNumber);

        Optional<String> response = mainService.registerUser(registerRecord);

        if (response.isPresent()) {
            if (response.get().equals("response")) {
                System.out.println("User added successfully!");
                MainMenu();
            } else {
                System.out.println("Register error");
                String error = response.get();
                System.out.println(error);
            }
        }

    }

    private void myLogin() {
        long now = System.currentTimeMillis();

        if (now < lockUntilMillis) {
            long secLeft = (lockUntilMillis - now + 999) / 1000;
            System.out.println("⛔ Too many attempts. Please wait " + secLeft + " sec.");
            return;
        }

        String phoneNumber = Util.getText("Enter your phone number");
        String password = Util.getText("Enter your password");

        LoginRecord loginRecord = new LoginRecord(phoneNumber, password);
        Optional<String> response = mainService.loginUser(loginRecord);

        if (response.isPresent() && response.get().equals("response")) {
            loginAttempts = 0; // сброс при успешном входе
            System.out.println("✅ Logged in");
            MainMenu();
        } else {
            loginAttempts++;
            System.out.println(response.orElse("User not found! (attempt " + loginAttempts + ")"));

            if (loginAttempts >= 3) {
                loginAttempts = 0; // сбрасываем счётчик (или оставить, как тебе логика)
                lockUntilMillis = System.currentTimeMillis() + 30_000; // 30 секунд блокировки
                System.out.println("❌ Too many attempts — blocked for 30 seconds.");
            }
        }
    }


    private void MainMenu() {
        while (true) {
            Optional<User> response = mainService.getCurrentUser();

            if (response.isPresent() && response.get().getCards() != null && !response.get().getCards().isEmpty()) {
                System.out.println(response.get().getUserName());

                System.out.println("""
                1. Account balance
                2. Transfer of amount
                3. Add more card
                0. Log out
                """);

                int option = Util.getNumber("Choose an option");

                switch (option) {
                    case 1 -> {
                        getAccountOfBalance();
                    }
                    case 2 -> {
                        transferMoney();
                    }
                    case 3 -> {
                        addCard();
                    }
                    case 0 -> {
                        System.out.println("I am looking forward to seeing to you!");
                        return;
                    }
                }

            }else {
                System.out.println("Welcome " + mainService.getCurrentUser());
                System.out.println("""
                1. Add Card
                0. Log out
                """);

                int option = Util.getNumber("Choose an option");

                switch (option) {
                    case 1 -> {
                        addCard();
                    }
                    case 0 -> {
                        System.out.println("I am looking forward to seeing to you!");
                        return;
                    }
                }
            }

        }
    }

    private void addCard() {
        String cardNumber = Util.getText("Enter your card number");
        String expireDate = Util.getText("Enter card expire date(mm/yy)");

        AddCardRecord addCardRecord = new AddCardRecord(cardNumber, expireDate);

        Optional<String> response = mainService.addCreditCard(addCardRecord);

        if (response.isPresent()) {
            if (response.get().equals("response")) {
                System.out.println("Card created successfully!");
            }else {
                System.out.println(response.get());
            }
        }
    }

    private void getAccountOfBalance() {
        Optional<ArrayList<Card>> userCards = mainService.getUserCards();

        userCards.ifPresent(System.out::println);
    }

    public void transferMoney() {
        String senderCardNumber = Util.getText("\uD83D\uDCB3 Enter your sender card number");

        String receiverCardNumber = Util.getText("💳 Enter receiver card number");

        double cash = Util.getNumber("\"\uD83D\uDCB5 Enter amount to send:\"");

        // Вызываем сервис
        mainService.sendCash(senderCardNumber, receiverCardNumber, cash);
    }

}
