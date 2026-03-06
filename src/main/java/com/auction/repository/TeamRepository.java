package com.auction.repository;

import com.auction.model.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeamRepository extends JpaRepository<Team, Long> {

    @Query("SELECT t FROM Team t JOIN t.owners o WHERE o.id = :ownerId")
    Optional<Team> findByOwnerId(@Param("ownerId") Long ownerId);

    @Query("SELECT t FROM Team t JOIN t.owners o WHERE o.id = :ownerId")
    List<Team> findAllByOwnerId(@Param("ownerId") Long ownerId);

    Optional<Team> findByName(String name);

    boolean existsByName(String name);

    @Query("SELECT DISTINCT t FROM Team t LEFT JOIN FETCH t.players ORDER BY t.name")
    List<Team> findAllWithPlayers();

    @Query("SELECT DISTINCT t FROM Team t LEFT JOIN FETCH t.owners ORDER BY t.name")
    List<Team> findAllWithOwners();
}
