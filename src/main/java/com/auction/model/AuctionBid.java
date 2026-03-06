package com.auction.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Records each bid made during auction.
 */
@Entity
@Table(name = "auction_bids")
public class AuctionBid {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @Column(nullable = false)
    private double bidAmount;

    @Column(nullable = false)
    private LocalDateTime bidTime;

    private boolean winningBid = false;

    // --- Constructors ---
    public AuctionBid() {
        this.bidTime = LocalDateTime.now();
    }

    public AuctionBid(Player player, Team team, double bidAmount) {
        this.player = player;
        this.team = team;
        this.bidAmount = bidAmount;
        this.bidTime = LocalDateTime.now();
    }

    // --- Getters & Setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Player getPlayer() { return player; }
    public void setPlayer(Player player) { this.player = player; }

    public Team getTeam() { return team; }
    public void setTeam(Team team) { this.team = team; }

    public double getBidAmount() { return bidAmount; }
    public void setBidAmount(double bidAmount) { this.bidAmount = bidAmount; }

    public LocalDateTime getBidTime() { return bidTime; }
    public void setBidTime(LocalDateTime bidTime) { this.bidTime = bidTime; }

    public boolean isWinningBid() { return winningBid; }
    public void setWinningBid(boolean winningBid) { this.winningBid = winningBid; }
}
