package com.auction.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "players")
public class Player {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Registration timestamp */
    private LocalDateTime timestamp;

    /** Category: OPEN, ABOVE_30, ABOVE_40, ABOVE_45 */
    @Enumerated(EnumType.STRING)
    private PlayerCategory category;

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false)
    private String fullName;

    @NotBlank
    @Size(max = 15)
    private String phoneNumber;

    private String jerseySize;

    @Column(length = 1000)
    private String achievements;

    /** File path to the professional image */
    private String profileImagePath;

    /** Base price for auction */
    private Double basePrice;

    /** Current / sold price */
    private Double soldPrice;

    /** Auction status */
    @Enumerated(EnumType.STRING)
    private PlayerStatus status = PlayerStatus.AVAILABLE;

    /** Team this player is sold to (null if unsold) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    /** If the player is also a team owner */
    private boolean isOwner = false;

    // --- Constructors ---
    public Player() {
        this.timestamp = LocalDateTime.now();
    }

    // --- Getters & Setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public PlayerCategory getCategory() { return category; }
    public void setCategory(PlayerCategory category) { this.category = category; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getJerseySize() { return jerseySize; }
    public void setJerseySize(String jerseySize) { this.jerseySize = jerseySize; }

    public String getAchievements() { return achievements; }
    public void setAchievements(String achievements) { this.achievements = achievements; }

    public String getProfileImagePath() { return profileImagePath; }
    public void setProfileImagePath(String profileImagePath) { this.profileImagePath = profileImagePath; }

    public Double getBasePrice() { return basePrice != null ? basePrice : 0.0; }
    public void setBasePrice(Double basePrice) { this.basePrice = basePrice; }

    public double getSoldPrice() { return soldPrice; }
    public void setSoldPrice(double soldPrice) { this.soldPrice = soldPrice; }

    public PlayerStatus getStatus() { return status; }
    public void setStatus(PlayerStatus status) { this.status = status; }

    public Team getTeam() { return team; }
    public void setTeam(Team team) { this.team = team; }

    public boolean isOwner() { return isOwner; }
    public void setOwner(boolean owner) { isOwner = owner; }

    public String getCategoryLabel() {
        return category != null ? category.getLabel() : "";
    }
}
