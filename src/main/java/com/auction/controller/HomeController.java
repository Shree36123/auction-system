package com.auction.controller;

import com.auction.model.*;
import com.auction.service.*;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Optional;

@Controller
public class HomeController {

    private final TeamService teamService;
    private final PlayerService playerService;
    private final UserService userService;
    private final AuctionService auctionService;

    public HomeController(TeamService teamService, PlayerService playerService, UserService userService, AuctionService auctionService) {
        this.teamService = teamService;
        this.playerService = playerService;
        this.userService = userService;
        this.auctionService = auctionService;
    }

    @GetMapping("/")
    public String home(Model model, Authentication authentication) {
        List<Team> teams = teamService.getAllTeams();
        long totalPlayers = playerService.getAllPlayers().size();
        long soldPlayers = playerService.getPlayersByStatus(PlayerStatus.SOLD).size();
        long availablePlayers = playerService.getPlayersByStatus(PlayerStatus.AVAILABLE).size();

        model.addAttribute("teams", teams);
        model.addAttribute("totalPlayers", totalPlayers);
        model.addAttribute("soldPlayers", soldPlayers);
        model.addAttribute("availablePlayers", availablePlayers);

        if (authentication != null) {
            Optional<User> user = userService.findByUsername(authentication.getName());
            user.ifPresent(u -> {
                model.addAttribute("currentUser", u);
                if (u.getRole() == UserRole.TEAM_OWNER) {
                    teamService.getTeamByOwnerId(u.getId()).ifPresent(t ->
                            model.addAttribute("myTeam", t));
                }
            });
        }

        return "home";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model, Authentication authentication) {
        if (authentication == null) return "redirect:/login";

        Optional<User> user = userService.findByUsername(authentication.getName());
        if (user.isEmpty()) return "redirect:/login";

        User currentUser = user.get();
        model.addAttribute("currentUser", currentUser);

        if (currentUser.getRole() == UserRole.ADMIN) {
            return "redirect:/admin/dashboard";
        } else {
            return "redirect:/owner/dashboard";
        }
    }
}
