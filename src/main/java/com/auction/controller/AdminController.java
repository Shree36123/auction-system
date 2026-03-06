package com.auction.controller;

import com.auction.model.*;
import com.auction.service.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final PlayerService playerService;
    private final TeamService teamService;
    private final AuctionService auctionService;
    private final UserService userService;
    private final ExcelImportService excelImportService;

    public AdminController(PlayerService playerService, TeamService teamService,
                           AuctionService auctionService, UserService userService,
                           ExcelImportService excelImportService) {
        this.playerService = playerService;
        this.teamService = teamService;
        this.auctionService = auctionService;
        this.userService = userService;
        this.excelImportService = excelImportService;
    }

    // ========== DASHBOARD ==========
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("teams", teamService.getAllTeams());
        model.addAttribute("totalPlayers", playerService.getAllPlayers().size());
        model.addAttribute("availablePlayers", playerService.getAvailablePlayers().size());
        model.addAttribute("soldPlayers", playerService.getPlayersByStatus(PlayerStatus.SOLD).size());
        model.addAttribute("unsoldPlayers", playerService.getPlayersByStatus(PlayerStatus.UNSOLD).size());
        return "admin/dashboard";
    }

    // ========== PLAYER MANAGEMENT ==========
    @GetMapping("/players")
    public String listPlayers(Model model,
                              @RequestParam(required = false) PlayerCategory category,
                              @RequestParam(required = false) PlayerStatus status) {
        List<Player> players;
        if (category != null && status != null) {
            players = playerService.getAvailablePlayersByCategory(category);
        } else if (category != null) {
            players = playerService.getPlayersByCategory(category);
        } else if (status != null) {
            players = playerService.getPlayersByStatus(status);
        } else {
            players = playerService.getAllPlayers();
        }
        model.addAttribute("players", players);
        model.addAttribute("categories", PlayerCategory.values());
        model.addAttribute("statuses", PlayerStatus.values());
        model.addAttribute("selectedCategory", category);
        model.addAttribute("selectedStatus", status);
        return "admin/players";
    }

    @GetMapping("/players/add")
    public String showAddPlayerForm(Model model) {
        model.addAttribute("player", new Player());
        model.addAttribute("categories", PlayerCategory.values());
        return "admin/player-form";
    }

    @PostMapping("/players/add")
    public String addPlayer(@ModelAttribute Player player,
                            @RequestParam("image") MultipartFile image,
                            RedirectAttributes redirectAttributes) {
        try {
            playerService.registerPlayer(player, image);
            redirectAttributes.addFlashAttribute("successMessage", "Player registered successfully!");
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error uploading image: " + e.getMessage());
        }
        return "redirect:/admin/players";
    }

    @GetMapping("/players/edit/{id}")
    public String showEditPlayerForm(@PathVariable Long id, Model model) {
        playerService.getPlayerById(id).ifPresent(player -> {
            model.addAttribute("player", player);
            model.addAttribute("categories", PlayerCategory.values());
        });
        return "admin/player-form";
    }

    @PostMapping("/players/edit/{id}")
    public String updatePlayer(@PathVariable Long id,
                               @ModelAttribute Player player,
                               @RequestParam(value = "image", required = false) MultipartFile image,
                               RedirectAttributes redirectAttributes) {
        try {
            Player existing = playerService.getPlayerById(id)
                    .orElseThrow(() -> new RuntimeException("Player not found"));
            existing.setFullName(player.getFullName());
            existing.setPhoneNumber(player.getPhoneNumber());
            existing.setJerseySize(player.getJerseySize());
            existing.setAchievements(player.getAchievements());
            existing.setBasePrice(player.getBasePrice());

            if (image != null && !image.isEmpty()) {
                // Re-upload logic handled in service
                playerService.registerPlayer(existing, image);
            } else {
                playerService.updatePlayer(existing);
            }
            redirectAttributes.addFlashAttribute("successMessage", "Player updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error: " + e.getMessage());
        }
        return "redirect:/admin/players";
    }

    @PostMapping("/players/delete/{id}")
    public String deletePlayer(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        playerService.deletePlayer(id);
        redirectAttributes.addFlashAttribute("successMessage", "Player deleted successfully!");
        return "redirect:/admin/players";
    }

    @PostMapping("/players/import-excel")
    public String importPlayersFromExcel(@RequestParam("excelFile") MultipartFile file,
                                         RedirectAttributes redirectAttributes) {
        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please select an Excel file to upload.");
            return "redirect:/admin/players";
        }

        String filename = file.getOriginalFilename();
        if (filename == null || (!filename.endsWith(".xlsx") && !filename.endsWith(".xls"))) {
            redirectAttributes.addFlashAttribute("errorMessage", "Only Excel files (.xlsx) are supported.");
            return "redirect:/admin/players";
        }

        ExcelImportService.ImportResult result = excelImportService.importPlayersFromExcel(file);

        if (result.importedCount() > 0) {
            redirectAttributes.addFlashAttribute("successMessage",
                    result.importedCount() + " player(s) imported successfully!");
        }
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Some rows had errors: " + String.join("; ", result.errors()));
        }
        return "redirect:/admin/players";
    }

    // ========== TEAM MANAGEMENT ==========
    @GetMapping("/teams")
    public String listTeams(Model model) {
        model.addAttribute("teams", teamService.getAllTeamsWithPlayers());
        return "admin/teams";
    }

    @GetMapping("/teams/add")
    public String showAddTeamForm(Model model) {
        model.addAttribute("users", userService.getUsersByRole(UserRole.TEAM_OWNER));
        return "admin/team-form";
    }

    @PostMapping("/teams/add")
    public String addTeam(@RequestParam String name,
                          @RequestParam double budget,
                          @RequestParam List<Long> ownerIds,
                          @RequestParam(value = "logoFile", required = false) MultipartFile logoFile,
                          RedirectAttributes redirectAttributes) {
        try {
            String logoPath = teamService.saveTeamLogo(logoFile);
            teamService.createTeamByOwnerIds(name, budget, ownerIds,
                    logoPath != null ? logoPath : "default-logo.png");
            redirectAttributes.addFlashAttribute("successMessage", "Team created successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error: " + e.getMessage());
        }
        return "redirect:/admin/teams";
    }

    @GetMapping("/teams/{id}")
    public String viewTeam(@PathVariable Long id, Model model) {
        teamService.getTeamById(id).ifPresent(team -> {
            model.addAttribute("team", team);
            model.addAttribute("players", playerService.getPlayersByTeam(id));
            model.addAttribute("categories", PlayerCategory.values());
        });
        return "admin/team-detail";
    }

    @GetMapping("/teams/edit/{id}")
    public String showEditTeamForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        Optional<com.auction.model.Team> teamOpt = teamService.getTeamById(id);
        if (teamOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Team not found.");
            return "redirect:/admin/teams";
        }
        model.addAttribute("team", teamOpt.get());
        model.addAttribute("users", userService.getUsersByRole(UserRole.TEAM_OWNER));
        return "admin/team-form";
    }

    @PostMapping("/teams/edit/{id}")
    public String updateTeam(@PathVariable Long id,
                             @RequestParam String name,
                             @RequestParam double budget,
                             @RequestParam List<Long> ownerIds,
                             @RequestParam(value = "logoFile", required = false) MultipartFile logoFile,
                             RedirectAttributes redirectAttributes) {
        try {
            com.auction.model.Team team = teamService.getTeamById(id)
                    .orElseThrow(() -> new RuntimeException("Team not found"));
            team.setName(name);
            // Adjust remaining budget by the difference
            double budgetDiff = budget - team.getTotalBudget();
            team.setTotalBudget(budget);
            team.setRemainingBudget(team.getRemainingBudget() + budgetDiff);
            String newLogo = teamService.saveTeamLogo(logoFile);
            if (newLogo != null) {
                team.setLogoPath(newLogo);
            }
            teamService.updateTeam(team);
            teamService.updateTeamOwners(id, ownerIds);
            redirectAttributes.addFlashAttribute("successMessage", "Team updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error: " + e.getMessage());
        }
        return "redirect:/admin/teams";
    }

    @PostMapping("/teams/delete/{id}")
    public String deleteTeam(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        teamService.deleteTeam(id);
        redirectAttributes.addFlashAttribute("successMessage", "Team deleted successfully!");
        return "redirect:/admin/teams";
    }

    // ========== AUCTION MANAGEMENT ==========
    @GetMapping("/auction")
    public String auctionPage(@RequestParam(required = false) String search,
                              @RequestParam(required = false) PlayerCategory category,
                              @RequestParam(required = false) PlayerStatus status,
                              Model model) {
        List<Player> players;
        boolean hasFilters = (search != null && !search.isBlank()) || category != null || status != null;

        if (hasFilters) {
            players = playerService.searchPlayers(search, category, status);
        } else {
            // Default: show all players
            players = playerService.getAllPlayers();
        }

        model.addAttribute("players", players);
        model.addAttribute("searchQuery", search);
        model.addAttribute("selectedCategory", category);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("teams", teamService.getAllTeams());
        model.addAttribute("categories", PlayerCategory.values());
        model.addAttribute("statuses", PlayerStatus.values());
        model.addAttribute("winningBids", auctionService.getWinningBids());
        return "admin/auction";
    }

    @PostMapping("/auction/sell")
    public String sellPlayer(@RequestParam Long playerId,
                             @RequestParam Long teamId,
                             @RequestParam double soldPrice,
                             RedirectAttributes redirectAttributes) {
        String result = auctionService.sellPlayer(playerId, teamId, soldPrice);
        if ("SUCCESS".equals(result)) {
            redirectAttributes.addFlashAttribute("successMessage", "Player sold successfully!");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", result);
        }
        return "redirect:/admin/auction";
    }

    @PostMapping("/auction/unsold/{playerId}")
    public String markUnsold(@PathVariable Long playerId, RedirectAttributes redirectAttributes) {
        auctionService.markUnsold(playerId);
        redirectAttributes.addFlashAttribute("successMessage", "Player marked as unsold.");
        return "redirect:/admin/auction";
    }

    @PostMapping("/auction/reset/{playerId}")
    public String resetPlayer(@PathVariable Long playerId, RedirectAttributes redirectAttributes) {
        auctionService.resetPlayer(playerId);
        redirectAttributes.addFlashAttribute("successMessage", "Player reset to available.");
        return "redirect:/admin/auction";
    }

    // ========== USER MANAGEMENT ==========
    @GetMapping("/users")
    public String listUsers(Model model) {
        model.addAttribute("users", userService.getAllUsers());
        return "admin/users";
    }

    @GetMapping("/users/add")
    public String showAddUserForm(Model model) {
        model.addAttribute("roles", UserRole.values());
        return "admin/user-form";
    }

    @PostMapping("/users/add")
    public String addUser(@RequestParam String username,
                          @RequestParam String password,
                          @RequestParam String fullName,
                          @RequestParam UserRole role,
                          RedirectAttributes redirectAttributes) {
        try {
            userService.createUser(username, password, fullName, role);
            redirectAttributes.addFlashAttribute("successMessage", "User created successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @GetMapping("/users/edit/{id}")
    public String showEditUserForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        model.addAttribute("roles", UserRole.values());
        userService.findById(id).ifPresentOrElse(
                user -> model.addAttribute("user", user),
                () -> redirectAttributes.addFlashAttribute("errorMessage", "User not found.")
        );
        return "admin/user-form";
    }

    @PostMapping("/users/edit/{id}")
    public String updateUser(@PathVariable Long id,
                             @RequestParam String fullName,
                             @RequestParam UserRole role,
                             @RequestParam(required = false) String password,
                             RedirectAttributes redirectAttributes) {
        try {
            userService.updateUser(id, fullName, role, password);
            redirectAttributes.addFlashAttribute("successMessage", "User updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/users/delete/{id}")
    public String deleteUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            userService.deleteUser(id);
            redirectAttributes.addFlashAttribute("successMessage", "User deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Cannot delete user: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/users/import-excel")
    public String importUsersFromExcel(@RequestParam("excelFile") MultipartFile file,
                                       RedirectAttributes redirectAttributes) {
        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please select an Excel file to upload.");
            return "redirect:/admin/users";
        }

        String filename = file.getOriginalFilename();
        if (filename == null || (!filename.endsWith(".xlsx") && !filename.endsWith(".xls"))) {
            redirectAttributes.addFlashAttribute("errorMessage", "Only Excel files (.xlsx) are supported.");
            return "redirect:/admin/users";
        }

        ExcelImportService.ImportResult result = excelImportService.importUsersFromExcel(file);

        if (result.importedCount() > 0) {
            redirectAttributes.addFlashAttribute("successMessage",
                    result.importedCount() + " user(s) imported successfully!");
        }
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Some rows had errors: " + String.join("; ", result.errors()));
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/teams/import-excel")
    public String importTeamsFromExcel(@RequestParam("excelFile") MultipartFile file,
                                       RedirectAttributes redirectAttributes) {
        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please select an Excel file to upload.");
            return "redirect:/admin/teams";
        }

        String filename = file.getOriginalFilename();
        if (filename == null || (!filename.endsWith(".xlsx") && !filename.endsWith(".xls"))) {
            redirectAttributes.addFlashAttribute("errorMessage", "Only Excel files (.xlsx) are supported.");
            return "redirect:/admin/teams";
        }

        ExcelImportService.ImportResult result = excelImportService.importTeamsFromExcel(file);

        if (result.importedCount() > 0) {
            redirectAttributes.addFlashAttribute("successMessage",
                    result.importedCount() + " team(s) imported successfully!");
        }
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Some rows had errors: " + String.join("; ", result.errors()));
        }
        return "redirect:/admin/teams";
    }
}
