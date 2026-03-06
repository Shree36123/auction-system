package com.auction.controller;

import com.auction.model.*;
import com.auction.service.*;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * Controller for Team Owner views.
 * Team Owners can view their team, budget, players, and the auction player list.
 */
@Controller
@RequestMapping("/owner")
public class OwnerController {

    private final TeamService teamService;
    private final PlayerService playerService;
    private final UserService userService;
    private final AuctionService auctionService;

    public OwnerController(TeamService teamService, PlayerService playerService,
                           UserService userService, AuctionService auctionService) {
        this.teamService = teamService;
        this.playerService = playerService;
        this.userService = userService;
        this.auctionService = auctionService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model, Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        if (currentUser == null) return "redirect:/login";

        model.addAttribute("currentUser", currentUser);

        List<Team> myTeams = teamService.getTeamsByOwnerId(currentUser.getId());
        if (!myTeams.isEmpty()) {
            Team team = myTeams.get(0); // primary team for dashboard
            model.addAttribute("team", team);
            model.addAttribute("teams", myTeams);
            model.addAttribute("players", playerService.getPlayersByTeam(team.getId()));
            model.addAttribute("categories", PlayerCategory.values());

            // Category-wise counts
            for (PlayerCategory cat : PlayerCategory.values()) {
                model.addAttribute("count_" + cat.name(),
                        playerService.countPlayersInTeamByCategory(team.getId(), cat));
            }
        }

        return "owner/dashboard";
    }

    @GetMapping("/players")
    public String viewAvailablePlayers(Model model, Authentication authentication,
                                       @RequestParam(required = false) PlayerCategory category) {
        User currentUser = getCurrentUser(authentication);
        if (currentUser == null) return "redirect:/login";

        model.addAttribute("currentUser", currentUser);

        List<Player> players;
        if (category != null) {
            players = playerService.getAvailablePlayersByCategory(category);
            model.addAttribute("selectedCategory", category);
        } else {
            players = playerService.getAvailablePlayers();
        }

        model.addAttribute("players", players);
        model.addAttribute("categories", PlayerCategory.values());

        List<Team> myTeams = teamService.getTeamsByOwnerId(currentUser.getId());
        if (!myTeams.isEmpty()) {
            model.addAttribute("team", myTeams.get(0));
        }

        return "owner/available-players";
    }

    @GetMapping("/team")
    public String viewMyTeam(Model model, Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        if (currentUser == null) return "redirect:/login";

        model.addAttribute("currentUser", currentUser);

        List<Team> myTeams = teamService.getTeamsByOwnerId(currentUser.getId());
        if (!myTeams.isEmpty()) {
            Team team = myTeams.get(0);
            model.addAttribute("team", team);
            model.addAttribute("players", playerService.getPlayersByTeam(team.getId()));
            model.addAttribute("categories", PlayerCategory.values());
        }

        return "owner/my-team";
    }

    private User getCurrentUser(Authentication authentication) {
        if (authentication == null) return null;
        return userService.findByUsername(authentication.getName()).orElse(null);
    }
}
