# Auction System

A Java Spring Boot web application for managing player auctions across teams.

## Features

- **Player Management**: Register players with details (name, age, phone, jersey size, achievements, professional image)
- **Team Management**: Create teams with budget allocation
- **Auction System**: Conduct live auctions - sell players to teams within budget constraints
- **Category-based Players**: Players are categorized by age (Open, 30+, 35+, 40+)
- **Budget Tracking**: Real-time budget tracking for each team
- **Team Roster**: Each team must buy 9 players across all categories
- **Role-based Access**: Admin and Team Owner roles with different views
- **Team Owners as Players**: Team owners can also participate as players

## Tech Stack

- **Backend**: Spring Boot 3.2, Spring MVC, Spring Security, Spring Data JPA
- **Frontend**: Thymeleaf, Bootstrap 5, Bootstrap Icons
- **Database**: H2 (development), MySQL (production-ready)
- **Build**: Maven

## Quick Start

### Prerequisites
- Java 17+
- Maven 3.8+

### Run the Application

```bash
cd auction-system
mvn spring-boot:run
```

The application starts at: **http://localhost:8080**

### Default Login Credentials

| Role | Username | Password |
|------|----------|----------|
| Admin | `admin` | `admin123` |
| Team Owner 1 | `owner1` | `owner123` |
| Team Owner 2 | `owner2` | `owner123` |
| Team Owner 3 | `owner3` | `owner123` |
| Team Owner 4 | `owner4` | `owner123` |

## Project Structure

```
auction-system/
├── src/main/java/com/auction/
│   ├── AuctionSystemApplication.java
│   ├── config/
│   │   ├── SecurityConfig.java
│   │   ├── WebConfig.java
│   │   └── DataInitializer.java
│   ├── controller/
│   │   ├── HomeController.java
│   │   ├── AdminController.java
│   │   └── OwnerController.java
│   ├── model/
│   │   ├── User.java
│   │   ├── Player.java
│   │   ├── Team.java
│   │   ├── AuctionBid.java
│   │   ├── PlayerCategory.java
│   │   ├── PlayerStatus.java
│   │   ├── AuctionStatus.java
│   │   └── UserRole.java
│   ├── repository/
│   │   ├── UserRepository.java
│   │   ├── PlayerRepository.java
│   │   ├── TeamRepository.java
│   │   └── AuctionBidRepository.java
│   └── service/
│       ├── UserService.java
│       ├── PlayerService.java
│       ├── TeamService.java
│       ├── AuctionService.java
│       └── CustomUserDetailsService.java
├── src/main/resources/
│   ├── application.properties
│   ├── templates/
│   │   ├── home.html
│   │   ├── login.html
│   │   ├── fragments/layout.html
│   │   ├── admin/
│   │   │   ├── dashboard.html
│   │   │   ├── players.html
│   │   │   ├── player-form.html
│   │   │   ├── teams.html
│   │   │   ├── team-form.html
│   │   │   ├── team-detail.html
│   │   │   ├── auction.html
│   │   │   ├── users.html
│   │   │   └── user-form.html
│   │   └── owner/
│   │       ├── dashboard.html
│   │       ├── available-players.html
│   │       └── my-team.html
│   └── static/css/style.css
└── pom.xml
```

## Player Categories

| Category | Age Range | Description |
|----------|-----------|-------------|
| Open     | Under 30  | No age restriction |
| 30+      | 30-35     | Players aged 30 and above |
| 35+      | 35-39     | Players aged 30 and above |
| 40+      | 40-44     | Players aged 40 and above |

## Rules

1. Each team has a **fixed budget** to buy players
2. Each team must buy **9 players** across categories
3. Players have a **base price** - bids must meet or exceed it
4. Admin conducts the auction and assigns players to teams
5. Team owners can view available players and their team roster
6. Category is auto-assigned based on player age

## Database Console

H2 Console available at: **http://localhost:8080/h2-console**
- JDBC URL: `jdbc:h2:file:./data/auctiondb`
- Username: `sa`
- Password: *(empty)*
