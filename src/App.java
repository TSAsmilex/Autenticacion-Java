import java.math.BigInteger;
import java.security.MessageDigest;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.InputMismatchException;
import java.util.Scanner;

import javax.security.auth.login.LoginException;


enum MenuOptions {
    NOOP(0),
    REGISTER_USER(1),
    LOGIN_USER(2),
    EXIT(3);

    private final int value;

    private MenuOptions(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static MenuOptions fromValue(int value) {
        for (MenuOptions option : MenuOptions.values()) {
            if (option.getValue() == value) {
                return option;
            }
        }

        throw new InputMismatchException("Invalid option");
    }

    public static void printMenu() {
        System.out.println("   1) Registrarse");
        System.out.println("   2) Login");
        System.out.println("   3) Salir");
    }
}


public class App {
    static UserDB userDB = new UserDB();
    static User user     = null;

    public static void main(String[] args) throws Exception {
        var       option = MenuOptions.NOOP;
        Scanner   scan   = new Scanner(System.in);
        Exception error  = null;

        do {
            clearScreen();

            if (error != null) {
                System.err.println("Se ha producido un error. Motivo: " + error.getMessage());
            }

            error  = null;
            option = MenuOptions.NOOP;

            if (user != null) {
                System.out.println("Hola, " + user.getName() + ". ¿Qué quieres hacer?");
            }
            else {
                System.out.println("\n¡Hola! ¿Qué quieres hacer?");
            }
            MenuOptions.printMenu();

            error = null;

            try {
                System.out.print("> ");
                option = MenuOptions.fromValue(scan.nextInt());
                scan.nextLine();
            }
            catch (InputMismatchException e) {
                error = new InputMismatchException("Introduce un comando válido.");
                scan.nextLine();
            }

            switch (option) {
                case REGISTER_USER -> {
                    try {
                        registerUser(scan);
                    }
                    catch (LoginException e) {
                        error = e;
                    }
                }
                case LOGIN_USER -> {
                    try {
                        login(scan);
                    }
                    catch (LoginException e) {
                        error = e;
                    }
                }
                case EXIT -> System.out.println("¡Hasta luego!");
                case NOOP -> {}
            }
        } while (option != MenuOptions.EXIT);

        scan.close();

        if (!userDB.writeDB()) {
            System.err.println("Ha habido errores escribiendo la base de datos");
        }
    }


    private static boolean login (Scanner scan) throws LoginException {
        System.out.println("¿Cuál es tu DNI?");
        System.out.print("> ");
        String dni = scan.nextLine();

        System.out.println("Escriba su contraseña");
        System.out.print("> ");
        String password = scan.nextLine();
        String hashedPassword = getSHA512(password);

        try {
            user = userDB.login(dni, hashedPassword);
        }
        catch (LoginException e) {
            throw new LoginException("El usuario o la contraseña son incorrectos");
        }

        return true;
    }


    private static boolean registerUser(Scanner scan) throws LoginException {
        System.out.println("Escriba un usuario");
        System.out.print("> ");
        String name = scan.nextLine();

        System.out.println("¿Cuál es tu DNI?");
        System.out.print("> ");
        String dni = scan.nextLine();

        System.out.println("Escriba una contraseña");
        System.out.print("> ");
        String password = scan.nextLine();
        String hashedPassword = getSHA512(password);

        User possibleUser = new User(name, dni, hashedPassword);

        boolean addedCorrectly = userDB.addUser(possibleUser);

        if (!addedCorrectly) {
            throw new LoginException("El usuario ya existe");
        }

        user = possibleUser;

        return addedCorrectly;
    }


    // ─────────────────────────────────────────────────────────────────────────────


    static void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }


    public static String getSHA512(String input){
        String toReturn = null;

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-512");
            digest.reset();
            digest.update(input.getBytes("utf8"));
            toReturn = String.format("%0128x", new BigInteger(1, digest.digest()));
        } catch (Exception e) {
            e.printStackTrace();
        }

        return toReturn;
    }
}
