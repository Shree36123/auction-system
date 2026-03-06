package com.auction.service;

import com.auction.model.Player;
import com.auction.model.PlayerCategory;
import com.auction.model.PlayerStatus;
import com.auction.repository.PlayerRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class PlayerService {

    private final PlayerRepository playerRepository;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    public PlayerService(PlayerRepository playerRepository) {
        this.playerRepository = playerRepository;
    }

    public Player registerPlayer(Player player, MultipartFile image) throws IOException {
        player.setTimestamp(LocalDateTime.now());
        player.setStatus(PlayerStatus.AVAILABLE);

        // Set default category if not provided
        if (player.getCategory() == null) {
            player.setCategory(PlayerCategory.OPEN);
        }

        if (image != null && !image.isEmpty()) {
            String fileName = UUID.randomUUID() + "_" + image.getOriginalFilename();
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            Files.copy(image.getInputStream(), uploadPath.resolve(fileName),
                    StandardCopyOption.REPLACE_EXISTING);
            player.setProfileImagePath(fileName);
        }

        return playerRepository.save(player);
    }

    public List<Player> getAllPlayers() {
        return playerRepository.findAll();
    }

    public Optional<Player> getPlayerById(Long id) {
        return playerRepository.findById(id);
    }

    public List<Player> getAvailablePlayers() {
        return playerRepository.findByStatus(PlayerStatus.AVAILABLE);
    }

    public List<Player> searchAvailablePlayers(String name, PlayerCategory category) {
        String searchName = (name != null && !name.isBlank()) ? name.trim() : null;
        return playerRepository.searchAvailablePlayers(PlayerStatus.AVAILABLE, searchName, category);
    }

    /**
     * Search players across all statuses (or a specific status) with optional name and category filters.
     */
    public List<Player> searchPlayers(String name, PlayerCategory category, PlayerStatus status) {
        String searchName = (name != null && !name.isBlank()) ? name.trim() : null;
        return playerRepository.searchPlayers(status, searchName, category);
    }

    public List<Player> getAvailablePlayersByCategory(PlayerCategory category) {
        return playerRepository.findByCategoryAndStatus(category, PlayerStatus.AVAILABLE);
    }

    public List<Player> getPlayersByTeam(Long teamId) {
        return playerRepository.findByTeamId(teamId);
    }

    public List<Player> getPlayersByStatus(PlayerStatus status) {
        return playerRepository.findByStatus(status);
    }

    public List<Player> getPlayersByCategory(PlayerCategory category) {
        return playerRepository.findByCategory(category);
    }

    public Player updatePlayer(Player player) {
        return playerRepository.save(player);
    }

    public void deletePlayer(Long id) {
        playerRepository.deleteById(id);
    }

    public long countPlayersInTeamByCategory(Long teamId, PlayerCategory category) {
        return playerRepository.countByTeamAndCategory(teamId, category);
    }

    public long countPlayersInTeam(Long teamId) {
        return playerRepository.countByTeamId(teamId);
    }
}
