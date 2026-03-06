package com.auction.repository;

import com.auction.model.AuctionBid;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuctionBidRepository extends JpaRepository<AuctionBid, Long> {

    List<AuctionBid> findByPlayerIdOrderByBidAmountDesc(Long playerId);

    List<AuctionBid> findByTeamIdOrderByBidTimeDesc(Long teamId);

    @Query("SELECT ab FROM AuctionBid ab WHERE ab.winningBid = true ORDER BY ab.bidTime DESC")
    List<AuctionBid> findAllWinningBids();

    @Query("SELECT ab FROM AuctionBid ab WHERE ab.player.id = :playerId AND ab.winningBid = true")
    AuctionBid findWinningBidForPlayer(@Param("playerId") Long playerId);
}
