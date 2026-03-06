package com.auction.service;

import com.auction.model.Team;
import com.auction.model.User;
import com.auction.repository.TeamRepository;
import com.auction.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class TeamService {

    private final TeamRepository teamRepository;
    private final UserRepository userRepository;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    public TeamService(TeamRepository teamRepository, UserRepository userRepository) {
        this.teamRepository = teamRepository;
        this.userRepository = userRepository;
    }

    /**
     * Saves an uploaded team logo file and returns the stored filename.
     * Returns "default-logo.png" when no file is provided.
     */
    public String saveTeamLogo(MultipartFile logoFile) throws IOException {
        if (logoFile == null || logoFile.isEmpty()) {
            return null; // caller decides whether to keep existing
        }
        String ext = "";
        String orig = logoFile.getOriginalFilename();
        if (orig != null && orig.contains(".")) {
            ext = orig.substring(orig.lastIndexOf('.'));
        }
        String fileName = "team_" + UUID.randomUUID() + ext;
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        Files.copy(logoFile.getInputStream(), uploadPath.resolve(fileName),
                StandardCopyOption.REPLACE_EXISTING);
        return fileName;
    }

    public Team createTeam(String name, double budget, User owner, String logoPath) {
        Team team = new Team(name, budget, owner);
        team.setLogoPath(logoPath != null ? logoPath : "default-logo.png");
        return teamRepository.save(team);
    }

    public Team createTeam(String name, double budget, List<User> owners, String logoPath) {
        Team team = new Team(name, budget, owners);
        team.setLogoPath(logoPath != null ? logoPath : "default-logo.png");
        return teamRepository.save(team);
    }

    /**
     * Create a team by loading owners from their IDs within the same transaction.
     * This avoids detached entity issues when users are loaded in a different transaction.
     */
    public Team createTeamByOwnerIds(String name, double budget, List<Long> ownerIds, String logoPath) {
        List<User> owners = new ArrayList<>();
        for (Long ownerId : ownerIds) {
            userRepository.findById(ownerId).ifPresent(owners::add);
        }
        if (owners.isEmpty()) {
            throw new RuntimeException("At least one valid owner is required");
        }
        Team team = new Team(name, budget, owners);
        team.setLogoPath(logoPath != null ? logoPath : "default-logo.png");
        return teamRepository.save(team);
    }

    /**
     * Update owners for an existing team by reloading users within the same transaction.
     */
    public Team updateTeamOwners(Long teamId, List<Long> ownerIds) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Team not found"));
        List<User> owners = new ArrayList<>();
        for (Long ownerId : ownerIds) {
            userRepository.findById(ownerId).ifPresent(owners::add);
        }
        if (owners.isEmpty()) {
            throw new RuntimeException("At least one valid owner is required");
        }
        team.setOwners(owners);
        return teamRepository.save(team);
    }

    public List<Team> getAllTeams() {
        return teamRepository.findAll();
    }

    public List<Team> getAllTeamsWithPlayers() {
        // Fetch players first, then owners in a second query to avoid MultipleBagFetchException
        List<Team> teams = teamRepository.findAllWithPlayers();
        // Initialize owners in the same persistence context
        teamRepository.findAllWithOwners();
        // Force-initialize owners on cached entities as a safety measure
        for (Team team : teams) {
            if (team.getOwners() != null) {
                team.getOwners().size();
            }
        }
        return teams;
    }

    public Optional<Team> getTeamById(Long id) {
        return teamRepository.findById(id);
    }

    public Optional<Team> getTeamByOwnerId(Long ownerId) {
        return teamRepository.findByOwnerId(ownerId);
    }

    public List<Team> getTeamsByOwnerId(Long ownerId) {
        return teamRepository.findAllByOwnerId(ownerId);
    }

    public Team updateTeam(Team team) {
        return teamRepository.save(team);
    }

    public void deleteTeam(Long id) {
        teamRepository.deleteById(id);
    }

    public boolean teamNameExists(String name) {
        return teamRepository.existsByName(name);
    }
}
