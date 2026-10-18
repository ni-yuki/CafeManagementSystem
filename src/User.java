public class User {
    private String userId;
    private String username;
    private String password;
    private String role; // Admin, Manager, Cashier

    public User(String userId, String username, String password, String role) {
        this.userId = userId;
        this.username = username;
        this.password = password;
        this.role = role;
    }

    public String getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getRole() { return role; }

    public String toFileString() {
        return userId + "," + username + "," + password + "," + role;
    }
}