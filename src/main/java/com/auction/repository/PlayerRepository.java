package com.auction.repository;

import com.auction.model.Player;
import com.auction.model.PlayerCategory;
import com.auction.model.PlayerStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlayerRepository extends JpaRepository<Player, Long> {

    List<Player> findByStatus(PlayerStatus status);

    List<Player> findByCategory(PlayerCategory category);

    List<Player> findByCategoryAndStatus(PlayerCategory category, PlayerStatus status);

    List<Player> findByTeamId(Long teamId);

    @Query("SELECT p FROM Player p WHERE p.status = :status ORDER BY p.category, p.fullName")
    List<Player> findAvailablePlayers(@Param("status") PlayerStatus status);

    @Query("SELECT p FROM Player p WHERE p.status = :status" +
           " AND (:name IS NULL OR LOWER(p.fullName) LIKE LOWER(CONCAT('%', :name, '%')))" +
           " AND (:category IS NULL OR p.category = :category)" +
           " ORDER BY p.category, p.fullName")
    List<Player> searchAvailablePlayers(@Param("status") PlayerStatus status,
                                        @Param("name") String name,
                                        @Param("category") PlayerCategory category);

    @Query("SELECT p FROM Player p WHERE" +
           " (:status IS NULL OR p.status = :status)" +
           " AND (:name IS NULL OR LOWER(p.fullName) LIKE LOWER(CONCAT('%', :name, '%')))" +
           " AND (:category IS NULL OR p.category = :category)" +
           " ORDER BY p.category, p.fullName")
    List<Player> searchPlayers(@Param("status") PlayerStatus status,
                               @Param("name") String name,
                               @Param("category") PlayerCategory category);

    @Query("SELECT COUNT(p) FROM Player p WHERE p.team.id = :teamId AND p.category = :category")
    long countByTeamAndCategory(@Param("teamId") Long teamId, @Param("category") PlayerCategory category);

    @Query("SELECT COUNT(p) FROM Player p WHERE p.team.id = :teamId")
    long countByTeamId(@Param("teamId") Long teamId);
}
