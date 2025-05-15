package Amogus.group.GameOn;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "Users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_seq")
    @SequenceGenerator(name = "user_seq", sequenceName = "users_seq", allocationSize = 1)
    @Column(name = "userid")
    private Long userId;

    @Column(name = "username", unique = true, nullable = false)
    private String username;

    @Column(name = "email", unique = true, nullable = false)
    private String email;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "walletbalance", precision = 10, scale = 2, nullable = false)
    private BigDecimal walletBalance = BigDecimal.ZERO;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "joindate", nullable = false, updatable = false)
    private Date joinDate;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Library> libraryEntries;

    // Constructors
    public User() {}

    public User(String username, String email, String password) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.joinDate = new Date(); // Automatically sets the join date
    }

    // Getters and Setters
    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public BigDecimal getWalletBalance() {
        return walletBalance;
    }

    public void setWalletBalance(BigDecimal walletBalance) {
        this.walletBalance = walletBalance;
    }

    // Overloaded method to set wallet balance using a double value
    public void setWalletBalance(Double walletBalance) {
        if (walletBalance != null) {
            this.walletBalance = BigDecimal.valueOf(walletBalance);
        } else {
            this.walletBalance = BigDecimal.ZERO;
        }
    }

    public Date getJoinDate() {
        return joinDate;
    }

    public void setJoinDate(Date joinDate) {
        this.joinDate = joinDate;
    }

    public List<Library> getLibraryEntries() {
        return libraryEntries;
    }

    public void setLibraryEntries(List<Library> libraryEntries) {
        this.libraryEntries = libraryEntries;
    }

    /**
     * Subtracts an amount from the user's wallet balance.
     * 
     * @param amount the amount to subtract
     */
    public void subtractFromWallet(BigDecimal amount) {
        if (walletBalance.compareTo(amount) >= 0) {
            this.walletBalance = this.walletBalance.subtract(amount);
        } else {
            throw new IllegalArgumentException("Insufficient balance");
        }
    }
}
