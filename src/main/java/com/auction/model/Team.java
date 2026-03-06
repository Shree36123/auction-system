package com.auction.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "teams")
public class Team {

    public static final int MAX_PLAYERS = 9;

    // Category slot limits per team
    public static final int MAX_OPEN    = 4;  // 4 Open category players
    public static final int MAX_ABOVE_30 = 1; // 1 guaranteed 30+ slot
    public static final int MAX_ABOVE_40 = 1; // 1 guaranteed 40+ slot
    public static final int MAX_ABOVE_45 = 2; // 2 slots for 45+
    // 1 extra slot shared between ABOVE_30 and ABOVE_40 (not mandatory)
    public static final int MAX_EXTRA_ABOVE_30_40 = 1;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 100)
    @Column(unique = true, nullable = false)
    private String name;

    @Column(nullable = false)
    private String logoPath;

    /** Total budget allocated to this team */
    @Column(nullable = false)
    private double totalBudget;

    /** Remaining budget */
    @Column(nullable = false)
    private double remainingBudget;

    /** Owners of the team (multiple user accounts) */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "team_owners",
        joinColumns = @JoinColumn(name = "team_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private List<User> owners = new ArrayList<>();

    /** Players bought by this team */
    @OneToMany(mappedBy = "team", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Player> players = new ArrayList<>();

    // --- Constructors ---
    public Team() {}

    public Team(String name, double totalBudget) {
        this.name = name;
        this.totalBudget = totalBudget;
        this.remainingBudget = totalBudget;
    }

    public Team(String name, double totalBudget, User owner) {
        this.name = name;
        this.totalBudget = totalBudget;
        this.remainingBudget = totalBudget;
        if (owner != null) {
            this.owners.add(owner);
        }
    }

    public Team(String name, double totalBudget, List<User> owners) {
        this.name = name;
        this.totalBudget = totalBudget;
        this.remainingBudget = totalBudget;
        if (owners != null) {
            this.owners.addAll(owners);
        }
    }

    // --- Business Methods ---
    public int getPlayerCount() {
        return players != null ? players.size() : 0;
    }

    public boolean canBuyMore() {
        return getPlayerCount() < MAX_PLAYERS;
    }

    public double getSpentBudget() {
        return totalBudget - remainingBudget;
    }

    public int getSlotsRemaining() {
        return MAX_PLAYERS - getPlayerCount();
    }

    public long getPlayerCountByCategory(PlayerCategory category) {
        return players.stream()
                .filter(p -> p.getCategory() == category)
                .count();
    }

    /**
     * Check if this team can buy one more player in the given category.
     * Rules:
     *   - OPEN:     max 4
     *   - ABOVE_30: max 2 (1 guaranteed + 1 shared extra with ABOVE_40)
     *   - ABOVE_40: max 2 (1 guaranteed + 1 shared extra with ABOVE_30)
     *   - ABOVE_45: max 2
     *   Combined ABOVE_30 + ABOVE_40 must not exceed 3 (1 + 1 + 1 shared extra).
     *   Total players must not exceed MAX_PLAYERS (9).
     */
    public boolean canBuyPlayerByCategory(PlayerCategory category) {
        long openCount    = getPlayerCountByCategory(PlayerCategory.OPEN);
        long above30Count = getPlayerCountByCategory(PlayerCategory.ABOVE_30);
        long above40Count = getPlayerCountByCategory(PlayerCategory.ABOVE_40);
        long above45Count = getPlayerCountByCategory(PlayerCategory.ABOVE_45);
        long total = openCount + above30Count + above40Count + above45Count;

        if (total >= MAX_PLAYERS) return false;

        return switch (category) {
            case OPEN     -> openCount < MAX_OPEN;
            case ABOVE_30 -> above30Count < (MAX_ABOVE_30 + MAX_EXTRA_ABOVE_30_40)
                             && (above30Count + above40Count) < (MAX_ABOVE_30 + MAX_ABOVE_40 + MAX_EXTRA_ABOVE_30_40);
            case ABOVE_40 -> above40Count < (MAX_ABOVE_40 + MAX_EXTRA_ABOVE_30_40)
                             && (above30Count + above40Count) < (MAX_ABOVE_30 + MAX_ABOVE_40 + MAX_EXTRA_ABOVE_30_40);
            case ABOVE_45 -> above45Count < MAX_ABOVE_45;
        };
    }

    /** Remaining slots for a given category (respects shared-extra rule). */
    public long getCategorySlotRemaining(PlayerCategory category) {
        long above30Count = getPlayerCountByCategory(PlayerCategory.ABOVE_30);
        long above40Count = getPlayerCountByCategory(PlayerCategory.ABOVE_40);
        long above45Count = getPlayerCountByCategory(PlayerCategory.ABOVE_45);
        long openCount    = getPlayerCountByCategory(PlayerCategory.OPEN);
        long total = openCount + above30Count + above40Count + above45Count;
        long totalRemaining = MAX_PLAYERS - total;

        return switch (category) {
            case OPEN     -> Math.min(MAX_OPEN - openCount, totalRemaining);
            case ABOVE_30 -> Math.min(
                    Math.min(MAX_ABOVE_30 + MAX_EXTRA_ABOVE_30_40 - above30Count,
                             MAX_ABOVE_30 + MAX_ABOVE_40 + MAX_EXTRA_ABOVE_30_40 - above30Count - above40Count),
                    totalRemaining);
            case ABOVE_40 -> Math.min(
                    Math.min(MAX_ABOVE_40 + MAX_EXTRA_ABOVE_30_40 - above40Count,
                             MAX_ABOVE_30 + MAX_ABOVE_40 + MAX_EXTRA_ABOVE_30_40 - above30Count - above40Count),
                    totalRemaining);
            case ABOVE_45 -> Math.min(MAX_ABOVE_45 - above45Count, totalRemaining);
        };
    }

    /** Human-readable slot summary for UI display. */
    public String getSlotSummary() {
        return "Open:" + getPlayerCountByCategory(PlayerCategory.OPEN) + "/" + MAX_OPEN
                + " | 30+:" + getPlayerCountByCategory(PlayerCategory.ABOVE_30) + "/" + (MAX_ABOVE_30 + MAX_EXTRA_ABOVE_30_40)
                + " | 40+:" + getPlayerCountByCategory(PlayerCategory.ABOVE_40) + "/" + (MAX_ABOVE_40 + MAX_EXTRA_ABOVE_30_40)
                + " | 45+:" + getPlayerCountByCategory(PlayerCategory.ABOVE_45) + "/" + MAX_ABOVE_45;
    }

    // --- Getters & Setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getLogoPath() { return logoPath; }
    public void setLogoPath(String logoPath) { this.logoPath = logoPath; }

    public double getTotalBudget() { return totalBudget; }
    public void setTotalBudget(double totalBudget) { this.totalBudget = totalBudget; }

    public double getRemainingBudget() { return remainingBudget; }
    public void setRemainingBudget(double remainingBudget) { this.remainingBudget = remainingBudget; }

    public List<User> getOwners() { return owners; }
    public void setOwners(List<User> owners) { this.owners = owners; }

    /** Convenience: get first owner (for backward compatibility) */
    public User getOwner() {
        return owners != null && !owners.isEmpty() ? owners.get(0) : null;
    }

    /** Convenience: get owners as comma-separated names */
    public String getOwnerNames() {
        if (owners == null || owners.isEmpty()) return "No Owner";
        return owners.stream()
                .map(User::getFullName)
                .reduce((a, b) -> a + ", " + b)
                .orElse("No Owner");
    }

    public List<Player> getPlayers() { return players; }
    public void setPlayers(List<Player> players) { this.players = players; }
}
