package com.auction.service;

import com.auction.model.*;
import com.auction.repository.AuctionBidRepository;
import com.auction.repository.PlayerRepository;
import com.auction.repository.TeamRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class AuctionService {

    private final PlayerRepository playerRepository;
    private final TeamRepository teamRepository;
    private final AuctionBidRepository bidRepository;

    public AuctionService(PlayerRepository playerRepository,
                          TeamRepository teamRepository,
                          AuctionBidRepository bidRepository) {
        this.playerRepository = playerRepository;
        this.teamRepository = teamRepository;
        this.bidRepository = bidRepository;
    }

    /**
     * Place a bid on a player by a team.
     * Validates budget, team size, and player availability.
     */
    public String placeBid(Long playerId, Long teamId, double bidAmount) {
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new RuntimeException("Player not found"));
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Team not found"));

        // Validation checks
        if (player.getStatus() != PlayerStatus.AVAILABLE) {
            return "Player is no longer available for auction.";
        }

        if (!team.canBuyMore()) {
            return "Team already has the maximum number of players (" + Team.MAX_PLAYERS + ").";
        }

        if (!team.canBuyPlayerByCategory(player.getCategory())) {
            return "Team cannot add another " + player.getCategory().getLabel()
                    + " player. Limits: Open=4, 30+=max 2 (incl. 1 shared extra), 40+=max 2 (incl. 1 shared extra), 45+=2.";
        }

        if (bidAmount > team.getRemainingBudget()) {
            return "Insufficient budget. Remaining budget: " + team.getRemainingBudget();
        }

        if (bidAmount < player.getBasePrice()) {
            return "Bid must be at least the base price: " + player.getBasePrice();
        }

        // Record the bid
        AuctionBid bid = new AuctionBid(player, team, bidAmount);
        bidRepository.save(bid);

        return "BID_PLACED";
    }

    /**
     * Sell a player to the highest bidding team.
     */
    public String sellPlayer(Long playerId, Long teamId, double soldPrice) {
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new RuntimeException("Player not found"));
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Team not found"));

        if (player.getStatus() != PlayerStatus.AVAILABLE) {
            return "Player is no longer available.";
        }

        if (!team.canBuyMore()) {
            return "Team already has maximum players.";
        }

        if (!team.canBuyPlayerByCategory(player.getCategory())) {
            return "Team cannot add another " + player.getCategory().getLabel()
                    + " player. Limits: Open=4, 30+=max 2 (incl. 1 shared extra), 40+=max 2 (incl. 1 shared extra), 45+=2.";
        }

        if (soldPrice > team.getRemainingBudget()) {
            return "Insufficient budget for this purchase.";
        }

        // Update player
        player.setStatus(PlayerStatus.SOLD);
        player.setTeam(team);
        player.setSoldPrice(soldPrice);
        playerRepository.save(player);

        // Update team budget
        team.setRemainingBudget(team.getRemainingBudget() - soldPrice);
        teamRepository.save(team);

        // Mark winning bid
        AuctionBid winningBid = new AuctionBid(player, team, soldPrice);
        winningBid.setWinningBid(true);
        bidRepository.save(winningBid);

        return "SUCCESS";
    }

    /**
     * Mark player as unsold.
     */
    public void markUnsold(Long playerId) {
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new RuntimeException("Player not found"));
        player.setStatus(PlayerStatus.UNSOLD);
        playerRepository.save(player);
    }

    /**
     * Get bid history for a player.
     */
    public List<AuctionBid> getBidsForPlayer(Long playerId) {
        return bidRepository.findByPlayerIdOrderByBidAmountDesc(playerId);
    }

    /**
     * Get all winning bids.
     */
    public List<AuctionBid> getWinningBids() {
        return bidRepository.findAllWinningBids();
    }

    /**
     * Reset a player back to available (admin function).
     */
    public void resetPlayer(Long playerId) {
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new RuntimeException("Player not found"));

        if (player.getTeam() != null) {
            Team team = player.getTeam();
            team.setRemainingBudget(team.getRemainingBudget() + player.getSoldPrice());
            teamRepository.save(team);
        }

        player.setStatus(PlayerStatus.AVAILABLE);
        player.setTeam(null);
        player.setSoldPrice(0);
        playerRepository.save(player);
    }
}
