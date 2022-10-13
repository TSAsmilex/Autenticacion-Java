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
    BOOK(1),
    CANCEL(2),
    LIST(3),
    REGISTER_USER(4),
    LOGIN_USER(5),
    EXIT(6);

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
        System.out.println("   1) Reservar entradas");
        System.out.println("   2) Anular una reserva");
        System.out.println("   3) Listar una reserva");
        System.out.println("   4) Registrarse");
        System.out.println("   5) Login");
        System.out.println("   6) Salir");
    }
}


public class App {
    static Baptisterio baptisterio = new Baptisterio();
    static UserDB userDB           = new UserDB();
    static User user               = null;

    public static void main(String[] args) throws Exception {
        var       option = MenuOptions.NOOP;
        Scanner   scan   = new Scanner(System.in);
        Exception error  = null;

        do {
            if (option != MenuOptions.LIST) {
                clearScreen();
            }

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
                case BOOK -> {
                    try {
                        book(scan);
                    }
                    catch (DateTimeException e) {
                        error = e;
                    }
                    catch (LoginException e) {
                        error = e;
                    }
                }
                case CANCEL -> {
                    try {
                        cancel(scan);
                    }
                    catch (LoginException e) {
                        error = e;
                    }
                }
                case LIST            -> baptisterio.printReservations();
                case REGISTER_USER   -> registerUser(scan);
                case LOGIN_USER      -> {
                    try {
                        login(scan);
                    }
                    catch (LoginException e) {
                        error = e;
                    }
                }
                case EXIT            -> System.out.println("¡Hasta luego!");
                case NOOP            -> {}
            }
        } while (option != MenuOptions.EXIT);

        scan.close();

        if (!userDB.writeDB()) {
            System.err.println("Ha habido errores escribiendo la base de datos");
        }
    }


    private static LocalDateWrapper readDate(Scanner scan) throws DateTimeException{
        System.out.println("¿Qué día quieres comprobar? (YYYY-MM-DD)");
        System.out.print("> ");

        String dateString = scan.nextLine();
        LocalDate date;

        try {
            date = LocalDate.parse(dateString);
        }
        catch (DateTimeParseException e) {
            throw new DateTimeException("La fecha introducida es inválida");
        }

        return new LocalDateWrapper(date);
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

    private static boolean registerUser(Scanner scan){
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
            throw new InputMismatchException("El usuario ya existe");
        }

        user = possibleUser;

        return addedCorrectly;
    }


    private static void book (Scanner scan)
        throws Exception, DateTimeException, LoginException
    {
        if (user == null) {
            throw new LoginException("Debes iniciar sesión para reservar las entradas.");
        }

        LocalDateWrapper date = null;

        try {
            date = readDate(scan);
        }
        catch (DateTimeException e) {
            throw e;
        }

        if (date.isBefore(LocalDate.now())) {
            throw new DateTimeException("No puedes reservar entradas para días pasados");
        }
        if (!baptisterio.avaliable(date)) {
            throw new Exception("La fecha introducida no está disponible");
        }

        int round     = 0;
        int remaining = baptisterio.PRICE;
        ArrayList<CoinEuro> coins = new ArrayList<CoinEuro>();


        do {
            clearScreen();
            System.out.println("Debes abonar " + baptisterio.PRICE + " céntimos.");
            System.out.println("Introduce las monedas en el sistema. Puedes separarlas por comas o introducirlas poco a poco");
            System.out.print(remaining + " céntimos restantes. \n> ");

            String coinsString = scan.nextLine();
            var coinsRound = CoinEuro.fromString(coinsString);
            coins.addAll(coinsRound);

            round      = CoinEuro.sum(coinsRound);
            remaining -= round;

        } while (round <= remaining);

        if (remaining < 0) {
            System.out.println("\nTe has pasado " + Math.abs(remaining) + " céntimos.\n(Te imaginas que te he dado el cambio porque esto es un programa cutre).");
            coins.remove(CoinEuro.fromValue(Math.abs(remaining)));
        }

        baptisterio.pay(user, date, coins);
        System.out.println("Su pago se ha procesado correctamente");

        Thread.sleep(2000);
    }


    private static void cancel (Scanner scan) throws InterruptedException, LoginException {
        if (user == null) {
            throw new LoginException("Debes iniciar sesión para reservar las entradas.");
        }

        LocalDateWrapper date = null;

        try {
            date = readDate(scan);
        }
        catch (DateTimeException e) {
            throw e;
        }

        if (baptisterio.cancel(user, date)) {
            System.out.println("Su cita se ha cancelado con éxito!");
        }
        else {
            System.out.println("No se ha podido cancelar su reserva.");
        }

        Thread.sleep(2000);
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
