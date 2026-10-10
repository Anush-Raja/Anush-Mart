import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class ShoppingMartApp {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}

// 1. LOGIN FRAME
class LoginFrame extends JFrame {
    private JTextField txtUser;
    private JPasswordField txtPass;

    public LoginFrame() {
        setTitle("Shopping Mart - Login");
        setSize(350, 220);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new GridLayout(4, 2, 10, 10));

        add(new JLabel("  Username:"));
        txtUser = new JTextField();
        add(txtUser);

        add(new JLabel("  Password:"));
        txtPass = new JPasswordField();
        add(txtPass);

        JButton btnLogin = new JButton("Login");
        JButton btnRegister = new JButton("Register Page");

        add(btnLogin);
        add(btnRegister);

        btnLogin.addActionListener(e -> loginUser());
        btnRegister.addActionListener(e -> {
            new RegisterFrame().setVisible(true);
            dispose();
        });
    }

    private void loginUser() {
        String user = txtUser.getText().trim();
        String pass = new String(txtPass.getPassword()).trim();

        if (user.isEmpty() || pass.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Fields cannot be empty!");
            return;
        }

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement("SELECT role FROM users WHERE username=? AND password=?")) {
            
            pst.setString(1, user);
            pst.setString(2, pass);
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                String role = rs.getString("role");
                JOptionPane.showMessageDialog(this, "Login Successful! Role: " + role);
                
                if ("admin".equalsIgnoreCase(role)) {
                    new AdminDashboard().setVisible(true);
                } else {
                    new UserDashboard(user).setVisible(true);
                }
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, "Invalid Username or Password!");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}

// 2. REGISTER FRAME
class RegisterFrame extends JFrame {
    private JTextField txtUser;
    private JPasswordField txtPass;

    public RegisterFrame() {
        setTitle("Shopping Mart - Registration");
        setSize(350, 200);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new GridLayout(3, 2, 10, 10));

        add(new JLabel("  New Username:"));
        txtUser = new JTextField();
        add(txtUser);

        add(new JLabel("  New Password:"));
        txtPass = new JPasswordField();
        add(txtPass);

        JButton btnRegister = new JButton("Register");
        JButton btnBack = new JButton("Back to Login");

        add(btnRegister);
        add(btnBack);

        btnRegister.addActionListener(e -> registerUser());
        btnBack.addActionListener(e -> {
            new LoginFrame().setVisible(true);
            dispose();
        });
    }

    private void registerUser() {
        String user = txtUser.getText().trim();
        String pass = new String(txtPass.getPassword()).trim();

        if (user.isEmpty() || pass.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Fields cannot be empty!");
            return;
        }

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement("INSERT INTO users(username, password, role) VALUES(?, ?, 'user')")) {
            
            pst.setString(1, user);
            pst.setString(2, pass);
            pst.executeUpdate();
            
            JOptionPane.showMessageDialog(this, "Registration Successful! Please Login.");
            new LoginFrame().setVisible(true);
            dispose();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Username already exists!");
        }
    }
}

// 3. ADMIN DASHBOARD
class AdminDashboard extends JFrame {
    private JTable table;
    private DefaultTableModel model;
    private JTextField txtName, txtPrice, txtStock;

    public AdminDashboard() {
        setTitle("Admin Panel - Manage Inventory");
        setSize(600, 450);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        model = new DefaultTableModel(new String[]{"ID", "Name", "Price", "Stock"}, 0);
        table = new JTable(model);
        loadProducts();

        JPanel inputPanel = new JPanel(new GridLayout(4, 2, 5, 5));
        inputPanel.add(new JLabel("Product Name:"));
        txtName = new JTextField();
        inputPanel.add(txtName);

        inputPanel.add(new JLabel("Price:"));
        txtPrice = new JTextField();
        inputPanel.add(txtPrice);

        inputPanel.add(new JLabel("Stock:"));
        txtStock = new JTextField();
        inputPanel.add(txtStock);

        JButton btnAdd = new JButton("Add Product");
        JButton btnLogout = new JButton("Logout");
        inputPanel.add(btnAdd);
        inputPanel.add(btnLogout);

        add(new JScrollPane(table), BorderLayout.CENTER);
        add(inputPanel, BorderLayout.SOUTH);

        btnAdd.addActionListener(e -> addProduct());
        btnLogout.addActionListener(e -> {
            new LoginFrame().setVisible(true);
            dispose();
        });
    }

