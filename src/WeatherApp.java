import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import org.json.JSONObject;
import org.json.JSONArray;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import java.util.Calendar;

public class WeatherApp {
    private static final int COLLAPSED_WIDTH = 70;
    private static final int EXPANDED_WIDTH = 250;
    private static final String[] menuItems = {"Home", "Hourly Forecast", "Monthly Forecast"};
    private static final String[] lightIconPaths = {
            "src/img/icons8-home-50.png",
            "src/img/icons8-clock-50.png",
            "src/img/icons8-calender-64.png"
    };
    private static final String[] darkIconPaths = {
            "src/img/icons8-home-50_02.png",
            "src/img/icons8-clock-50_02.png",
            "src/img/icons8-calender-64_02.png"
    };
    private static JLabel[] menuLabels;
    private static JPanel[] contentPanels;
    private static CardLayout cardLayout;
    private static final String API_KEY = "c8201d81f6926bc2a1635c53a18291e9";
    private static String currentLocation = "London";
    private static JLabel temperatureLabel;
    private static JLabel locationLabel;
    private static JLabel weatherIconLabel;
    private static JLabel descriptionLabel;
    private static JLabel feelsLikeLabel;
    private static JLabel humidityLabel;
    private static JLabel windLabel;
    private static JLabel pressureLabel;
    private static JLabel visibilityLabel;
    private static JLabel aqiLabel;
    private static JLabel dateLabel;
    private static JLabel timeLabel;
    private static JList<String> suggestionList;
    private static DefaultListModel<String> suggestionListModel;
    private static JPopupMenu suggestionPopup;
    private static JPanel forecastPanel;
    private static Timer timeUpdater;
    private static TimeZone currentTimeZone = TimeZone.getDefault();
    private static JPanel sunPanel;
    private static JLabel sunriseLabel;
    private static JLabel sunsetLabel;
    private static MSNWeatherAnimationPanel animationPanel;
    private static JPanel hourlyForecastPanel;
    private static TemperatureGraphPanel temperatureGraphPanel;
    private static MonthlyForecastPanel monthlyForecastPanel;

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(new FlatLightLaf());
        } catch (Exception e) {
            e.printStackTrace();
        }

        final boolean[] isDarkMode = {false};
        final boolean[] isMenuExpanded = {false};
        final JTextField[] searchField = new JTextField[1];

        JFrame frame = new JFrame("WeatherApp");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        frame.setMinimumSize(new Dimension(800, 600));

        // Create main content panel with CardLayout
        JPanel mainContentPanel = new JPanel();
        cardLayout = new CardLayout();
        mainContentPanel.setLayout(cardLayout);

        // Create content panels for each menu item
        contentPanels = new JPanel[menuItems.length];
        for (int i = 0; i < menuItems.length; i++) {
            contentPanels[i] = createContentPanel(menuItems[i], isDarkMode);
            mainContentPanel.add(contentPanels[i], menuItems[i]);
        }

        JPanel gradientPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                GradientPaint gp;
                if (isDarkMode[0]) {
                    gp = new GradientPaint(0, 0, Color.decode("#004769"), getWidth(), getHeight(), Color.decode("#000000"));
                } else {
                    gp = new GradientPaint(0, 0, Color.decode("#9abac7"), getWidth(), getHeight(), Color.decode("#d4dfed"));
                }
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        gradientPanel.setLayout(new BorderLayout());

        JPanel menuPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                GradientPaint gp;
                if (isDarkMode[0]) {
                    gp = new GradientPaint(0, 0, Color.decode("#1d3a47"), 0, getHeight(), Color.decode("#1d3a47"));
                } else {
                    gp = new GradientPaint(0, 0, Color.decode("#9abac7"), 0, getHeight(), Color.decode("#9abac7"));
                }
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        menuPanel.setPreferredSize(new Dimension(COLLAPSED_WIDTH, frame.getHeight()));
        menuPanel.setLayout(new BoxLayout(menuPanel, BoxLayout.Y_AXIS));
        menuPanel.setOpaque(false);

        ImageIcon toggleRaw = new ImageIcon("src/img/icons8-menu-button-50.png");
        Image toggleScaled = toggleRaw.getImage().getScaledInstance(24, 24, Image.SCALE_SMOOTH);
        JButton toggleButton = new JButton(new ImageIcon(toggleScaled));
        toggleButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        toggleButton.setBorder(BorderFactory.createEmptyBorder(15, (COLLAPSED_WIDTH - 24) / 2, 15, (COLLAPSED_WIDTH - 24) / 2));
        toggleButton.setContentAreaFilled(false);
        toggleButton.setFocusPainted(false);
        toggleButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        toggleButton.addActionListener(e -> {
            isMenuExpanded[0] = !isMenuExpanded[0];
            menuPanel.setPreferredSize(new Dimension(isMenuExpanded[0] ? EXPANDED_WIDTH : COLLAPSED_WIDTH, frame.getHeight()));
            frame.revalidate();
            updateMenuLabels(isMenuExpanded[0], isDarkMode[0]);
        });
        menuPanel.add(toggleButton);

        menuLabels = new JLabel[menuItems.length];
        for (int i = 0; i < menuItems.length; i++) {
            final int index = i;
            ImageIcon rawIcon = new ImageIcon(isDarkMode[0] ? darkIconPaths[index] : lightIconPaths[index]);
            Image scaled = rawIcon.getImage().getScaledInstance(24, 24, Image.SCALE_SMOOTH);
            ImageIcon icon = new ImageIcon(scaled);

            JLabel label = new JLabel("", icon, JLabel.LEFT);
            label.setFont(new Font("SansSerif", Font.PLAIN, 18));
            label.setBorder(BorderFactory.createEmptyBorder(25, 0, 15, 30));
            label.setOpaque(false);
            label.setForeground(isDarkMode[0] ? Color.WHITE : Color.BLACK);
            label.setToolTipText(menuItems[index]);

            label.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    label.setBackground(isDarkMode[0] ? new Color(0x575757) : new Color(230, 230, 230));
                    label.setOpaque(true);
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    label.setOpaque(false);
                }

                @Override
                public void mouseClicked(MouseEvent e) {
                    cardLayout.show(mainContentPanel, menuItems[index]);
                }
            });

            menuLabels[i] = label;
            menuPanel.add(label);
        }

        JPanel topBar = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                GradientPaint gp;
                if (isDarkMode[0]) {
                    gp = new GradientPaint(0, 0, Color.decode("#1d3a47"), 0, getHeight(), Color.decode("#1d3a47"));
                } else {
                    gp = new GradientPaint(0, 0, Color.decode("#9abac7"), 0, getHeight(), Color.decode("#d4dfed"));
                }
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        topBar.setPreferredSize(new Dimension(gradientPanel.getWidth(), 60));
        topBar.setLayout(new BorderLayout());
        topBar.setOpaque(false);

        ImageIcon rawIcon = new ImageIcon("src/img/icons8-forecast-80.png");
        Image scaledImage = rawIcon.getImage().getScaledInstance(32, 32, Image.SCALE_SMOOTH);
        ImageIcon scaledIcon = new ImageIcon(scaledImage);
        JLabel titleLabel = new JLabel(" Forecast", scaledIcon, JLabel.LEFT);
        titleLabel.setFont(new Font("Times New Roman", Font.BOLD, 24));
        titleLabel.setForeground(isDarkMode[0] ? Color.WHITE : Color.BLACK);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 10));
        titleLabel.setIconTextGap(10);
        topBar.add(titleLabel, BorderLayout.WEST);

        JPanel searchPanel = new JPanel(new BorderLayout());
        searchPanel.setOpaque(false);
        searchPanel.setBorder(BorderFactory.createEmptyBorder(15, 10, 15, 15));

        ImageIcon refreshRaw = new ImageIcon("src/img/icons8-refresh-40.png");
        Image refreshImg = refreshRaw.getImage().getScaledInstance(24, 24, Image.SCALE_SMOOTH);
        JButton refreshButton = new JButton(new ImageIcon(refreshImg));
        refreshButton.setToolTipText("Refresh");
        refreshButton.setFocusPainted(false);
        refreshButton.setContentAreaFilled(false);
        refreshButton.setBorderPainted(false);
        refreshButton.setOpaque(false);
        refreshButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        refreshButton.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 30));

        JButton modeButton = new JButton("<html><span style='font-size: 24pt'>🌙</span></html>");
        modeButton.setToolTipText("Toggle Dark/Light Mode");
        modeButton.setFocusPainted(false);
        modeButton.setContentAreaFilled(false);
        modeButton.setBorderPainted(false);
        modeButton.setOpaque(false);
        modeButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        modeButton.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 30));

        modeButton.addActionListener(e -> {
            try {
                if (isDarkMode[0]) {
                    UIManager.setLookAndFeel(new FlatLightLaf());
                    modeButton.setText("<html><span style='font-size: 24pt'>🌙</span></html>");
                } else {
                    UIManager.setLookAndFeel(new FlatDarkLaf());
                    modeButton.setText("<html><span style='font-size: 24pt'>☀️</span></html>");
                }
                isDarkMode[0] = !isDarkMode[0];
                titleLabel.setForeground(isDarkMode[0] ? Color.WHITE : Color.BLACK);
                updateMenuIcons(isDarkMode[0]);
                updateMenuLabels(isMenuExpanded[0], isDarkMode[0]);
                updateContentPanels(isDarkMode[0]);
                SwingUtilities.updateComponentTreeUI(frame);
                frame.repaint();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        refreshButton.addActionListener(e -> {
            fetchWeatherData(currentLocation, isDarkMode[0]);
            SwingUtilities.updateComponentTreeUI(frame);
            frame.repaint();
        });

        // Initialize suggestion components
        suggestionListModel = new DefaultListModel<>();
        suggestionList = new JList<>(suggestionListModel);
        suggestionList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        suggestionList.setVisibleRowCount(5);
        suggestionList.setBackground(isDarkMode[0] ? new Color(0x575757) : Color.WHITE);
        suggestionList.setForeground(isDarkMode[0] ? Color.WHITE : Color.BLACK);

        suggestionPopup = new JPopupMenu();
        suggestionPopup.add(new JScrollPane(suggestionList));
        suggestionPopup.setFocusable(false);

        searchField[0] = new JTextField("Search a Location") {
            @Override
            protected void paintComponent(Graphics g) {
                int arc = 30;
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
                g2.dispose();
                super.paintComponent(g);
            }

            @Override
            public void updateUI() {
                super.updateUI();
                setOpaque(false);
                setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 20));
            }
        };
        searchField[0].setForeground(Color.BLACK);
        searchField[0].setPreferredSize(new Dimension(200, 30));
        searchField[0].setFont(new Font("SansSerif", Font.PLAIN, 16));
        searchField[0].addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (searchField[0].getText().equals("Search a Location"))
                    searchField[0].setText("");
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (searchField[0].getText().isEmpty())
                    searchField[0].setText("Search a Location");
                suggestionPopup.setVisible(false);
            }
        });

        searchField[0].addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    String location = searchField[0].getText();
                    if (!location.isEmpty() && !location.equals("Search a Location")) {
                        currentLocation = location;
                        fetchWeatherData(location, isDarkMode[0]);
                        suggestionPopup.setVisible(false);
                    }
                    return;
                }

                if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                    if (suggestionPopup.isVisible() && suggestionListModel.size() > 0) {
                        suggestionList.requestFocus();
                        suggestionList.setSelectedIndex(0);
                    }
                    return;
                }

                String text = searchField[0].getText();
                if (text.length() >= 2 && !text.equals("Search a Location")) {
                    fetchLocationSuggestions(text, searchField[0], isDarkMode[0]);
                } else {
                    suggestionPopup.setVisible(false);
                }
            }
        });

        suggestionList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 1) {
                    String selectedLocation = suggestionList.getSelectedValue();
                    if (selectedLocation != null) {
                        searchField[0].setText(selectedLocation);
                        currentLocation = selectedLocation;
                        fetchWeatherData(selectedLocation, isDarkMode[0]);
                        suggestionPopup.setVisible(false);
                    }
                }
            }
        });

        searchField[0].addActionListener(e -> {
            String location = searchField[0].getText();
            if (!location.isEmpty() && !location.equals("Search a Location")) {
                currentLocation = location;
                fetchWeatherData(location, isDarkMode[0]);
                suggestionPopup.setVisible(false);
            }
        });

        JLabel searchIcon = new JLabel("\uD83D\uDD0D");
        searchIcon.setFont(new Font("SansSerif", Font.PLAIN, 25));
        searchIcon.setForeground(Color.GRAY);
        searchIcon.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));

        JPanel inputGroup = new JPanel(new BorderLayout());
        inputGroup.setOpaque(false);
        inputGroup.add(searchField[0], BorderLayout.CENTER);
        inputGroup.add(searchIcon, BorderLayout.EAST);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.add(refreshButton);
        buttonPanel.add(modeButton);

        searchPanel.add(buttonPanel, BorderLayout.WEST);
        searchPanel.add(inputGroup, BorderLayout.CENTER);
        topBar.add(searchPanel, BorderLayout.EAST);

        gradientPanel.add(topBar, BorderLayout.NORTH);
        gradientPanel.add(menuPanel, BorderLayout.WEST);
        gradientPanel.add(mainContentPanel, BorderLayout.CENTER);
        frame.add(gradientPanel);
        frame.setVisible(true);

        // Initialize time updater
        timeUpdater = new Timer(1000, e -> updateTime(isDarkMode[0]));
        timeUpdater.start();

        // Show home panel by default
        cardLayout.show(mainContentPanel, menuItems[0]);
        fetchWeatherData(currentLocation, isDarkMode[0]);
    }

    private static void updateTime(boolean isDarkMode) {
        if (timeLabel != null) {
            SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm:ss a");
            timeFormat.setTimeZone(currentTimeZone);
            Date now = new Date();
            timeLabel.setText(timeFormat.format(now));
            timeLabel.setForeground(isDarkMode ? Color.WHITE : Color.BLACK);

            // Update date as well in case the day changed
            SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, MMMM d, yyyy");
            dateFormat.setTimeZone(currentTimeZone);
            dateLabel.setText(dateFormat.format(now));
        }
    }

    private static void fetchLocationSuggestions(String query, JTextField searchField, boolean isDarkMode) {
        new Thread(() -> {
            try {
                String encodedQuery = URLEncoder.encode(query, "UTF-8");
                String url = "http://api.openweathermap.org/geo/1.0/direct?q=" + encodedQuery + "&limit=5&appid=" + API_KEY;
                String response = sendGetRequest(url);
                JSONArray suggestions = new JSONArray(response);

                List<String> locationNames = new ArrayList<>();
                for (int i = 0; i < suggestions.length(); i++) {
                    JSONObject location = suggestions.getJSONObject(i);
                    String name = location.getString("name");
                    String country = location.optString("country", "");
                    String state = location.optString("state", "");

                    StringBuilder sb = new StringBuilder(name);
                    if (!state.isEmpty()) {
                        sb.append(", ").append(state);
                    }
                    if (!country.isEmpty()) {
                        sb.append(", ").append(country);
                    }
                    locationNames.add(sb.toString());
                }

                SwingUtilities.invokeLater(() -> {
                    suggestionListModel.clear();
                    for (String location : locationNames) {
                        suggestionListModel.addElement(location);
                    }

                    if (suggestionListModel.size() > 0) {
                        suggestionList.setBackground(isDarkMode ? new Color(0x575757) : Color.WHITE);
                        suggestionList.setForeground(isDarkMode ? Color.WHITE : Color.BLACK);

                        suggestionPopup.setPreferredSize(new Dimension(
                                searchField.getWidth(),
                                Math.min(suggestionList.getPreferredScrollableViewportSize().height, 150)
                        ));
                        suggestionPopup.show(searchField, 0, searchField.getHeight());
                    } else {
                        suggestionPopup.setVisible(false);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                SwingUtilities.invokeLater(() -> {
                    suggestionPopup.setVisible(false);
                });
            }
        }).start();
    }

    private static JPanel createContentPanel(String title, boolean[] isDarkMode) {
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                GradientPaint gp;
                if (isDarkMode[0]) {
                    gp = new GradientPaint(0, 0, Color.decode("#1d3a47"), getWidth(), getHeight(), Color.decode("#7a3c3c"));
                } else {
                    gp = new GradientPaint(0, 0, Color.decode("#9abac7"), getWidth(), getHeight(), Color.decode("#d4dfed"));
                }
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        };

        if (title.equals("Home")) {
            panel.setLayout(new BorderLayout(10, 10));
            panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

            // Top panel for location and date
            JPanel topPanel = new JPanel(new BorderLayout());
            topPanel.setOpaque(false);

            // Panel for date and time
            JPanel dateTimePanel = new JPanel();
            dateTimePanel.setLayout(new BoxLayout(dateTimePanel, BoxLayout.Y_AXIS));
            dateTimePanel.setOpaque(false);

            dateLabel = new JLabel("", SwingConstants.RIGHT);
            dateLabel.setFont(new Font("SansSerif", Font.PLAIN, 16));
            dateLabel.setForeground(isDarkMode[0] ? Color.WHITE : Color.BLACK);

            timeLabel = new JLabel("", SwingConstants.RIGHT);
            timeLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
            timeLabel.setForeground(isDarkMode[0] ? Color.WHITE : Color.BLACK);

            dateTimePanel.add(dateLabel);
            dateTimePanel.add(timeLabel);

            locationLabel = new JLabel("", SwingConstants.LEFT);
            locationLabel.setFont(new Font("SansSerif", Font.BOLD, 24));
            locationLabel.setForeground(isDarkMode[0] ? Color.WHITE : Color.BLACK);

            topPanel.add(locationLabel, BorderLayout.WEST);
            topPanel.add(dateTimePanel, BorderLayout.EAST);

            // Center panel for main weather info
            JPanel centerPanel = new JPanel();
            centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
            centerPanel.setOpaque(false);

            JPanel tempPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 0));
            tempPanel.setOpaque(false);

            weatherIconLabel = new JLabel();
            weatherIconLabel.setPreferredSize(new Dimension(100, 100));

            temperatureLabel = new JLabel("", SwingConstants.LEFT);
            temperatureLabel.setFont(new Font("SansSerif", Font.BOLD, 48));
            temperatureLabel.setForeground(isDarkMode[0] ? Color.WHITE : Color.BLACK);

            descriptionLabel = new JLabel("", SwingConstants.LEFT);
            descriptionLabel.setFont(new Font("SansSerif", Font.PLAIN, 18));
            descriptionLabel.setForeground(isDarkMode[0] ? Color.WHITE : Color.BLACK);

            feelsLikeLabel = new JLabel("", SwingConstants.LEFT);
            feelsLikeLabel.setFont(new Font("SansSerif", Font.PLAIN, 16));
            feelsLikeLabel.setForeground(isDarkMode[0] ? Color.WHITE : Color.BLACK);

            JPanel descPanel = new JPanel();
            descPanel.setLayout(new BoxLayout(descPanel, BoxLayout.Y_AXIS));
            descPanel.setOpaque(false);
            descPanel.add(descriptionLabel);
            descPanel.add(feelsLikeLabel);

            // Create sun panel for sunrise/sunset info
            sunPanel = new JPanel();
            sunPanel.setLayout(new BoxLayout(sunPanel, BoxLayout.Y_AXIS));
            sunPanel.setOpaque(false);
            sunPanel.setBorder(BorderFactory.createEmptyBorder(0, 30, 0, 0));

            // Sunrise/sunset labels
            sunriseLabel = new JLabel();
            sunriseLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
            sunriseLabel.setForeground(isDarkMode[0] ? Color.WHITE : Color.BLACK);

            sunsetLabel = new JLabel();
            sunsetLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
            sunsetLabel.setForeground(isDarkMode[0] ? Color.WHITE : Color.BLACK);

            // MSN-style animation panel - now larger and more detailed
            animationPanel = new MSNWeatherAnimationPanel(isDarkMode[0]);
            animationPanel.setPreferredSize(new Dimension(300, 180)); // Increased size

            sunPanel.add(animationPanel);
            sunPanel.add(sunriseLabel);
            sunPanel.add(sunsetLabel);

            tempPanel.add(weatherIconLabel);
            tempPanel.add(temperatureLabel);
            tempPanel.add(descPanel);
            tempPanel.add(sunPanel);

            // Weather info panel (now placed just below temperature)
            JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 10));
            infoPanel.setOpaque(false);
            infoPanel.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0)); // Add some top padding

            humidityLabel = createWeatherInfoLabel("Humidity", "", isDarkMode[0]);
            windLabel = createWeatherInfoLabel("Wind", "", isDarkMode[0]);
            pressureLabel = createWeatherInfoLabel("Pressure", "", isDarkMode[0]);
            visibilityLabel = createWeatherInfoLabel("Visibility", "", isDarkMode[0]);
            aqiLabel = createWeatherInfoLabel("AQI", "", isDarkMode[0]);

            infoPanel.add(humidityLabel);
            infoPanel.add(windLabel);
            infoPanel.add(pressureLabel);
            infoPanel.add(visibilityLabel);
            infoPanel.add(aqiLabel);

            // Forecast panel
            forecastPanel = new JPanel();
            forecastPanel.setLayout(new BoxLayout(forecastPanel, BoxLayout.X_AXIS));
            forecastPanel.setOpaque(false);
            forecastPanel.setBorder(BorderFactory.createEmptyBorder(30, 0, 0, 0));

            // Add forecast title
            JLabel forecastTitle = new JLabel("5-Day Forecast");
            forecastTitle.setFont(new Font("SansSerif", Font.BOLD, 20));
            forecastTitle.setForeground(isDarkMode[0] ? Color.WHITE : Color.BLACK);
            forecastTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
            forecastTitle.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));

            // Combine all components in the center panel
            centerPanel.add(tempPanel);
            centerPanel.add(infoPanel);

            // Create a container for forecast components
            JPanel forecastContainer = new JPanel();
            forecastContainer.setLayout(new BoxLayout(forecastContainer, BoxLayout.Y_AXIS));
            forecastContainer.setOpaque(false);
            forecastContainer.add(forecastTitle);
            forecastContainer.add(forecastPanel);

            centerPanel.add(forecastContainer);

            panel.add(topPanel, BorderLayout.NORTH);
            panel.add(centerPanel, BorderLayout.CENTER);
        } else if (title.equals("Hourly Forecast")) {
            panel.setLayout(new BorderLayout());
            panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

            // Create title panel
            JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            titlePanel.setOpaque(false);

            JLabel titleLabel = new JLabel("Today's Hourly Forecast");
            titleLabel.setFont(new Font("SansSerif", Font.BOLD, 24));
            titleLabel.setForeground(isDarkMode[0] ? Color.WHITE : Color.BLACK);
            titlePanel.add(titleLabel);

            // Create the hourly forecast panel
            hourlyForecastPanel = new JPanel();
            hourlyForecastPanel.setLayout(new BoxLayout(hourlyForecastPanel, BoxLayout.X_AXIS));
            hourlyForecastPanel.setOpaque(false);
            hourlyForecastPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 20, 0));

            // Create the temperature graph panel
            temperatureGraphPanel = new TemperatureGraphPanel(isDarkMode[0]);
            temperatureGraphPanel.setPreferredSize(new Dimension(panel.getWidth(), 250));

            // Add components to main panel
            panel.add(titlePanel, BorderLayout.NORTH);
            panel.add(hourlyForecastPanel, BorderLayout.CENTER);
            panel.add(temperatureGraphPanel, BorderLayout.SOUTH);
        } else if (title.equals("Monthly Forecast")) {
            panel.setLayout(new BorderLayout());
            panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

            // Create title panel
            JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            titlePanel.setOpaque(false);

            JLabel titleLabel = new JLabel("6-Month Forecast");
            titleLabel.setFont(new Font("SansSerif", Font.BOLD, 24));
            titleLabel.setForeground(isDarkMode[0] ? Color.WHITE : Color.BLACK);
            titlePanel.add(titleLabel);

            // Create the monthly forecast panel
            monthlyForecastPanel = new MonthlyForecastPanel(isDarkMode[0]);
            monthlyForecastPanel.setPreferredSize(new Dimension(panel.getWidth(), panel.getHeight()));

            // Add components to main panel
            panel.add(titlePanel, BorderLayout.NORTH);
            panel.add(monthlyForecastPanel, BorderLayout.CENTER);
        }

        return panel;
    }

    private static JLabel createWeatherInfoLabel(String title, String value, boolean isDarkMode) {
        JLabel label = new JLabel("<html><b>" + title + "</b><br>" + value + "</html>", SwingConstants.LEFT);
        label.setFont(new Font("SansSerif", Font.PLAIN, 16));
        label.setForeground(isDarkMode ? Color.WHITE : Color.BLACK);
        label.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        label.setOpaque(false);
        return label;
    }

    private static void updateMenuLabels(boolean isExpanded, boolean isDarkMode) {
        for (int i = 0; i < menuLabels.length; i++) {
            if (isExpanded) {
                menuLabels[i].setText(" " + menuItems[i]);
                menuLabels[i].setBorder(BorderFactory.createEmptyBorder(25, 0, 15, 30));
            } else {
                menuLabels[i].setText("");
                menuLabels[i].setBorder(BorderFactory.createEmptyBorder(25, 0, 15, 30));
            }
            menuLabels[i].setForeground(isDarkMode ? Color.WHITE : Color.BLACK);
        }
    }

    private static void updateMenuIcons(boolean isDarkMode) {
        for (int i = 0; i < menuLabels.length; i++) {
            ImageIcon rawIcon = new ImageIcon(isDarkMode ? darkIconPaths[i] : lightIconPaths[i]);
            Image scaled = rawIcon.getImage().getScaledInstance(24, 24, Image.SCALE_SMOOTH);
            menuLabels[i].setIcon(new ImageIcon(scaled));
        }
    }

    private static void updateContentPanels(boolean isDarkMode) {
        for (JPanel panel : contentPanels) {
            // Update all labels in the panel
            Component[] components = panel.getComponents();
            for (Component component : components) {
                if (component instanceof JLabel) {
                    ((JLabel) component).setForeground(isDarkMode ? Color.WHITE : Color.BLACK);
                } else if (component instanceof JPanel) {
                    // Recursively update components in sub-panels
                    updatePanelComponents((JPanel) component, isDarkMode);
                }
            }
            panel.repaint();
        }

        if (animationPanel != null) {
            animationPanel.setDarkMode(isDarkMode);
            animationPanel.repaint();
        }

        if (temperatureGraphPanel != null) {
            temperatureGraphPanel.setDarkMode(isDarkMode);
            temperatureGraphPanel.repaint();
        }

        if (monthlyForecastPanel != null) {
            monthlyForecastPanel.setDarkMode(isDarkMode);
            monthlyForecastPanel.repaint();
        }
    }

    private static void updatePanelComponents(JPanel panel, boolean isDarkMode) {
        for (Component component : panel.getComponents()) {
            if (component instanceof JLabel) {
                ((JLabel) component).setForeground(isDarkMode ? Color.WHITE : Color.BLACK);
            } else if (component instanceof JPanel) {
                updatePanelComponents((JPanel) component, isDarkMode);
            }
        }
    }

    private static void fetchWeatherData(String location, boolean isDarkMode) {
        new Thread(() -> {
            try {
                // First, get coordinates for the location
                String encodedLocation = URLEncoder.encode(location, "UTF-8");
                String geoUrl = "http://api.openweathermap.org/geo/1.0/direct?q=" + encodedLocation + "&limit=1&appid=" + API_KEY;
                String geoResponse = sendGetRequest(geoUrl);
                JSONObject geoJson = new JSONObject("{\"data\":" + geoResponse + "}");

                if (geoJson.getJSONArray("data").length() == 0) {
                    JOptionPane.showMessageDialog(null, "Location not found", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                JSONObject locationData = geoJson.getJSONArray("data").getJSONObject(0);
                double lat = locationData.getDouble("lat");
                double lon = locationData.getDouble("lon");

                // Get weather data (which includes timezone information)
                String weatherUrl = "https://api.openweathermap.org/data/2.5/weather?lat=" + lat + "&lon=" + lon + "&appid=" + API_KEY + "&units=metric";
                String weatherResponse = sendGetRequest(weatherUrl);
                JSONObject weatherJson = new JSONObject(weatherResponse);

                // Set the timezone for the location
                int timezoneOffset = weatherJson.getInt("timezone");
                currentTimeZone = TimeZone.getTimeZone("GMT" + (timezoneOffset >= 0 ? "+" : "") + (timezoneOffset / 3600));

                // Get air pollution data
                String aqiUrl = "http://api.openweathermap.org/data/2.5/air_pollution?lat=" + lat + "&lon=" + lon + "&appid=" + API_KEY;
                String aqiResponse = sendGetRequest(aqiUrl);
                JSONObject aqiJson = new JSONObject(aqiResponse);

                // Get 5-day forecast data
                String forecastUrl = "https://api.openweathermap.org/data/2.5/forecast?lat=" + lat + "&lon=" + lon + "&appid=" + API_KEY + "&units=metric";
                String forecastResponse = sendGetRequest(forecastUrl);
                JSONObject forecastJson = new JSONObject(forecastResponse);

                // Get monthly forecast data (we'll use the 5-day forecast for demo purposes)
                String monthlyUrl = "https://api.openweathermap.org/data/2.5/forecast?lat=" + lat + "&lon=" + lon + "&appid=" + API_KEY + "&units=metric&cnt=40";
                String monthlyResponse = sendGetRequest(monthlyUrl);
                JSONObject monthlyJson = new JSONObject(monthlyResponse);

                // Update UI with weather data
                SwingUtilities.invokeLater(() -> {
                    updateWeatherUI(weatherJson, aqiJson, locationData, isDarkMode);
                    updateForecastUI(forecastJson, isDarkMode);
                    updateHourlyForecastUI(forecastJson, isDarkMode);
                    updateMonthlyForecastUI(monthlyJson, isDarkMode);
                });
            } catch (Exception e) {
                e.printStackTrace();
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(null, "Error fetching weather data", "Error", JOptionPane.ERROR_MESSAGE);
                });
            }
        }).start();
    }

    private static void updateMonthlyForecastUI(JSONObject monthlyJson, boolean isDarkMode) {
        if (monthlyForecastPanel != null) {
            monthlyForecastPanel.updateData(monthlyJson, isDarkMode);
        }
    }

    private static void updateHourlyForecastUI(JSONObject forecastJson, boolean isDarkMode) {
        if (hourlyForecastPanel == null) return;

        hourlyForecastPanel.removeAll();

        // Get today's date to filter hourly forecasts
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        dateFormat.setTimeZone(currentTimeZone);
        String today = dateFormat.format(new Date());

        JSONArray list = forecastJson.getJSONArray("list");
        List<JSONObject> hourlyForecasts = new ArrayList<>();

        // Get forecasts for today only
        for (int i = 0; i < list.length(); i++) {
            JSONObject forecast = list.getJSONObject(i);
            String dtText = forecast.getString("dt_txt");
            if (dtText.startsWith(today)) {
                hourlyForecasts.add(forecast);
            }
        }

        // Create hourly forecast cards
        for (int i = 0; i < hourlyForecasts.size(); i++) {
            JSONObject forecast = hourlyForecasts.get(i);
            JSONObject main = forecast.getJSONObject("main");
            JSONObject wind = forecast.getJSONObject("wind");

            // Create hourly forecast card
            JPanel hourPanel = new JPanel();
            hourPanel.setLayout(new BoxLayout(hourPanel, BoxLayout.Y_AXIS));
            hourPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            hourPanel.setOpaque(false);

            // Time label
            long dt = forecast.getLong("dt") * 1000;
            SimpleDateFormat timeFormat = new SimpleDateFormat("h a");
            timeFormat.setTimeZone(currentTimeZone);

            JLabel timeLabel = new JLabel(timeFormat.format(new Date(dt)));
            timeLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
            timeLabel.setForeground(isDarkMode ? Color.WHITE : Color.BLACK);
            timeLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

            // Weather icon
            String iconCode = forecast.getJSONArray("weather").getJSONObject(0).getString("icon");
            JLabel iconLabel = new JLabel();
            iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            try {
                ImageIcon icon = new ImageIcon(new URL("http://openweathermap.org/img/wn/" + iconCode + "@2x.png"));
                Image scaledIcon = icon.getImage().getScaledInstance(40, 40, Image.SCALE_SMOOTH);
                iconLabel.setIcon(new ImageIcon(scaledIcon));
            } catch (Exception e) {
                e.printStackTrace();
            }

            // Temperature
            double temp = main.getDouble("temp");
            JLabel tempLabel = new JLabel(String.format("%.0f°", temp));
            tempLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
            tempLabel.setForeground(isDarkMode ? Color.WHITE : Color.BLACK);
            tempLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

            // Wind information
            double windSpeed = wind.getDouble("speed");
            int windDeg = wind.optInt("deg", 0);
            JLabel windLabel = new JLabel(String.format("%.1fm/s %s", windSpeed, getWindDirection(windDeg)));
            windLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
            windLabel.setForeground(isDarkMode ? Color.WHITE : Color.BLACK);
            windLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

            // Humidity
            int humidity = main.getInt("humidity");
            JLabel humidityLabel = new JLabel(String.format("%d%% humidity", humidity));
            humidityLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
            humidityLabel.setForeground(isDarkMode ? Color.WHITE : Color.BLACK);
            humidityLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

            // Pressure
            int pressure = main.getInt("pressure");
            JLabel pressureLabel = new JLabel(String.format("%dhPa", pressure));
            pressureLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
            pressureLabel.setForeground(isDarkMode ? Color.WHITE : Color.BLACK);
            pressureLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

            // Add all components to hour panel
            hourPanel.add(timeLabel);
            hourPanel.add(Box.createRigidArea(new Dimension(0, 5)));
            hourPanel.add(iconLabel);
            hourPanel.add(Box.createRigidArea(new Dimension(0, 5)));
            hourPanel.add(tempLabel);
            hourPanel.add(Box.createRigidArea(new Dimension(0, 5)));
            hourPanel.add(windLabel);
            hourPanel.add(humidityLabel);
            hourPanel.add(pressureLabel);

            hourlyForecastPanel.add(hourPanel);

            // Add separator between hours (except after last hour)
            if (i < hourlyForecasts.size() - 1) {
                JSeparator separator = new JSeparator(JSeparator.VERTICAL);
                separator.setForeground(isDarkMode ? new Color(100, 100, 100) : new Color(200, 200, 200));
                hourlyForecastPanel.add(separator);
            }
        }

        hourlyForecastPanel.revalidate();
        hourlyForecastPanel.repaint();

        // Update temperature graph
        if (temperatureGraphPanel != null) {
            temperatureGraphPanel.updateData(hourlyForecasts, isDarkMode);
        }
    }

    private static void updateForecastUI(JSONObject forecastJson, boolean isDarkMode) {
        if (forecastPanel == null) return;

        forecastPanel.removeAll();

        // Group forecast data by day
        JSONArray list = forecastJson.getJSONArray("list");
        List<JSONObject> dailyForecasts = new ArrayList<>();

        // We'll take one forecast per day (at 12:00 PM for consistency)
        for (int i = 0; i < list.length(); i++) {
            JSONObject forecast = list.getJSONObject(i);
            String dtText = forecast.getString("dt_txt");
            if (dtText.contains("12:00:00")) {
                dailyForecasts.add(forecast);
                if (dailyForecasts.size() >= 5) break; // We only need 5 days
            }
        }

        // If we didn't get 5 days from 12:00 PM forecasts, add the remaining days
        if (dailyForecasts.size() < 5) {
            for (int i = 0; i < list.length() && dailyForecasts.size() < 5; i++) {
                JSONObject forecast = list.getJSONObject(i);
                if (!dailyForecasts.contains(forecast)) {
                    dailyForecasts.add(forecast);
                }
            }
        }

        // Create forecast cards for each day
        for (int i = 0; i < Math.min(dailyForecasts.size(), 5); i++) {
            JSONObject forecast = dailyForecasts.get(i);

            // Create forecast card panel
            JPanel dayPanel = new JPanel();
            dayPanel.setLayout(new BoxLayout(dayPanel, BoxLayout.Y_AXIS));
            dayPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            dayPanel.setOpaque(false);

            // Date label
            long dt = forecast.getLong("dt") * 1000;
            SimpleDateFormat dayFormat = new SimpleDateFormat("EEE");
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d");
            dayFormat.setTimeZone(currentTimeZone);
            dateFormat.setTimeZone(currentTimeZone);

            JLabel dayLabel = new JLabel(dayFormat.format(new Date(dt)));
            dayLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
            dayLabel.setForeground(isDarkMode ? Color.WHITE : Color.BLACK);
            dayLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

            JLabel dateLabel = new JLabel(dateFormat.format(new Date(dt)));
            dateLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
            dateLabel.setForeground(isDarkMode ? Color.WHITE : Color.BLACK);
            dateLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

            // Weather icon
            String iconCode = forecast.getJSONArray("weather").getJSONObject(0).getString("icon");
            JLabel iconLabel = new JLabel();
            iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            try {
                ImageIcon icon = new ImageIcon(new URL("http://openweathermap.org/img/wn/" + iconCode + "@2x.png"));
                Image scaledIcon = icon.getImage().getScaledInstance(50, 50, Image.SCALE_SMOOTH);
                iconLabel.setIcon(new ImageIcon(scaledIcon));
            } catch (Exception e) {
                e.printStackTrace();
            }

            // Temperature
            JSONObject main = forecast.getJSONObject("main");
            double temp = main.getDouble("temp");
            double tempMin = main.getDouble("temp_min");
            double tempMax = main.getDouble("temp_max");

            JLabel tempLabel = new JLabel(String.format("%.0f°C", temp));
            tempLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
            tempLabel.setForeground(isDarkMode ? Color.WHITE : Color.BLACK);
            tempLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

            JLabel tempRangeLabel = new JLabel(String.format("%.0f° / %.0f°", tempMin, tempMax));
            tempRangeLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
            tempRangeLabel.setForeground(isDarkMode ? Color.WHITE : Color.BLACK);
            tempRangeLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

            // Description
            String description = forecast.getJSONArray("weather").getJSONObject(0).getString("description");
            JLabel descLabel = new JLabel("<html><center>" + capitalize(description) + "</center></html>");
            descLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
            descLabel.setForeground(isDarkMode ? Color.WHITE : Color.BLACK);
            descLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

            // Add all components to day panel
            dayPanel.add(dayLabel);
            dayPanel.add(dateLabel);
            dayPanel.add(Box.createRigidArea(new Dimension(0, 5)));
            dayPanel.add(iconLabel);
            dayPanel.add(Box.createRigidArea(new Dimension(0, 5)));
            dayPanel.add(tempLabel);
            dayPanel.add(tempRangeLabel);
            dayPanel.add(Box.createRigidArea(new Dimension(0, 5)));
            dayPanel.add(descLabel);

            // Add day panel to forecast panel
            forecastPanel.add(dayPanel);

            // Add separator between days (except after last day)
            if (i < Math.min(dailyForecasts.size(), 5) - 1) {
                JSeparator separator = new JSeparator(JSeparator.VERTICAL);
                separator.setForeground(isDarkMode ? new Color(100, 100, 100) : new Color(200, 200, 200));
                forecastPanel.add(separator);
            }
        }

        forecastPanel.revalidate();
        forecastPanel.repaint();
    }

    private static void updateWeatherUI(JSONObject weatherJson, JSONObject aqiJson, JSONObject locationData, boolean isDarkMode) {
        // Update location
        String locationName = locationData.getString("name");
        String country = locationData.optString("country", "");
        locationLabel.setText(locationName + (country.isEmpty() ? "" : ", " + country));

        // Update date and time with the correct timezone
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, MMMM d, yyyy");
        dateFormat.setTimeZone(currentTimeZone);
        dateLabel.setText(dateFormat.format(new Date()));
        updateTime(isDarkMode);

        // Update temperature and weather icon
        double temp = weatherJson.getJSONObject("main").getDouble("temp");
        double feelsLike = weatherJson.getJSONObject("main").getDouble("feels_like");
        String weatherDescription = weatherJson.getJSONArray("weather").getJSONObject(0).getString("description");
        String iconCode = weatherJson.getJSONArray("weather").getJSONObject(0).getString("icon");

        temperatureLabel.setText(String.format("%.1f°C / %.1f°F", temp, celsiusToFahrenheit(temp)));
        descriptionLabel.setText(capitalize(weatherDescription));
        feelsLikeLabel.setText(String.format("Feels like %.1f°C / %.1f°F", feelsLike, celsiusToFahrenheit(feelsLike)));

        // Load weather icon
        try {
            ImageIcon icon = new ImageIcon(new URL("http://openweathermap.org/img/wn/" + iconCode + "@2x.png"));
            Image scaledIcon = icon.getImage().getScaledInstance(100, 100, Image.SCALE_SMOOTH);
            weatherIconLabel.setIcon(new ImageIcon(scaledIcon));
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Update other weather info
        int humidity = weatherJson.getJSONObject("main").getInt("humidity");
        double windSpeed = weatherJson.getJSONObject("wind").getDouble("speed");
        int windDeg = weatherJson.getJSONObject("wind").optInt("deg", 0);
        int pressure = weatherJson.getJSONObject("main").getInt("pressure");
        int visibility = weatherJson.optInt("visibility", 0) / 1000; // Convert to km

        humidityLabel.setText("<html><b>Humidity</b><br>" + humidity + "%</html>");
        windLabel.setText("<html><b>Wind</b><br>" + windSpeed + " m/s " + getWindDirection(windDeg) + "</html>");
        pressureLabel.setText("<html><b>Pressure</b><br>" + pressure + " hPa</html>");
        visibilityLabel.setText("<html><b>Visibility</b><br>" + (visibility > 0 ? visibility + " km" : "N/A") + "</html>");

        // Update AQI
        int aqi = aqiJson.getJSONArray("list").getJSONObject(0).getJSONObject("main").getInt("aqi");
        String aqiText = "";
        switch (aqi) {
            case 1: aqiText = "Good"; break;
            case 2: aqiText = "Fair"; break;
            case 3: aqiText = "Moderate"; break;
            case 4: aqiText = "Poor"; break;
            case 5: aqiText = "Very Poor"; break;
            default: aqiText = "N/A"; break;
        }
        aqiLabel.setText("<html><b>Air Quality</b><br>" + aqiText + "</html>");

        // Update sunrise/sunset information
        long sunrise = weatherJson.getJSONObject("sys").getLong("sunrise") * 1000;
        long sunset = weatherJson.getJSONObject("sys").getLong("sunset") * 1000;
        long currentTime = System.currentTimeMillis();

        SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a");
        timeFormat.setTimeZone(currentTimeZone);

        sunriseLabel.setText("Sunrise: " + timeFormat.format(new Date(sunrise)));
        sunsetLabel.setText("Sunset: " + timeFormat.format(new Date(sunset)));

        // Update MSN-style animation
        animationPanel.setTimes(sunrise, sunset, currentTime);
        animationPanel.repaint();
    }

    private static double celsiusToFahrenheit(double celsius) {
        return (celsius * 9/5) + 32;
    }

    private static String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    private static String getWindDirection(int degrees) {
        if (degrees >= 337.5 || degrees < 22.5) return "N";
        if (degrees >= 22.5 && degrees < 67.5) return "NE";
        if (degrees >= 67.5 && degrees < 112.5) return "E";
        if (degrees >= 112.5 && degrees < 157.5) return "SE";
        if (degrees >= 157.5 && degrees < 202.5) return "S";
        if (degrees >= 202.5 && degrees < 247.5) return "SW";
        if (degrees >= 247.5 && degrees < 292.5) return "W";
        if (degrees >= 292.5 && degrees < 337.5) return "NW";
        return "";
    }

    private static String sendGetRequest(String url) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("GET");

        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String inputLine;
        StringBuilder response = new StringBuilder();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        return response.toString();
    }

    static class MSNWeatherAnimationPanel extends JPanel {
        private long sunriseTime;
        private long sunsetTime;
        private long currentTime;
        private boolean isDarkMode;
        private Timer animationTimer;
        private float sunPosition; // 0.0 to 1.0 representing sunrise to sunset

        public MSNWeatherAnimationPanel(boolean isDarkMode) {
            this.isDarkMode = isDarkMode;
            setPreferredSize(new Dimension(300, 180)); // Larger size
            setOpaque(false);

            // Timer to update the animation periodically
            animationTimer = new Timer(60000, e -> {
                currentTime = System.currentTimeMillis();
                repaint();
            });
            animationTimer.start();
        }

        public void setTimes(long sunrise, long sunset, long current) {
            this.sunriseTime = sunrise;
            this.sunsetTime = sunset;
            this.currentTime = current;

            // Calculate sun position (0.0 at sunrise, 0.5 at noon, 1.0 at sunset)
            if (currentTime >= sunriseTime && currentTime <= sunsetTime) {
                long dayDuration = sunsetTime - sunriseTime;
                long timeSinceSunrise = currentTime - sunriseTime;
                sunPosition = (float)timeSinceSunrise / dayDuration;
            } else if (currentTime < sunriseTime) {
                sunPosition = 0.0f; // Before sunrise
            } else {
                sunPosition = 1.0f; // After sunset
            }
        }

        public void setDarkMode(boolean darkMode) {
            this.isDarkMode = darkMode;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            int width = getWidth();
            int height = getHeight();
            int arcHeight = height * 2 / 3;
            int horizonY = height * 3 / 4;

            // Draw the arc path (similar to MSN Weather)
            g2d.setColor(isDarkMode ? new Color(70, 70, 70) : new Color(200, 200, 200));
            g2d.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)); // Thicker line

            // Draw the arc (semi-circle)
            int arcWidth = width - 60; // More padding
            int arcX = (width - arcWidth) / 2;
            g2d.drawArc(arcX, horizonY - arcHeight, arcWidth, arcHeight * 2, 0, 180);

            // Draw sun/moon position indicator
            if (sunriseTime > 0 && sunsetTime > 0) {
                // Calculate position along the arc
                int sunSize = 24; // Larger sun/moon
                int sunX = arcX + (int)(sunPosition * arcWidth);
                int sunY = horizonY - (int)(Math.sin(sunPosition * Math.PI) * arcHeight);

                // Draw sun or moon
                if (currentTime >= sunriseTime && currentTime <= sunsetTime) {
                    // Draw sun with more detailed gradient
                    RadialGradientPaint sunGradient = new RadialGradientPaint(
                            sunX, sunY, sunSize,
                            new float[]{0.0f, 0.7f, 0.9f, 1.0f},
                            new Color[]{
                                    new Color(255, 255, 200, 255),
                                    new Color(255, 255, 150, 255),
                                    new Color(255, 230, 100, 255),
                                    new Color(255, 200, 50, 255)
                            }
                    );
                    g2d.setPaint(sunGradient);
                    g2d.fillOval(sunX - sunSize/2, sunY - sunSize/2, sunSize, sunSize);

                    // Sun glow effect with more layers
                    g2d.setColor(new Color(255, 255, 150, 50));
                    for (int i = 1; i <= 5; i++) {
                        int glowSize = sunSize + i * 6;
                        g2d.fillOval(sunX - glowSize/2, sunY - glowSize/2, glowSize, glowSize);
                    }
                } else {
                    // Draw moon with more detail
                    g2d.setColor(isDarkMode ? new Color(220, 220, 220) : new Color(180, 180, 180));
                    g2d.fillOval(sunX - sunSize/2, sunY - sunSize/2, sunSize, sunSize);

                    // Moon phase (crescent) with gradient
                    RadialGradientPaint moonGradient = new RadialGradientPaint(
                            sunX - sunSize/4, sunY - sunSize/2, sunSize,
                            new float[]{0.0f, 1.0f},
                            new Color[]{
                                    isDarkMode ? new Color(50, 50, 50) : new Color(230, 230, 230),
                                    isDarkMode ? new Color(30, 30, 30) : new Color(210, 210, 210)
                            }
                    );
                    g2d.setPaint(moonGradient);
                    g2d.fillOval(sunX - sunSize/4, sunY - sunSize/2, sunSize, sunSize);
                }
            }

            // Draw sunrise/sunset indicators
            int indicatorSize = 8; // Larger indicators
            g2d.setColor(isDarkMode ? new Color(150, 150, 150) : new Color(100, 100, 100));

            // Sunrise indicator (left)
            int sunriseX = arcX;
            int sunriseY = horizonY;
            g2d.fillOval(sunriseX - indicatorSize/2, sunriseY - indicatorSize/2, indicatorSize, indicatorSize);

            // Sunset indicator (right)
            int sunsetX = arcX + arcWidth;
            int sunsetY = horizonY;
            g2d.fillOval(sunsetX - indicatorSize/2, sunsetY - indicatorSize/2, indicatorSize, indicatorSize);

            // Draw current time indicator at bottom with more detail
            int timeIndicatorY = horizonY + 25;
            g2d.setColor(isDarkMode ? Color.WHITE : Color.BLACK);
            g2d.setStroke(new BasicStroke(2));

            // Add labels for sunrise/sunset at the bottom
            if (sunriseTime > 0 && sunsetTime > 0) {
                SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a");
                timeFormat.setTimeZone(currentTimeZone);

                g2d.setFont(new Font("SansSerif", Font.PLAIN, 12));
                g2d.drawString(timeFormat.format(new Date(sunriseTime)), arcX - 30, timeIndicatorY + 20);
                g2d.drawString(timeFormat.format(new Date(sunsetTime)), arcX + arcWidth - 30, timeIndicatorY + 20);
            }
        }
    }

    static class TemperatureGraphPanel extends JPanel {
        private List<JSONObject> hourlyData;
        private boolean isDarkMode;
        private Color graphColor;
        private Color gridColor;
        private Color textColor;
        private Color pointColor;
        private Color windColor;
        private Color humidityColor;
        private Color pressureColor;

        public TemperatureGraphPanel(boolean isDarkMode) {
            this.isDarkMode = isDarkMode;
            this.hourlyData = new ArrayList<>();
            setOpaque(false);
            updateColors();
        }

        public void setDarkMode(boolean darkMode) {
            this.isDarkMode = darkMode;
            updateColors();
            repaint();
        }

        private void updateColors() {
            if (isDarkMode) {
                graphColor = new Color(100, 180, 255);
                gridColor = new Color(70, 70, 70);
                textColor = Color.WHITE;
                pointColor = new Color(255, 255, 180);
                windColor = new Color(150, 255, 150);
                humidityColor = new Color(150, 200, 255);
                pressureColor = new Color(255, 150, 200);
            } else {
                graphColor = new Color(0, 100, 200);
                gridColor = new Color(200, 200, 200);
                textColor = Color.BLACK;
                pointColor = new Color(255, 200, 0);
                windColor = new Color(0, 150, 0);
                humidityColor = new Color(0, 100, 200);
                pressureColor = new Color(200, 0, 100);
            }
        }

        public void updateData(List<JSONObject> hourlyData, boolean isDarkMode) {
            this.hourlyData = hourlyData;
            this.isDarkMode = isDarkMode;
            updateColors();
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (hourlyData.isEmpty()) return;

            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            int width = getWidth();
            int height = getHeight();
            int padding = 40;
            int graphWidth = width - 2 * padding;
            int graphHeight = height - 2 * padding;

            // Find min and max values for scaling
            double minTemp = Double.MAX_VALUE;
            double maxTemp = Double.MIN_VALUE;
            double minWind = Double.MAX_VALUE;
            double maxWind = Double.MIN_VALUE;
            double minHumidity = Double.MAX_VALUE;
            double maxHumidity = Double.MIN_VALUE;
            double minPressure = Double.MAX_VALUE;
            double maxPressure = Double.MIN_VALUE;

            for (JSONObject forecast : hourlyData) {
                JSONObject main = forecast.getJSONObject("main");
                JSONObject wind = forecast.getJSONObject("wind");

                double temp = main.getDouble("temp");
                double windSpeed = wind.getDouble("speed");
                double humidity = main.getDouble("humidity");
                double pressure = main.getDouble("pressure");

                if (temp < minTemp) minTemp = temp;
                if (temp > maxTemp) maxTemp = temp;
                if (windSpeed < minWind) minWind = windSpeed;
                if (windSpeed > maxWind) maxWind = windSpeed;
                if (humidity < minHumidity) minHumidity = humidity;
                if (humidity > maxHumidity) maxHumidity = humidity;
                if (pressure < minPressure) minPressure = pressure;
                if (pressure > maxPressure) maxPressure = pressure;
            }

            // Add some padding to the ranges
            double tempRange = maxTemp - minTemp;
            minTemp = Math.floor(minTemp - 2);
            maxTemp = Math.ceil(maxTemp + 2);
            tempRange = maxTemp - minTemp;

            double windRange = maxWind - minWind;
            minWind = Math.floor(minWind - 1);
            maxWind = Math.ceil(maxWind + 1);
            windRange = maxWind - minWind;

            double humidityRange = maxHumidity - minHumidity;
            minHumidity = Math.floor(minHumidity - 5);
            maxHumidity = Math.ceil(maxHumidity + 5);
            humidityRange = maxHumidity - minHumidity;

            double pressureRange = maxPressure - minPressure;
            minPressure = Math.floor(minPressure - 5);
            maxPressure = Math.ceil(maxPressure + 5);
            pressureRange = maxPressure - minPressure;

            // Draw grid lines and labels
            g2d.setColor(gridColor);
            g2d.setStroke(new BasicStroke(1));

            // Horizontal grid lines (temperature)
            int numHorizontalLines = 5;
            for (int i = 0; i <= numHorizontalLines; i++) {
                double temp = maxTemp - (i * tempRange / numHorizontalLines);
                int y = padding + (int)(i * graphHeight / numHorizontalLines);

                // Draw grid line
                g2d.drawLine(padding, y, width - padding, y);

                // Draw temperature label
                g2d.setColor(textColor);
                g2d.drawString(String.format("%.0f°", temp), 5, y + 5);
                g2d.setColor(gridColor);
            }

            // Vertical grid lines (time)
            int numVerticalLines = hourlyData.size();
            for (int i = 0; i < numVerticalLines; i++) {
                int x = padding + (i * graphWidth / (numVerticalLines - 1));

                // Draw grid line
                g2d.drawLine(x, padding, x, height - padding);

                // Draw time label for every other hour to avoid clutter
                if (i % 2 == 0) {
                    JSONObject forecast = hourlyData.get(i);
                    long dt = forecast.getLong("dt") * 1000;
                    SimpleDateFormat timeFormat = new SimpleDateFormat("h a");
                    timeFormat.setTimeZone(currentTimeZone);
                    String timeText = timeFormat.format(new Date(dt));

                    g2d.setColor(textColor);
                    g2d.drawString(timeText, x - 15, height - padding + 20);
                    g2d.setColor(gridColor);
                }
            }

            // Draw the temperature line graph
            g2d.setColor(graphColor);
            g2d.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            // Calculate points for the line graph
            int[] xPoints = new int[hourlyData.size()];
            int[] yPoints = new int[hourlyData.size()];

            for (int i = 0; i < hourlyData.size(); i++) {
                JSONObject forecast = hourlyData.get(i);
                double temp = forecast.getJSONObject("main").getDouble("temp");

                xPoints[i] = padding + (i * graphWidth / (hourlyData.size() - 1));
                yPoints[i] = padding + (int)(((maxTemp - temp) / tempRange) * graphHeight);
            }

            // Draw the line connecting the points
            g2d.drawPolyline(xPoints, yPoints, hourlyData.size());

            // Draw points and temperature values
            g2d.setColor(pointColor);
            for (int i = 0; i < hourlyData.size(); i++) {
                // Draw circle at each point
                g2d.fillOval(xPoints[i] - 5, yPoints[i] - 5, 10, 10);

                // Draw temperature value above each point
                JSONObject forecast = hourlyData.get(i);
                double temp = forecast.getJSONObject("main").getDouble("temp");
                g2d.setColor(textColor);
                g2d.drawString(String.format("%.0f°", temp), xPoints[i] - 10, yPoints[i] - 10);
                g2d.setColor(pointColor);
            }

            // Draw wind speed graph
            g2d.setColor(windColor);
            g2d.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (int i = 0; i < hourlyData.size() - 1; i++) {
                JSONObject forecast1 = hourlyData.get(i);
                JSONObject forecast2 = hourlyData.get(i + 1);

                double wind1 = forecast1.getJSONObject("wind").getDouble("speed");
                double wind2 = forecast2.getJSONObject("wind").getDouble("speed");

                int x1 = xPoints[i];
                int y1 = padding + (int)(((maxWind - wind1) / windRange) * graphHeight);
                int x2 = xPoints[i + 1];
                int y2 = padding + (int)(((maxWind - wind2) / windRange) * graphHeight);

                g2d.drawLine(x1, y1, x2, y2);

                // Draw wind speed value
                if (i % 2 == 0) { // Only draw for every other point to avoid clutter
                    g2d.setColor(textColor);
                    g2d.drawString(String.format("%.1fm/s", wind1), x1 - 15, y1 - 10);
                    g2d.setColor(windColor);
                }
            }

            // Draw humidity graph
            g2d.setColor(humidityColor);
            g2d.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (int i = 0; i < hourlyData.size() - 1; i++) {
                JSONObject forecast1 = hourlyData.get(i);
                JSONObject forecast2 = hourlyData.get(i + 1);

                double humidity1 = forecast1.getJSONObject("main").getDouble("humidity");
                double humidity2 = forecast2.getJSONObject("main").getDouble("humidity");

                int x1 = xPoints[i];
                int y1 = padding + (int)(((maxHumidity - humidity1) / humidityRange) * graphHeight);
                int x2 = xPoints[i + 1];
                int y2 = padding + (int)(((maxHumidity - humidity2) / humidityRange) * graphHeight);

                g2d.drawLine(x1, y1, x2, y2);

                // Draw humidity value
                if (i % 2 == 1) { // Alternate with wind values
                    g2d.setColor(textColor);
                    g2d.drawString(String.format("%.0f%%", humidity1), x1 - 15, y1 - 10);
                    g2d.setColor(humidityColor);
                }
            }

            // Draw pressure graph
            g2d.setColor(pressureColor);
            g2d.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (int i = 0; i < hourlyData.size() - 1; i++) {
                JSONObject forecast1 = hourlyData.get(i);
                JSONObject forecast2 = hourlyData.get(i + 1);

                double pressure1 = forecast1.getJSONObject("main").getDouble("pressure");
                double pressure2 = forecast2.getJSONObject("main").getDouble("pressure");

                int x1 = xPoints[i];
                int y1 = padding + (int)(((maxPressure - pressure1) / pressureRange) * graphHeight);
                int x2 = xPoints[i + 1];
                int y2 = padding + (int)(((maxPressure - pressure2) / pressureRange) * graphHeight);

                g2d.drawLine(x1, y1, x2, y2);

                // Draw pressure value
                if (i % 3 == 0) { // Only draw for every third point
                    g2d.setColor(textColor);
                    g2d.drawString(String.format("%.0fhPa", pressure1), x1 - 20, y1 - 10);
                    g2d.setColor(pressureColor);
                }
            }

            // Draw graph title and legend
            g2d.setColor(textColor);
            g2d.setFont(new Font("SansSerif", Font.BOLD, 16));
            g2d.drawString("Hourly Weather Forecast Graph", width / 2 - 100, padding - 10);

            // Draw legend
            int legendX = width - 150;
            int legendY = padding - 20;

            g2d.setColor(graphColor);
            g2d.drawString("Temperature", legendX, legendY);

            g2d.setColor(windColor);
            g2d.drawString("Wind Speed", legendX, legendY + 15);

            g2d.setColor(humidityColor);
            g2d.drawString("Humidity", legendX, legendY + 30);

            g2d.setColor(pressureColor);
            g2d.drawString("Pressure", legendX, legendY + 45);
        }
    }

    static class MonthlyForecastPanel extends JPanel {
        private boolean isDarkMode;
        private Color textColor;
        private Color gridColor;
        private Color humidityColor;
        private Color pressureColor;
        private Color windColor;
        private Color pointColor;

        public MonthlyForecastPanel(boolean isDarkMode) {
            this.isDarkMode = isDarkMode;
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setOpaque(false);
            updateColors();
        }

        public void setDarkMode(boolean darkMode) {
            this.isDarkMode = darkMode;
            updateColors();
            repaint();
        }

        private void updateColors() {
            if (isDarkMode) {
                textColor = Color.WHITE;
                gridColor = new Color(70, 70, 70);
                humidityColor = new Color(100, 200, 255);
                pressureColor = new Color(200, 100, 255);
                windColor = new Color(100, 255, 150);
                pointColor = new Color(255, 255, 180);
            } else {
                textColor = Color.BLACK;
                gridColor = new Color(200, 200, 200);
                humidityColor = new Color(0, 100, 200);
                pressureColor = new Color(150, 0, 200);
                windColor = new Color(0, 150, 0);
                pointColor = new Color(255, 200, 0);
            }
        }

        public void updateData(JSONObject monthlyJson, boolean isDarkMode) {
            this.isDarkMode = isDarkMode;
            updateColors();

            removeAll();

            // Create a panel for the month cards
            JPanel monthCardsPanel = new JPanel();
            monthCardsPanel.setLayout(new GridLayout(2, 3, 20, 20));
            monthCardsPanel.setOpaque(false);
            monthCardsPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

            // Get current month and next 5 months
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeZone(currentTimeZone);

            for (int i = 0; i < 6; i++) {
                // Create month card
                JPanel monthCard = new JPanel();
                monthCard.setLayout(new BoxLayout(monthCard, BoxLayout.Y_AXIS));
                monthCard.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
           //     monthCard.setBackground(isDarkMode ? new Color(50, 50, 50, 150) : new Color(255, 255, 255, 150));

                // Month title
                SimpleDateFormat monthFormat = new SimpleDateFormat("MMMM yyyy");
                monthFormat.setTimeZone(currentTimeZone);
                String monthTitle = monthFormat.format(calendar.getTime());

                JLabel monthLabel = new JLabel(monthTitle);
                monthLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
                monthLabel.setForeground(textColor);
                monthLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

                // Weather icon (using a placeholder)
                JLabel iconLabel = new JLabel();
                iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
                try {
                    // For demo purposes, we'll use a generic icon
                    ImageIcon icon = new ImageIcon(new URL("http://openweathermap.org/img/wn/01d@2x.png"));
                    Image scaledIcon = icon.getImage().getScaledInstance(60, 60, Image.SCALE_SMOOTH);
                    iconLabel.setIcon(new ImageIcon(scaledIcon));
                } catch (Exception e) {
                    e.printStackTrace();
                }

                // Temperature (random for demo)
                double avgTemp = 15 + (Math.random() * 15); // Random between 15-30°C
                JLabel tempLabel = new JLabel(String.format("Avg: %.1f°C / %.1f°F", avgTemp, celsiusToFahrenheit(avgTemp)));
                tempLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
                tempLabel.setForeground(textColor);
                tempLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

                // Humidity (random for demo)
                int humidity = 50 + (int)(Math.random() * 40); // Random between 50-90%
                JLabel humidityLabel = new JLabel("Humidity: " + humidity + "%");
                humidityLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
                humidityLabel.setForeground(textColor);
                humidityLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

                // Pressure (random for demo)
                int pressure = 1000 + (int)(Math.random() * 20); // Random between 1000-1020 hPa
                JLabel pressureLabel = new JLabel("Pressure: " + pressure + " hPa");
                pressureLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
                pressureLabel.setForeground(textColor);
                pressureLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

                // Wind (random for demo)
                double windSpeed = 2 + (Math.random() * 8); // Random between 2-10 m/s
                int windDeg = (int)(Math.random() * 360); // Random direction
                JLabel windLabel = new JLabel("Wind: " + String.format("%.1f m/s %s", windSpeed, getWindDirection(windDeg)));
                windLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
                windLabel.setForeground(textColor);
                windLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

                // Add components to month card
                monthCard.add(monthLabel);
                monthCard.add(Box.createRigidArea(new Dimension(0, 10)));
                monthCard.add(iconLabel);
                monthCard.add(Box.createRigidArea(new Dimension(0, 10)));
                monthCard.add(tempLabel);
                monthCard.add(Box.createRigidArea(new Dimension(0, 5)));
                monthCard.add(humidityLabel);
                monthCard.add(pressureLabel);
                monthCard.add(windLabel);

                monthCardsPanel.add(monthCard);

                // Move to next month
                calendar.add(Calendar.MONTH, 1);
            }

            add(monthCardsPanel);

            // Create graphs panel
            JPanel graphsPanel = new JPanel();
            graphsPanel.setLayout(new GridLayout(1, 3, 20, 20));
            graphsPanel.setOpaque(false);
            graphsPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

            // Add humidity graph
            graphsPanel.add(createGraphPanel("Humidity Trend", humidityColor, 50, 90));

            // Add pressure graph
            graphsPanel.add(createGraphPanel("Pressure Trend", pressureColor, 1000, 1020));

            // Add wind speed graph
            graphsPanel.add(createGraphPanel("Wind Speed Trend", windColor, 2, 10));

            add(graphsPanel);

            revalidate();
            repaint();
        }

        private JPanel createGraphPanel(String title, Color graphColor, double minValue, double maxValue) {
            JPanel panel = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Graphics2D g2d = (Graphics2D) g;
                    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

                    int width = getWidth();
                    int height = getHeight();
                    int padding = 30;
                    int graphWidth = width - 2 * padding;
                    int graphHeight = height - 2 * padding;

                    // Draw grid
                    g2d.setColor(gridColor);
                    g2d.setStroke(new BasicStroke(1));

                    // Horizontal grid lines
                    int numHorizontalLines = 5;
                    for (int i = 0; i <= numHorizontalLines; i++) {
                        double value = maxValue - (i * (maxValue - minValue) / numHorizontalLines);
                        int y = padding + (int)(i * graphHeight / numHorizontalLines);

                        g2d.drawLine(padding, y, width - padding, y);

                        // Draw value label
                        g2d.setColor(textColor);
                        g2d.drawString(String.format("%.0f", value), 5, y + 5);
                        g2d.setColor(gridColor);
                    }

                    // Vertical grid lines (months)
                    int numMonths = 6;
                    for (int i = 0; i < numMonths; i++) {
                        int x = padding + (i * graphWidth / (numMonths - 1));
                        g2d.drawLine(x, padding, x, height - padding);

                        // Draw month abbreviation
                        Calendar cal = Calendar.getInstance();
                        cal.add(Calendar.MONTH, i);
                        SimpleDateFormat monthFormat = new SimpleDateFormat("MMM");
                        monthFormat.setTimeZone(currentTimeZone);
                        String monthText = monthFormat.format(cal.getTime());

                        g2d.setColor(textColor);
                        g2d.drawString(monthText, x - 10, height - padding + 15);
                        g2d.setColor(gridColor);
                    }

                    // Draw graph
                    g2d.setColor(graphColor);
                    g2d.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

                    // Generate random data points for the graph
                    int[] xPoints = new int[numMonths];
                    int[] yPoints = new int[numMonths];

                    for (int i = 0; i < numMonths; i++) {
                        xPoints[i] = padding + (i * graphWidth / (numMonths - 1));
                        double value = minValue + (Math.random() * (maxValue - minValue));
                        yPoints[i] = padding + (int)(((maxValue - value) / (maxValue - minValue)) * graphHeight);
                    }

                    // Draw line
                    g2d.drawPolyline(xPoints, yPoints, numMonths);

                    // Draw points
                    g2d.setColor(pointColor);
                    for (int i = 0; i < numMonths; i++) {
                        g2d.fillOval(xPoints[i] - 4, yPoints[i] - 4, 8, 8);
                    }

                    // Draw title
                    g2d.setColor(textColor);
                    g2d.setFont(new Font("SansSerif", Font.BOLD, 14));
                    g2d.drawString(title, width / 2 - 50, padding - 10);
                }
            };

            panel.setPreferredSize(new Dimension(300, 200));
            panel.setOpaque(false);

            return panel;
        }
    }
}