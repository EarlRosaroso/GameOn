package Amogus.group.GameOn;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;

@Controller
public class GameController {

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private LibraryRepository libraryRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private UserRepository userRepository;

    // Handle game purchase (Creates Transaction, then adds game to Library)
    @PostMapping("/games/purchase/{id}")
    @Transactional
    public String purchaseGame(@PathVariable("id") int id, HttpSession session, RedirectAttributes redirectAttributes) {
        Object userIdObj = session.getAttribute("userId");

        if (userIdObj == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "You must be logged in to purchase a game.");
            return "redirect:/login";
        }

        Long userId;
        try {
            userId = Long.valueOf(userIdObj.toString());
        } catch (NumberFormatException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Invalid user ID.");
            return "redirect:/login";
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid user ID: " + userId));
        Game game = gameRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid game ID: " + id));

        // Check if the user already owns the game
        boolean alreadyOwned = libraryRepository.existsByUser_UserIdAndGame_Id(userId, (long) id);
        if (alreadyOwned) {
            redirectAttributes.addFlashAttribute("errorMessage", "You already own this game.");
            return "redirect:/games/list";
        }

        // Check wallet balance
        if (user.getWalletBalance().compareTo(game.getPrice()) < 0) {
            redirectAttributes.addFlashAttribute("errorMessage", "Insufficient wallet balance to purchase this game.");
            return "redirect:/games/list";
        }

        try {
            // Create transaction and update wallet
            Transaction transaction = new Transaction(user, game, game.getPrice());
            transactionRepository.save(transaction);

            user.setWalletBalance(user.getWalletBalance().subtract(game.getPrice()));
            userRepository.save(user);

            Library newEntry = new Library(user, game);
            libraryRepository.save(newEntry);

            redirectAttributes.addFlashAttribute("successMessage", "Game purchased successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Purchase failed: " + e.getMessage());
        }
        return "redirect:/games/list";
    }

    // Library page
    @GetMapping("/library")
    public String libraryPage(HttpSession session, Model model) {
        Long userId = (Long) session.getAttribute("userId");

        if (userId != null) {
            List<Library> libraryEntries = libraryRepository.findByUser_UserId(userId);
            model.addAttribute("library", libraryEntries);
        } else {
            return "redirect:/login";
        }

        return "library";
    }

    // List all games
    @GetMapping("/games/list")
    public String listGames(Model model, HttpSession session) {
        List<Game> games = gameRepository.findAll();
        model.addAttribute("games", games);

        // Fetch wallet balance for the logged-in user
        Long userId = (Long) session.getAttribute("userId");
        if (userId != null) {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid user ID: " + userId));
            model.addAttribute("walletBalance", user.getWalletBalance()); // Add wallet balance to the model
        }

        // Check if a user is logged in and whether they are an Admin
        Boolean isAdmin = (Boolean) session.getAttribute("isAdmin");
        model.addAttribute("isAdmin", isAdmin != null && isAdmin);

        return "games";
    }

    // Show the add game form (ADMIN only)
    @GetMapping("/games/add")
    public String showAddGameForm(HttpSession session, Model model) {
        if (Boolean.TRUE.equals(session.getAttribute("isAdmin"))) {
            model.addAttribute("game", new Game());
            return "add";
        } else {
            return "redirect:/login";
        }
    }

    // Handle the form submission (add game for ADMIN only)
    @PostMapping("/games/add")
    public String addGame(@ModelAttribute Game game, Model model, @RequestParam(required = false) boolean free, HttpSession session) {
        if (Boolean.TRUE.equals(session.getAttribute("isAdmin"))) {
            if (free) {
                game.setPrice(BigDecimal.ZERO);
            }
            gameRepository.save(game);
            model.addAttribute("successMessage", "Game added successfully!");
            return "redirect:/games/list";
        } else {
            return "redirect:/login";
        }
    }

    // Show the edit form for a specific game (ADMIN only)
    @GetMapping("/games/edit/{id}")
    public String showEditGameForm(@PathVariable("id") int id, HttpSession session, Model model) {
        if (Boolean.TRUE.equals(session.getAttribute("isAdmin"))) {
            Game game = gameRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid game ID: " + id));
            model.addAttribute("game", game);
            return "edit";
        } else {
            return "redirect:/login";
        }
    }

    // Handle the edit form submission (ADMIN ONLY)
    @PostMapping("/games/edit/{id}")
    public String updateGame(@PathVariable("id") int id, @Valid @ModelAttribute Game game, BindingResult bindingResult, HttpSession session, Model model) {
        if (Boolean.TRUE.equals(session.getAttribute("isAdmin"))) {
            if (bindingResult.hasErrors()) {
                model.addAttribute("game", game);
                return "edit";
            }
            Game existingGame = gameRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid game ID: " + id));

            existingGame.setName(game.getName());
            existingGame.setGenre(game.getGenre());
            existingGame.setDescription(game.getDescription());
            existingGame.setPrice(game.getPrice());
            existingGame.setImageUrl(game.getImageUrl());

            gameRepository.save(existingGame);
            model.addAttribute("successMessage", "Game updated successfully!");
            return "redirect:/games/list";
        } else {
            return "redirect:/login";
        }
    }

    // Handle the delete action (ADMIN only)
    @PostMapping("/games/delete/{id}")
    @Transactional
    public String deleteGame(@PathVariable("id") int id, HttpSession session, RedirectAttributes redirectAttributes) {
        if (!Boolean.TRUE.equals(session.getAttribute("isAdmin"))) {
            redirectAttributes.addFlashAttribute("errorMessage", "Unauthorized action.");
            return "redirect:/login";
        }

        try {
            Game game = gameRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid game ID: " + id));

            gameRepository.delete(game);
            redirectAttributes.addFlashAttribute("successMessage", "Game deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "An error occurred while deleting the game: " + e.getMessage());
        }

        return "redirect:/games/list";
    }
}