    private void loadProducts() {
        model.setRowCount(0);
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM products")) {
            while (rs.next()) {
                model.addRow(new Object[]{rs.getInt("id"), rs.getString("name"), rs.getDouble("price"), rs.getInt("stock")});
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void addProduct() {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement("INSERT INTO products(name, price, stock) VALUES(?, ?, ?)")) {
            pst.setString(1, txtName.getText().trim());
            pst.setDouble(2, Double.parseDouble(txtPrice.getText().trim()));
            pst.setInt(3, Integer.parseInt(txtStock.getText().trim()));
            pst.executeUpdate();
            
            JOptionPane.showMessageDialog(this, "Product Added Successfully!");
            loadProducts();
            txtName.setText(""); txtPrice.setText(""); txtStock.setText("");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error adding product!");
        }
    }
}

// 4. USER DASHBOARD
class UserDashboard extends JFrame {
    private String currentUser;
    private JTable table;
    private DefaultTableModel model;
    private JTextField txtQty;

    public UserDashboard(String username) {
        this.currentUser = username;
        setTitle("User Shopping Mart - Welcome " + username);
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        model = new DefaultTableModel(new String[]{"ID", "Product Name", "Price", "Stock"}, 0);
        table = new JTable(model);
        loadProducts();

        JPanel buyPanel = new JPanel();
        buyPanel.add(new JLabel("Quantity:"));
        txtQty = new JTextField("1", 5);
        buyPanel.add(txtQty);

        JButton btnBuy = new JButton("Buy Selected Product");
        JButton btnLogout = new JButton("Logout");
        buyPanel.add(btnBuy);
        buyPanel.add(btnLogout);

        add(new JScrollPane(table), BorderLayout.CENTER);
        add(buyPanel, BorderLayout.SOUTH);

        btnBuy.addActionListener(e -> buyProduct());
        btnLogout.addActionListener(e -> {
            new LoginFrame().setVisible(true);
            dispose();
        });
    }

    private void loadProducts() {
        model.setRowCount(0);
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM products")) {
            while (rs.next()) {
                model.addRow(new Object[]{rs.getInt("id"), rs.getString("name"), rs.getDouble("price"), rs.getInt("stock")});
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void buyProduct() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a product from the table!");
            return;
        }

        int productId = (int) model.getValueAt(selectedRow, 0);
        String pName = (String) model.getValueAt(selectedRow, 1);
        double price = (double) model.getValueAt(selectedRow, 2);
        int currentStock = (int) model.getValueAt(selectedRow, 3);
        int qty = Integer.parseInt(txtQty.getText().trim());

        if (qty > currentStock) {
            JOptionPane.showMessageDialog(this, "Insufficient stock!");
            return;
        }

        double total = price * qty;

        try (Connection conn = DBConnection.getConnection()) {
            PreparedStatement pstOrder = conn.prepareStatement("INSERT INTO orders(username, product_name, quantity, total_price) VALUES(?, ?, ?, ?)");
            pstOrder.setString(1, currentUser);
            pstOrder.setString(2, pName);
            pstOrder.setInt(3, qty);
            pstOrder.setDouble(4, total);
            pstOrder.executeUpdate();

            PreparedStatement pstStock = conn.prepareStatement("UPDATE products SET stock = stock - ? WHERE id = ?");
            pstStock.setInt(1, qty);
            pstStock.setInt(2, productId);
            pstStock.executeUpdate();

            JOptionPane.showMessageDialog(this, "Purchase Successful! Total Bill: Rs. " + total);
            loadProducts();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}

// 5. DATABASE CONNECTION CLASS
class DBConnection {
    private static final String URL = "jdbc:mysql://localhost:3306/shopping_mart";
    private static final String USER = "root";
    private static final String PASSWORD = "";

    public static Connection getConnection() {
        Connection conn = null;
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            conn = java.sql.DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return conn;
    }
}