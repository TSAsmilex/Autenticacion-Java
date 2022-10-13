public class User {
    // dni, name
    private String dni;
    private String name;
    private String hashedPassword;


    public User(String name, String dni, String hashedPassword){
        this.name = name;
        this.dni  = dni;
        this.hashedPassword=hashedPassword;
    }

    public String getDni() {
        return dni;
    }

    public String getName() {
        return name;
    }
    public String getHashedPassword() {
        return hashedPassword;
    }
    public void setDni(String dni) {
        this.dni = dni;
    }

    public void setName(String name) {
        this.name = name;
    }
    public void setHashedPassword(String hashedPassword) {
        this.hashedPassword = hashedPassword;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        User o = (User) obj;
        return o.getDni().equals(dni) && o.getName().equals(name);
    }

}

/*


public static void main(String[] argv) {

	String inputValue = "this is an example";

	// With the java libraries
	String sha256 = getSHA256(inputValue);

	System.out.println("The SHA-256 of \"" + inputValue + "\" is:");
	System.out.println(sha256);
 */