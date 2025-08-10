package service;

import dto.AddCardRecord;
import dto.LoginRecord;
import dto.RegisterRecord;
import entity.Card;
import entity.User;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

public class MainService {
    ArrayList<Card> cards = new ArrayList<>();
    public User currentUser;
    private final ReentrantLock fileLock = new ReentrantLock();

    ExecutorService executorService = Executors.newFixedThreadPool(12);

    public Optional<User> getCurrentUser() {
            return Optional.ofNullable(currentUser);
        }

    public void setCurrentUser(User currentUser) {
        this.currentUser = currentUser;
    }

    private Optional<String> checkUsername(String userName) throws InterruptedException {
        if (userName.length() < 5) {
            return Optional.of("❌ Username is not being less than 5");
        }

        return Optional.empty();
    };

    private Optional<String> checkPassword(String password) throws InterruptedException {
        if (password.length() < 8) {
            return Optional.of("❌ Password mustn't be less than 8");
        }

        return Optional.empty();
    }

    private Optional<String> checkPhoneNumber(String phoneNumber) throws InterruptedException {
        if (phoneNumber.length() != 9  ) {
            return Optional.of("❌ Phone number must be equal to 9");
        }

        for (int i = 0; i < phoneNumber.length(); i++) {
            if (!Character.isDigit(phoneNumber.charAt(i))) {
                return Optional.of("❌ Enter only digits to this field!");
            }
        }

        return Optional.empty();
    }

