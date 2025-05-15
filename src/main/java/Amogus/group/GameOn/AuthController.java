package Amogus.group.GameOn;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ModelAttribute;
import jakarta.servlet.http.HttpSession;

@Controller
public class AuthController {

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private UserRepository userRepository;

    // Show the wallet balance update form for admin
    @GetMapping("/admin/set-wallet")
    public String showSetWalletForm(HttpSession session, Model model) {
        // Check if user is admin
        Boolean isAdmin = (Boolean) session.getAttribute("isAdmin");
        if (isAdmin == null || !isAdmin) {
            return "redirect:/login";  // Only admins allowed
        }

        List<User> users = userRepository.findAll();
        model.addAttribute("users", users);
        return "admin-wallet"; // The Thymeleaf page for setting wallet balance
    }
    
 // Handle wallet balance update
    @PostMapping("/admin/set-wallet")
    public String setWalletBalance(@RequestParam Long userId,
                                   @RequestParam Double walletBalance,
                                   HttpSession session,
                                   Model model) {
        // Check if user is admin
        Boolean isAdmin = (Boolean) session.getAttribute("isAdmin");
        if (isAdmin == null || !isAdmin) {
            return "redirect:/login";  // Only admins allowed
        }

        Optional<User> optionalUser = userRepository.findById(userId);
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            user.setWalletBalance(walletBalance);
            userRepository.save(user);
            model.addAttribute("successMessage", "Wallet balance updated successfully.");
        } else {
            model.addAttribute("errorMessage", "User not found.");
        }

        // Reload the users list to repopulate the form
        List<User> users = userRepository.findAll();
        model.addAttribute("users", users);

        return "admin-wallet";
    }
    
    // Login for both Admin and User
    @PostMapping("/login")
    public String login(@RequestParam String email, @RequestParam String password, HttpSession session, Model model) {
        // Check if the login is for an Admin
        Admin admin = adminRepository.findByEmailAndPassword(email, password);
        if (admin != null) {
            session.setAttribute("isAdmin", true);
            session.setAttribute("isLoggedIn", true);
            session.setAttribute("username", admin.getEmail());
            return "redirect:/games/list";
        }

        // Check if the login is for a user
        User user = userRepository.findByEmail(email).orElse(null);
        if (user != null && password.equals(user.getPassword())) {
            session.setAttribute("isLoggedIn", true);
            session.setAttribute("username", user.getUsername());
            session.setAttribute("userId", user.getUserId());
            session.setAttribute("walletBalance", user.getWalletBalance());
            return "redirect:/games/list";
        }

        // If login fails
        model.addAttribute("errorMessage", "Invalid email or password. Please try again.");
        return "login";
    }

    // Logout for Admin and Users
    @PostMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

    // Login page
    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    // SignUp page
    @GetMapping("/signup")
    public String signupPage() {
        return "signup";
    }

    // SignUp handling
    @PostMapping("/signup")
    public String signup(@ModelAttribute User user, Model model) {
        // Check if the username already exists
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            model.addAttribute("errorMessage", "Username already exists. Please choose a different one.");
            return "signup";  // Return to the signup page with the error message
        }

        // Check if the email already exists
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            model.addAttribute("errorMessage", "Email already exists. Please use a different one.");
            return "signup";  // Return to the signup page with the error message
        }

        // Save the new user if both username and email are unique
        userRepository.save(user);
        model.addAttribute("successMessage", "Account created successfully. You can now log in.");
        return "login";  // Go to login page after successful signup
    }

}