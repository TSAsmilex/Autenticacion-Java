import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.security.auth.login.LoginException;

public class UserDB {
    private static final String DB_FILEPATH = "./db/user.csv";
    private HashSet<User> users = new HashSet<>();


    public UserDB() {
        try {
            loadDB();
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }


    public boolean loadDB() throws FileNotFoundException, IOException {
        File csvFile = new File(UserDB.DB_FILEPATH);

        if (!csvFile.exists()) {
            return false;
        }

        try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
            String line;

            while ((line = br.readLine()) != null) {
                String[] taskString = line.split(";");

                User user = new User(
                    taskString[0],
                    taskString[1],
                    taskString[2]
                );

                this.users.add(user);
            }
        }

        return true;
    }


    public boolean writeDB() throws IOException {
        String output = this.users.stream()
            .map(user -> new String (
                user.getName() + ";" + user.getDni() + ";" + user.getHashedPassword()
            )).collect(Collectors.joining("\n"));

        File csvFile = new File(UserDB.DB_FILEPATH);

        if (!csvFile.exists()) {
            csvFile.getParentFile().mkdirs();
            csvFile.createNewFile();
        }

        try (PrintWriter pw = new PrintWriter(csvFile)) {
            pw.println(output);
        }

        return csvFile.exists();
    }


    public boolean addUser(User user) {
        if (exists(user.getDni())) {
            return false;
        }

        return this.users.add(user);
    }


    public boolean exists (String dni) {
        return this.users.stream()
            .filter(u -> u.getDni().equals(dni))
            .findFirst()
            .isPresent();
    }


    public User login (String dni, String password) throws LoginException {
        Optional<User> user = this.users.stream()
            .filter(u -> u.getDni().equals(dni) && u.getHashedPassword().equals(password))
            .findFirst();

        if (!user.isPresent()) {
            throw new LoginException("Credenciales inválidos");
        }

        return user.get();
    }
}
