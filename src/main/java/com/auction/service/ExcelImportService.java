package com.auction.service;

import com.auction.model.*;
import com.auction.repository.PlayerRepository;
import com.auction.repository.TeamRepository;
import com.auction.repository.UserRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Service to import player, user, and team details from Excel (.xlsx) files.
 * Reads column positions dynamically from the header row (case-insensitive).
 */
@Service
public class ExcelImportService {

    private final PlayerRepository playerRepository;
    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final PasswordEncoder passwordEncoder;

    public ExcelImportService(PlayerRepository playerRepository,
                              UserRepository userRepository,
                              TeamRepository teamRepository,
                              PasswordEncoder passwordEncoder) {
        this.playerRepository = playerRepository;
        this.userRepository = userRepository;
        this.teamRepository = teamRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public ImportResult importPlayersFromExcel(MultipartFile file) {
        List<Player> players = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        try (InputStream is = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rows = sheet.iterator();

            // Read header row to build column index map
            if (!rows.hasNext()) {
                errors.add("Excel file is empty");
                return new ImportResult(0, errors);
            }

            Row headerRow = rows.next();
            Map<String, Integer> columnMap = buildColumnMap(headerRow);

            while (rows.hasNext()) {
                Row row = rows.next();

                // Skip completely empty rows
                if (isRowEmpty(row)) continue;

                try {
                    Player player = parsePlayerRow(row, columnMap);
                    players.add(player);
                } catch (Exception e) {
                    errors.add("Row " + (row.getRowNum() + 1) + ": " + e.getMessage());
                }
            }

            int savedCount = 0;
            for (int i = 0; i < players.size(); i++) {
                try {
                    playerRepository.save(players.get(i));
                    savedCount++;
                } catch (Exception ex) {
                    errors.add("Save error for '" + players.get(i).getFullName() + "': " + ex.getMessage());
                }
            }
            return new ImportResult(savedCount, errors);

        } catch (Exception e) {
            errors.add("Failed to read Excel file: " + e.getMessage());
        }

        return new ImportResult(0, errors);
    }

    /**
     * Import users from an Excel file.
     * Expected columns: username, password, fullName/full name, role (ADMIN/TEAM_OWNER/owner/admin)
     */
    public ImportResult importUsersFromExcel(MultipartFile file) {
        List<String> errors = new ArrayList<>();
        int savedCount = 0;

        try (InputStream is = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rows = sheet.iterator();

            if (!rows.hasNext()) {
                errors.add("Excel file is empty");
                return new ImportResult(0, errors);
            }

            Row headerRow = rows.next();
            Map<String, Integer> columnMap = buildColumnMap(headerRow);

            while (rows.hasNext()) {
                Row row = rows.next();
                if (isRowEmpty(row)) continue;

                try {
                    // Username
                    Integer usernameCol = findColumn(columnMap, "username", "user name", "user", "login");
                    String username = usernameCol != null ? getStringCellValue(row, usernameCol) : null;
                    if (username == null || username.isBlank()) {
                        throw new IllegalArgumentException("Username is required");
                    }
                    username = username.trim();

                    // Check if username already exists
                    if (userRepository.existsByUsernameIgnoreCase(username)) {
                        errors.add("Row " + (row.getRowNum() + 1) + ": Username '" + username + "' already exists, skipped");
                        continue;
                    }

                    // Password
                    Integer passwordCol = findColumn(columnMap, "password", "pass", "pwd");
                    String password = passwordCol != null ? getStringCellValue(row, passwordCol) : null;
                    if (password == null || password.isBlank()) {
                        password = username + "123"; // default password
                    }

                    // Full Name
                    Integer nameCol = findColumn(columnMap, "fullname", "full name", "name", "displayname");
                    String fullName = nameCol != null ? getStringCellValue(row, nameCol) : null;
                    if (fullName == null || fullName.isBlank()) {
                        fullName = username; // default to username
                    }

                    // Role
                    Integer roleCol = findColumn(columnMap, "role", "userrole", "user role", "type");
                    String roleStr = roleCol != null ? getStringCellValue(row, roleCol) : null;
                    UserRole role = parseUserRole(roleStr);

                    User user = new User(username, passwordEncoder.encode(password.trim()), fullName.trim(), role);
                    userRepository.save(user);
                    savedCount++;

                } catch (Exception e) {
                    errors.add("Row " + (row.getRowNum() + 1) + ": " + e.getMessage());
                }
            }

        } catch (Exception e) {
            errors.add("Failed to read Excel file: " + e.getMessage());
        }

        return new ImportResult(savedCount, errors);
    }

    /**
     * Import teams from an Excel file.
     * Expected columns: name/team name, budget/total budget, owner/owner username, logo/logo path
     */
    public ImportResult importTeamsFromExcel(MultipartFile file) {
        List<String> errors = new ArrayList<>();
        int savedCount = 0;

        try (InputStream is = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rows = sheet.iterator();

            if (!rows.hasNext()) {
                errors.add("Excel file is empty");
                return new ImportResult(0, errors);
            }

            Row headerRow = rows.next();
            Map<String, Integer> columnMap = buildColumnMap(headerRow);

            while (rows.hasNext()) {
                Row row = rows.next();
                if (isRowEmpty(row)) continue;

                try {
                    // Team Name
                    Integer nameCol = findColumn(columnMap, "name", "teamname", "team name", "team");
                    String name = nameCol != null ? getStringCellValue(row, nameCol) : null;
                    if (name == null || name.isBlank()) {
                        throw new IllegalArgumentException("Team name is required");
                    }
                    name = name.trim();

                    // Check if team name already exists
                    if (teamRepository.existsByName(name)) {
                        errors.add("Row " + (row.getRowNum() + 1) + ": Team '" + name + "' already exists, skipped");
                        continue;
                    }

                    // Budget
                    Integer budgetCol = findColumn(columnMap, "budget", "totalbudget", "total budget", "amount", "purse");
                    double budget = budgetCol != null ? getDoubleCellValue(row, budgetCol) : 0;
                    if (budget <= 0) {
                        budget = 100000; // default budget
                    }

                    // Owner(s) (by username - comma separated for multiple owners)
                    Integer ownerCol = findColumn(columnMap, "owner", "owners", "ownerusername", "owner username", "username", "owneruser");
                    String ownerValue = ownerCol != null ? getStringCellValue(row, ownerCol) : null;
                    List<User> owners = new ArrayList<>();
                    if (ownerValue != null && !ownerValue.isBlank()) {
                        String[] ownerNames = ownerValue.split(",");
                        for (String ownerName : ownerNames) {
                            String trimmedOwner = ownerName.trim();
                            if (!trimmedOwner.isEmpty()) {
                                Optional<User> ownerOpt = userRepository.findByUsernameIgnoreCase(trimmedOwner);
                                if (ownerOpt.isPresent()) {
                                    owners.add(ownerOpt.get());
                                } else {
                                    errors.add("Row " + (row.getRowNum() + 1) + ": Owner '" + trimmedOwner + "' not found");
                                }
                            }
                        }
                    }

                    // Logo path
                    Integer logoCol = findColumn(columnMap, "logo", "logopath", "logo path", "image");
                    String logoPath = logoCol != null ? getStringCellValue(row, logoCol) : null;
                    if (logoPath == null || logoPath.isBlank()) {
                        logoPath = "default-logo.png";
                    }

                    Team team = new Team(name, budget, owners);
                    team.setLogoPath(logoPath.trim());
                    teamRepository.save(team);
                    savedCount++;

                } catch (Exception e) {
                    errors.add("Row " + (row.getRowNum() + 1) + ": " + e.getMessage());
                }
            }

        } catch (Exception e) {
            errors.add("Failed to read Excel file: " + e.getMessage());
        }

        return new ImportResult(savedCount, errors);
    }

    private UserRole parseUserRole(String roleStr) {
        if (roleStr == null || roleStr.isBlank()) return UserRole.TEAM_OWNER;
        String val = roleStr.trim().toLowerCase().replaceAll("[^a-z]", "");
        if (val.contains("admin")) return UserRole.ADMIN;
        if (val.contains("owner") || val.contains("team")) return UserRole.TEAM_OWNER;
        try {
            return UserRole.valueOf(roleStr.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return UserRole.TEAM_OWNER;
        }
    }

    /**
     * Builds a map of lowercase header name -> column index.
     */
    private Map<String, Integer> buildColumnMap(Row headerRow) {
        Map<String, Integer> map = new HashMap<>();
        for (Cell cell : headerRow) {
            if (cell.getCellType() == CellType.STRING) {
                String header = cell.getStringCellValue().trim().toLowerCase()
                        .replaceAll("[^a-z0-9]", ""); // normalize: remove spaces, special chars
                map.put(header, cell.getColumnIndex());
            }
        }
        return map;
    }

    /**
     * Finds the column index for a field by trying multiple possible header names.
     */
    private Integer findColumn(Map<String, Integer> columnMap, String... possibleNames) {
        for (String name : possibleNames) {
            String normalized = name.toLowerCase().replaceAll("[^a-z0-9]", "");
            if (columnMap.containsKey(normalized)) {
                return columnMap.get(normalized);
            }
        }
        return null;
    }

    private Player parsePlayerRow(Row row, Map<String, Integer> columnMap) {
        Player player = new Player();
        player.setTimestamp(LocalDateTime.now());
        player.setStatus(PlayerStatus.AVAILABLE);

        // Full Name
        Integer nameCol = findColumn(columnMap, "Player name","fullname", "full name", "name", "playername", "player name");
        String fullName = nameCol != null ? getStringCellValue(row, nameCol) : null;
        if (fullName == null || fullName.isBlank()) {
            throw new IllegalArgumentException("Full Name is required");
        }
        player.setFullName(fullName.trim());

        // Phone Number
        Integer phoneCol = findColumn(columnMap, "Phone","phonenumber", "phone number", "phone", "mobile", "contact", "mobilenumber");
        String phone = phoneCol != null ? getStringCellValue(row, phoneCol) : null;
        player.setPhoneNumber(phone != null && !phone.isBlank() ? phone.trim() : "N/A");

        // Category defaults to OPEN (can be set from Excel if a Category column exists)
        Integer catCol = findColumn(columnMap, "Category","category", "playercategory", "player category");
        String catVal = catCol != null ? getStringCellValue(row, catCol) : null;
        PlayerCategory parsedCategory = PlayerCategory.fromString(catVal);
        player.setCategory(parsedCategory != null ? parsedCategory : PlayerCategory.OPEN);

        // Jersey Size
        Integer jerseyCol = findColumn(columnMap, "Jersey size","jerseysize", "jersey size", "jersey", "tshirtsize", "size");
        String jerseySize = jerseyCol != null ? getStringCellValue(row, jerseyCol) : null;
        player.setJerseySize(jerseySize != null ? jerseySize.trim() : "");

        // Base Price
        Integer priceCol = findColumn(columnMap, "Base price","baseprice", "base price", "price", "baseamount", "amount");
        double basePrice = priceCol != null ? getDoubleCellValue(row, priceCol) : 0;
        player.setBasePrice(basePrice);

        // Achievements
        Integer achieveCol = findColumn(columnMap, "Achievements","achievements", "achievement", "awards", "experience", "description");
        String achievements = achieveCol != null ? getStringCellValue(row, achieveCol) : "";
        player.setAchievements(achievements != null ? achievements.trim() : "");

        // Photo URL — supports Google Drive share links or any public image URL
        Integer photoCol = findColumn(columnMap,
                "photo", "photourl", "photo url", "imageurl", "image url",
                "driveurl", "drive url", "picture", "pictureurl",
                "profileimage", "profile image", "playerphoto", "player photo");
        String photoUrl = photoCol != null ? getStringCellValue(row, photoCol) : null;
        if (photoUrl != null && !photoUrl.isBlank()) {
            player.setProfileImagePath(convertGoogleDriveUrl(photoUrl.trim()));
        }

        return player;
    }

    /**
     * Converts a Google Drive share URL into a direct embeddable URL.
     *
     * Supported formats:
     *   https://drive.google.com/file/d/FILE_ID/view?usp=sharing  → direct view URL
     *   https://drive.google.com/open?id=FILE_ID                  → direct view URL
     *   https://drive.google.com/uc?id=FILE_ID                    → adds export=view
     *
     * Any non-Drive URL is returned as-is.
     */
    private String convertGoogleDriveUrl(String url) {
        if (url == null || url.isBlank()) return url;

        // Format: .../file/d/FILE_ID/view...
        if (url.contains("drive.google.com/file/d/")) {
            String fileId = url.replaceAll(".*/file/d/([^/?]+).*", "$1");
            if (!fileId.equals(url)) {
                return "https://drive.google.com/uc?export=view&id=" + fileId;
            }
        }
        // Format: .../open?id=FILE_ID
        if (url.contains("drive.google.com/open") && url.contains("id=")) {
            String fileId = url.replaceAll(".*[?&]id=([^&]+).*", "$1");
            if (!fileId.equals(url)) {
                return "https://drive.google.com/uc?export=view&id=" + fileId;
            }
        }
        // Format: .../uc?id=FILE_ID (missing export= param)
        if (url.contains("drive.google.com/uc") && url.contains("id=") && !url.contains("export=")) {
            return url.contains("?")
                    ? url.replace("drive.google.com/uc?", "drive.google.com/uc?export=view&")
                    : url + "?export=view";
        }
        return url;
    }

    private boolean isRowEmpty(Row row) {
        if (row == null) return true;
        for (Cell cell : row) {
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                String val = cell.toString().trim();
                if (!val.isEmpty()) return false;
            }
        }
        return true;
    }

    private String getStringCellValue(Row row, int colIndex) {
        Cell cell = row.getCell(colIndex, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) return null;

        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> null;
        };
    }

    private int getNumericCellValue(Row row, int colIndex) {
        Cell cell = row.getCell(colIndex, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) return 0;

        return switch (cell.getCellType()) {
            case NUMERIC -> (int) cell.getNumericCellValue();
            case STRING -> {
                try {
                    yield Integer.parseInt(cell.getStringCellValue().trim());
                } catch (NumberFormatException e) {
                    yield 0;
                }
            }
            default -> 0;
        };
    }

    private double getDoubleCellValue(Row row, int colIndex) {
        Cell cell = row.getCell(colIndex, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) return 0;

        return switch (cell.getCellType()) {
            case NUMERIC -> cell.getNumericCellValue();
            case STRING -> {
                try {
                    yield Double.parseDouble(cell.getStringCellValue().trim());
                } catch (NumberFormatException e) {
                    yield 0.0;
                }
            }
            default -> 0.0;
        };
    }

    public record ImportResult(int importedCount, List<String> errors) {
        public boolean hasErrors() {
            return errors != null && !errors.isEmpty();
        }
    }
}