    private Optional<String> checkExistedUser(String phoneNumber) throws InterruptedException, IOException {
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader("io/UserData.txt"))) {
            String line = bufferedReader.readLine();

            while (line != null) {
                if (line.contains(phoneNumber)) {
                    return Optional.of("❌ This user already registered!");
                }

                line = bufferedReader.readLine();

            }
        }

        return Optional.empty();
    }

    public Optional<String> registerUser(RegisterRecord userRecord) {
        List<Callable<Optional<String>>> tasks = List.of(
                () -> checkPassword(userRecord.password()),
                () -> checkUsername(userRecord.userName()),
                () -> checkPhoneNumber(userRecord.phoneNumber()),
                () -> checkExistedUser(userRecord.phoneNumber())
        );

        try {
            List<Future<Optional<String>>> futures = executorService.invokeAll(tasks);

            for (Future<Optional<String>> future : futures) {
                Optional<String> error = future.get();

                if (error.isPresent()) {
                    return error;
                }
            }

            User user = new User(userRecord.userName(), userRecord.password(), userRecord.phoneNumber());

            try (FileWriter fileWriter = new FileWriter("io/UserData.txt", true)) {
                BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);

                bufferedWriter.write(
                        user.getUserName() + "#" +
                                user.getPassword() + "#" +
                                user.getPhoneNumber()
                );

                bufferedWriter.newLine();
            }

            setCurrentUser(user);

            return Optional.of("response");
        }catch (InterruptedException | ExecutionException | IOException e) {
            String error = "❌ " + e.getMessage();
            executorService.shutdown();
            return Optional.of(error);
        }
    }

    public Optional<String> loginUser(LoginRecord loginRecord) {
        try {
            Future<Optional<String>> checkPhoneF = executorService.submit(() -> checkPhoneNumber(loginRecord.phoneNumber()));
            Future<Optional<String>> checkPasswordF = executorService.submit(() -> checkPassword(loginRecord.password()));

            Optional<String> phoneError = checkPhoneF.get();
            if (phoneError.isPresent()) {
                return phoneError;
            }

            Optional<String> passwordError = checkPasswordF.get();
            if (passwordError.isPresent()) {
                return passwordError;
            }

            ArrayList<Card> cards = new ArrayList<>();

            try (BufferedReader br = new BufferedReader(new FileReader("io/UserData.txt"))) {
                String line;
                while ((line = br.readLine()) != null) {
                    String[] data = line.trim().split("#");
                    if (data.length < 3) continue;

                    String phoneNumber = data[2];
                    String password = data[1];

                    if (phoneNumber.equals(loginRecord.phoneNumber()) && password.equals(loginRecord.password())) {
                        String userName = data[0];

                        for (int i = 3; i + 2 < data.length; i += 3) {
                            String cardNumber = data[i];
                            String expireDate = data[i + 1];
                            double amount = Double.parseDouble(data[i + 2]);
                            cards.add(new Card(cardNumber, expireDate, amount));
                        }

                        User user = new User(userName, password, phoneNumber, cards);
                        setCurrentUser(user);

                        return Optional.of(user.getUserName());
                    }
                }
            }

            if (currentUser != null) {
                currentUser.setCards(cards);
                return Optional.of("response");
            } else {
                return Optional.empty();
            }

        }catch (InterruptedException | ExecutionException | IOException e) {
            executorService.shutdown();
            return Optional.of(e.getMessage());
        }
    }

    public void switchOff() {
        executorService.shutdown();

        try {
            if (executorService.awaitTermination(1, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public Optional<ArrayList<Card>> getUserCards() {
        return Optional.of(currentUser.getCards());
    }

    public Optional<String> addCreditCard(AddCardRecord addCardRecord) {
        try {
            Future<Optional<String>> checkCardF = executorService.submit(() -> {
                if (addCardRecord.cardNumber().length() != 16) {
                    return Optional.of("❌ Card number must be 16 digits");
                }

                return Optional.empty();
            });

            Optional<String> cardLengthError = checkCardF.get();
            if (cardLengthError.isPresent()) {
                return cardLengthError;
            }

            Card newCard = new Card(addCardRecord.cardNumber(), addCardRecord.expireDate(), 0);

            Future<Optional<String>> existingCardErrorF = executorService.submit(() -> {
                try (BufferedReader bufferedReader = new BufferedReader(new FileReader("io/UserData.txt"))) {
                    String line;

                    while ((line = bufferedReader.readLine()) != null) {
                        if (line.contains(addCardRecord.cardNumber())) {
                            return Optional.of("❌ This card has already been added");
                        }
                    }
                }

                return Optional.empty();
            });

            Optional<String> existingCardError = existingCardErrorF.get();
            if (existingCardError.isPresent()) {
                return existingCardError;
            }

            if (currentUser.getCards() == null) {
                currentUser.setCards(new ArrayList<>());
            }

            currentUser.getCards().add(newCard);

            // Запись в файл
            try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter("io/UserData.txt", true))) {
                bufferedWriter.write(
                        currentUser.getUserName() + "#" +
                                currentUser.getPassword() + "#" +
                                currentUser.getPhoneNumber() + "#" +
                                newCard.getCardNumber() + "#" +
                                newCard.getExpireDate() + "#" +
                                newCard.getAmount()
                );

                bufferedWriter.newLine();
            }

            return Optional.of("✅ Card successfully added");

        } catch (InterruptedException | ExecutionException | IOException e) {
            System.out.println(e.getMessage());
            executorService.shutdown();
            return Optional.of("❌ " + e.getMessage());
        }
    }

    public void sendCash(String senderCardNumber, String receiverCardNumber, double cash) {
        fileLock.lock();

        try {
            boolean senderHasEnough = false;

            List<String> lines = new ArrayList<>();

            try (BufferedReader reader = new BufferedReader(new FileReader("io/UserData.txt"))) {
                String line;

                while ((line = reader.readLine()) != null) {
                    String[] data = line.split("#");

                    if (data.length >= 6 && data[3].equals(senderCardNumber)) {
                        double amount = Double.parseDouble(data[5]);
                        if (amount >= cash) {
                            amount -= cash;
                            data[5] = String.valueOf(amount);
                            senderHasEnough = true;

                            line = String.join("#", data);
                        } else {
                            System.out.println("❌ Недостаточно средств на карте отправителя!");
                            return;
                        }
                    }
                    lines.add(line);
                }
            }

            if (!senderHasEnough) {
                System.out.println("❌ Карта отправителя не найдена!");
                return;
            }

            boolean receiverFound = false;
            List<String> updatedLines = new ArrayList<>();

            for (String l : lines) {
                String[] data = l.split("#");

                if (data.length >= 6 && data[3].equals(receiverCardNumber)) {
                    double amount = Double.parseDouble(data[5]);
                    amount += cash;
                    data[5] = String.valueOf(amount);
                    l = String.join("#", data);
                    receiverFound = true;
                }
                updatedLines.add(l);
            }

            if (!receiverFound) {
                System.out.println("❌ Карта получателя не найдена!");
                return;
            }

            try (BufferedWriter writer = new BufferedWriter(new FileWriter("io/UserData.txt"))) {
                for (String updatedLine : updatedLines) {
                    writer.write(updatedLine);
                    writer.newLine();
                }
            }

            reloadCurrentUser();

            System.out.println("✅ Перевод выполнен успешно!");

        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("❌ Ошибка при работе с файлом.");
        } finally {
            fileLock.unlock();
        }
    }

    public void reloadCurrentUser() throws IOException {
        String phone = currentUser.getPhoneNumber();
        try (BufferedReader br = new BufferedReader(new FileReader("io/UserData.txt"))) {
            String line;

            while ((line = br.readLine()) != null) {
                String[] data = line.split("#");
                if (data.length >= 3 && data[2].equals(phone)) {
                    // Создаем список карт из данных
                    ArrayList<Card> cards = new ArrayList<>();
                    for (int i = 3; i + 2 < data.length; i += 3) {
                        String cardNum = data[i];
                        String expire = data[i + 1];
                        double amount = Double.parseDouble(data[i + 2]);
                        cards.add(new Card(cardNum, expire, amount));
                    }
                    currentUser.setCards(cards);
                    break;
                }
            }
        }
    }

    // База данных

    {
        Card card = new Card("000000000000", "00/00", 1000);
        cards.add(card);

        User admin = new User("admin", "12345678", "997494262", cards);

        boolean adminExists = false;

        try {
            try (FileReader fileReader = new FileReader("io/UserData.txt")) {
                BufferedReader bufferedReader = new BufferedReader(fileReader);
                String line = bufferedReader.readLine();

                while (line != null) {
                    String[] data = line.split("#");

                    if (data[0].equals(admin.getUserName()) && data[2].equals(admin.getPhoneNumber()) ) {
                        adminExists = true;
                        break;
                    }

                    line = bufferedReader.readLine();
                }

            }

            if (!adminExists) {
                try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter("io/UserData.txt", true))) {
                    bufferedWriter.write(admin.getUserName() + "#" + admin.getPassword() +
                            "#" + admin.getPhoneNumber() + "#" + card.getCardNumber() + "#" +
                            card.getExpireDate() + "#" + card.getAmount());

                    bufferedWriter.newLine();
                }
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}