package com.dmmflipper;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.util.QuantityFormatter;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class DMMFlipperPanel extends PluginPanel
{
	private final DMMFlipperPlugin plugin;
	private final PriceApiClient priceApiClient;
	private final GEOfferTracker geOfferTracker;
	private final FlipHistory flipHistory;
	private final DMMFlipperConfig config;

	private final JPanel opportunitiesPanel;
	private final JPanel bulkOpportunitiesPanel;
	private final JPanel activeFlippingPanel;
	private final JPanel offersPanel;
	private final JPanel statsPanel;
	private final JButton refreshButton;
	private final JLabel sessionProfitLabel;
	private final JLabel totalProfitLabel;

	public DMMFlipperPanel(DMMFlipperPlugin plugin, PriceApiClient priceApiClient, 
						   GEOfferTracker geOfferTracker, FlipHistory flipHistory, DMMFlipperConfig config)
	{
		super(false);
		this.plugin = plugin;
		this.priceApiClient = priceApiClient;
		this.geOfferTracker = geOfferTracker;
		this.flipHistory = flipHistory;
		this.config = config;

		setLayout(new BorderLayout());
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		// Header
		JPanel headerPanel = new JPanel(new BorderLayout());
		headerPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		headerPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

		JPanel titlePanel = new JPanel(new GridLayout(3, 1));
		titlePanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);

		JLabel titleLabel = new JLabel("DMM Flipper");
		titleLabel.setForeground(Color.WHITE);
		titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
		
		sessionProfitLabel = new JLabel("Session: 0 gp");
		sessionProfitLabel.setForeground(Color.GREEN);
		sessionProfitLabel.setFont(new Font("Arial", Font.PLAIN, 11));
		
		totalProfitLabel = new JLabel("Total: 0 gp");
		totalProfitLabel.setForeground(Color.CYAN);
		totalProfitLabel.setFont(new Font("Arial", Font.PLAIN, 11));

		titlePanel.add(titleLabel);
		titlePanel.add(sessionProfitLabel);
		titlePanel.add(totalProfitLabel);

		headerPanel.add(titlePanel, BorderLayout.WEST);

		refreshButton = new JButton("Refresh");
		refreshButton.addActionListener(e -> 		private void refreshFlips()
		{
			SwingUtilities.invokeLater(() -> {
				refreshButton.setEnabled(false);
				refreshButton.setText("Loading...");
			});

			new Thread(() -> {
				try
				{
					List<FlipOpportunity> opportunities = priceApiClient.calculateOpportunities(
						config.minProfit(),
						config.minROI(),
						config.maxROI(),
						config.maxAge(),
						config.budget()
					);

					List<FlipOpportunity> activeFlippingOpportunities = priceApiClient.calculateActiveFlippingOpportunities(
						1,
						25000,
						config.maxAge(),
						config.budget()
					);

					List<FlipOpportunity> bulkOpportunities = priceApiClient.calculateBulkOpportunities(
						config.minProfit(),
						config.minROI(),
						config.maxROI(),
						config.maxAge(),
						config.budget(),
						1000
					);

					SwingUtilities.invokeLater(() -> {
						updateOpportunitiesDisplay(opportunities);
						updateActiveFlippingDisplay(activeFlippingOpportunities);
						updateBulkOpportunitiesDisplay(bulkOpportunities);
						private void updateActiveFlippingDisplay(List<FlipOpportunity> opportunities)
						{
							activeFlippingPanel.removeAll();

							if (opportunities.isEmpty())
							{
								JLabel noDataLabel = new JLabel("No active flipping opportunities found.");
								noDataLabel.setForeground(Color.LIGHT_GRAY);
								noDataLabel.setBorder(new EmptyBorder(10, 10, 10, 10));
								activeFlippingPanel.add(noDataLabel);
							}
							else
							{
								int count = Math.min(30, opportunities.size());
								for (int i = 0; i < count; i++)
								{
									FlipOpportunity opp = opportunities.get(i);
									activeFlippingPanel.add(createActiveFlippingPanel(opp));
								}
							}

							activeFlippingPanel.revalidate();
							activeFlippingPanel.repaint();
						}

						updateOfferDisplay();
						updateStatsDisplay();
						updateProfitLabels();
						refreshButton.setEnabled(true);
						refreshButton.setText("Refresh");
					});
				}
				catch (Exception e)
				{
					log.error("Error refreshing flips", e);
					SwingUtilities.invokeLater(() -> {
						refreshButton.setEnabled(true);
						refreshButton.setText("Refresh");
					});
				}
			}).start();
		}
);
		headerPanel.add(refreshButton, BorderLayout.EAST);

		add(headerPanel, BorderLayout.NORTH);

		// Main content with tabs
		JTabbedPane tabbedPane = new JTabbedPane();
		tabbedPane.setBackground(ColorScheme.DARK_GRAY_COLOR);

		// Opportunities tab
		opportunitiesPanel = new JPanel();
		opportunitiesPanel.setLayout(new BoxLayout(opportunitiesPanel, BoxLayout.Y_AXIS));
		opportunitiesPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		
		JScrollPane opportunitiesScroll = new JScrollPane(opportunitiesPanel);
		opportunitiesScroll.setBackground(ColorScheme.DARK_GRAY_COLOR);
		tabbedPane.addTab("Best Margin", opportunitiesScroll);

		// Active flipping tab
		activeFlippingPanel = new JPanel();
		activeFlippingPanel.setLayout(new BoxLayout(activeFlippingPanel, BoxLayout.Y_AXIS));
		activeFlippingPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		
		JScrollPane activeFlippingScroll = new JScrollPane(activeFlippingPanel);
		activeFlippingScroll.setBackground(ColorScheme.DARK_GRAY_COLOR);
		tabbedPane.addTab("Active Flip", activeFlippingScroll);

		// Bulk opportunities tab
		bulkOpportunitiesPanel = new JPanel();
		bulkOpportunitiesPanel.setLayout(new BoxLayout(bulkOpportunitiesPanel, BoxLayout.Y_AXIS));
		bulkOpportunitiesPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		
		JScrollPane bulkOpportunitiesScroll = new JScrollPane(bulkOpportunitiesPanel);
		bulkOpportunitiesScroll.setBackground(ColorScheme.DARK_GRAY_COLOR);
		tabbedPane.addTab("Bulk Volume", bulkOpportunitiesScroll);

		// Active offers tab
		offersPanel = new JPanel();
		offersPanel.setLayout(new BoxLayout(offersPanel, BoxLayout.Y_AXIS));
		offersPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		
		JScrollPane offersScroll = new JScrollPane(offersPanel);
		offersScroll.setBackground(ColorScheme.DARK_GRAY_COLOR);
		tabbedPane.addTab("Active Offers", offersScroll);

		// Statistics tab
		statsPanel = new JPanel();
		statsPanel.setLayout(new BoxLayout(statsPanel, BoxLayout.Y_AXIS));
		statsPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		
		JScrollPane statsScroll = new JScrollPane(statsPanel);
		statsScroll.setBackground(ColorScheme.DARK_GRAY_COLOR);
		tabbedPane.addTab("Statistics", statsScroll);

		add(tabbedPane, BorderLayout.CENTER);

		// Initial load
		refreshFlips();
		updateProfitLabels();
	}

	private void refreshFlips()
	{
		SwingUtilities.invokeLater(() -> {
			refreshButton.setEnabled(false);
			refreshButton.setText("Loading...");
		});

		new Thread(() -> {
			try
			{
				List<FlipOpportunity> opportunities = priceApiClient.calculateOpportunities(
					config.minProfit(),
					config.minROI(),
					config.maxROI(),
					config.maxAge(),
					config.budget()
				);
				
				List<FlipOpportunity> bulkOpportunities = priceApiClient.calculateBulkOpportunities(
					config.minProfit(),
					config.minROI(),
					config.maxROI(),
					config.maxAge(),
					config.budget(),
					1000  // Minimum limit of 1000 for bulk trading
				);

				SwingUtilities.invokeLater(() -> {
					updateOpportunitiesDisplay(opportunities);
					private void updateBulkOpportunitiesDisplay(List<FlipOpportunity> opportunities)
					{
						bulkOpportunitiesPanel.removeAll();

						if (opportunities.isEmpty())
						{
							JLabel noDataLabel = new JLabel("No bulk opportunities found.");
							noDataLabel.setForeground(Color.LIGHT_GRAY);
							noDataLabel.setBorder(new EmptyBorder(10, 10, 10, 10));
							bulkOpportunitiesPanel.add(noDataLabel);
						}
						else
						{
							int count = Math.min(20, opportunities.size());
							for (int i = 0; i < count; i++)
							{
								FlipOpportunity opp = opportunities.get(i);
								bulkOpportunitiesPanel.add(createBulkOpportunityPanel(opp));
								private JPanel createActiveFlippingPanel(FlipOpportunity opp)
								{
									JPanel panel = new JPanel();
									panel.setLayout(new BorderLayout());
									panel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
									panel.setBorder(new EmptyBorder(5, 5, 5, 5));

									JPanel infoPanel = new JPanel(new GridLayout(4, 1));
									infoPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);

									JLabel nameLabel = new JLabel(truncateName(opp.getItemName(), 25));
									nameLabel.setForeground(Color.WHITE);
									nameLabel.setFont(new Font("Arial", Font.BOLD, 12));

									int totalVolume = opp.getLowVolume() + opp.getHighVolume();
									JLabel profitLabel = new JLabel(String.format("Margin: %s gp | Vol: %s",
										QuantityFormatter.formatNumber(opp.getProfit()),
										QuantityFormatter.formatNumber(totalVolume)));
									profitLabel.setForeground(Color.GREEN);

									JLabel priceLabel = new JLabel(String.format("Buy: %s → Sell: %s",
										QuantityFormatter.formatNumber(opp.getBuyPrice()),
										QuantityFormatter.formatNumber(opp.getSellPrice())));
									priceLabel.setForeground(Color.CYAN);

									String freshness = opp.getAge() < 2 ? "FRESH" : opp.getAge() < 5 ? "Recent" : "Old";
									Color freshnessColor = opp.getAge() < 2 ? Color.GREEN : opp.getAge() < 5 ? Color.YELLOW : Color.ORANGE;

									JLabel detailLabel = new JLabel(String.format("Limit: %s | ROI: %.1f%% | %s (%dm)",
										QuantityFormatter.formatNumber(opp.getLimit()),
										opp.getRoi(),
										freshness,
										opp.getAge()));
									detailLabel.setForeground(freshnessColor);
									detailLabel.setFont(new Font("Arial", Font.PLAIN, 10));

									infoPanel.add(nameLabel);
									infoPanel.add(profitLabel);
									infoPanel.add(priceLabel);
									infoPanel.add(detailLabel);

									panel.add(infoPanel, BorderLayout.CENTER);

									return panel;
								}

								private JPanel createBulkOpportunityPanel(FlipOpportunity opp)
								{
									JPanel panel = new JPanel();
									panel.setLayout(new BorderLayout());
									panel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
									panel.setBorder(new EmptyBorder(5, 5, 5, 5));

									JPanel infoPanel = new JPanel(new GridLayout(4, 1));
									infoPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);

									JLabel nameLabel = new JLabel(truncateName(opp.getItemName(), 25));
									nameLabel.setForeground(Color.WHITE);
									nameLabel.setFont(new Font("Arial", Font.BOLD, 12));

									int perItemProfit = opp.getProfit() / opp.getLimit();
									JLabel profitLabel = new JLabel(String.format("Total: %s gp (%s ea)",
										QuantityFormatter.formatNumber(opp.getProfit()),
										QuantityFormatter.formatNumber(perItemProfit)));
									profitLabel.setForeground(Color.GREEN);

									JLabel priceLabel = new JLabel(String.format("Buy: %s → Sell: %s",
										QuantityFormatter.formatNumber(opp.getBuyPrice()),
										QuantityFormatter.formatNumber(opp.getSellPrice())));
									priceLabel.setForeground(Color.CYAN);

									JLabel detailLabel = new JLabel(String.format("Limit: %s | ROI: %.1f%% | Age: %dm",
										QuantityFormatter.formatNumber(opp.getLimit()),
										opp.getRoi(),
										opp.getAge()));
									detailLabel.setForeground(Color.LIGHT_GRAY);
									detailLabel.setFont(new Font("Arial", Font.PLAIN, 10));

									infoPanel.add(nameLabel);
									infoPanel.add(profitLabel);
									infoPanel.add(priceLabel);
									infoPanel.add(detailLabel);

									panel.add(infoPanel, BorderLayout.CENTER);

									return panel;
								}

							}
						}

						bulkOpportunitiesPanel.revalidate();
						bulkOpportunitiesPanel.repaint();
					}

					updateBulkOpportunitiesDisplay(bulkOpportunities);
					updateOfferDisplay();
					updateStatsDisplay();
					updateProfitLabels();
					refreshButton.setEnabled(true);
					refreshButton.setText("Refresh");
				});
			}
			catch (Exception e)
			{
				log.error("Error refreshing flips", e);
				SwingUtilities.invokeLater(() -> {
					refreshButton.setEnabled(true);
					refreshButton.setText("Refresh");
				});
			}
		}).start();
	}

	private void updateOpportunitiesDisplay(List<FlipOpportunity> opportunities)
	{
		opportunitiesPanel.removeAll();

		if (opportunities.isEmpty())
		{
			JLabel noDataLabel = new JLabel("No flips found. Check your filters.");
			noDataLabel.setForeground(Color.LIGHT_GRAY);
			noDataLabel.setBorder(new EmptyBorder(10, 10, 10, 10));
			opportunitiesPanel.add(noDataLabel);
		}
		else
		{
			// Show top 20 opportunities
			int count = Math.min(20, opportunities.size());
			for (int i = 0; i < count; i++)
			{
				FlipOpportunity opp = opportunities.get(i);
				opportunitiesPanel.add(createOpportunityPanel(opp));
			}
		}

		opportunitiesPanel.revalidate();
		opportunitiesPanel.repaint();
	}

	private JPanel createOpportunityPanel(FlipOpportunity opp)
	{
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		panel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		panel.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createMatteBorder(0, 0, 1, 0, ColorScheme.DARK_GRAY_COLOR),
			new EmptyBorder(8, 8, 8, 8)
		));

		// Item name
		JLabel nameLabel = new JLabel(truncateName(opp.getItemName(), 25));
		nameLabel.setForeground(Color.WHITE);
		nameLabel.setFont(new Font("Arial", Font.BOLD, 12));
		panel.add(nameLabel, BorderLayout.NORTH);

		// Details
		JPanel detailsPanel = new JPanel(new GridLayout(3, 2, 5, 2));
		detailsPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);

		addDetailLabel(detailsPanel, "Buy:", QuantityFormatter.quantityToStackSize(opp.getBuyPrice()) + " gp");
		addDetailLabel(detailsPanel, "Sell:", QuantityFormatter.quantityToStackSize(opp.getSellPrice()) + " gp");
		addDetailLabel(detailsPanel, "Profit:", QuantityFormatter.quantityToStackSize(opp.getProfit()) + " gp");
		addDetailLabel(detailsPanel, "ROI:", String.format("%.1f%%", opp.getRoi()));
		addDetailLabel(detailsPanel, "Limit:", String.valueOf(opp.getLimit()));
		addDetailLabel(detailsPanel, "Age:", opp.getAgeMinutes() + "m");

		panel.add(detailsPanel, BorderLayout.CENTER);

		return panel;
	}

	public void updateOfferDisplay()
	{
		SwingUtilities.invokeLater(() -> {
			offersPanel.removeAll();

			List<GEOfferTracker.TrackedOffer> offers = geOfferTracker.getActiveOffers();

			if (offers.isEmpty())
			{
				JLabel noOffersLabel = new JLabel("No active GE offers");
				noOffersLabel.setForeground(Color.LIGHT_GRAY);
				noOffersLabel.setBorder(new EmptyBorder(10, 10, 10, 10));
				offersPanel.add(noOffersLabel);
			}
			else
			{
				for (GEOfferTracker.TrackedOffer offer : offers)
				{
					offersPanel.add(createOfferPanel(offer));
				}
			}

			offersPanel.revalidate();
			offersPanel.repaint();
		});
	}

	private JPanel createOfferPanel(GEOfferTracker.TrackedOffer offer)
	{
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		panel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		panel.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createMatteBorder(0, 0, 1, 0, ColorScheme.DARK_GRAY_COLOR),
			new EmptyBorder(8, 8, 8, 8)
		));

		// Get item info
		ItemInfo itemInfo = priceApiClient.getItemInfo(offer.getItemId());
		String itemName = itemInfo != null ? itemInfo.getName() : "Item #" + offer.getItemId();

		// Header
		JLabel nameLabel = new JLabel(truncateName(itemName, 25));
		nameLabel.setForeground(Color.WHITE);
		nameLabel.setFont(new Font("Arial", Font.BOLD, 12));
		panel.add(nameLabel, BorderLayout.NORTH);

		// Details
		JPanel detailsPanel = new JPanel(new GridLayout(4, 2, 5, 2));
		detailsPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);

		String type = offer.isBuying() ? "Buying" : "Selling";
		addDetailLabel(detailsPanel, "Type:", type);
		addDetailLabel(detailsPanel, "Price:", QuantityFormatter.quantityToStackSize(offer.getPrice()) + " gp");
		addDetailLabel(detailsPanel, "Qty:", offer.getQuantityFilled() + "/" + offer.getQuantity());
		
		// Show slot timer
		long inactiveMinutes = offer.getInactiveTimeMinutes();
		String timerText = inactiveMinutes + "m inactive";
		Color timerColor = inactiveMinutes > 30 ? Color.ORANGE : Color.LIGHT_GRAY;
		JLabel timerLabel = new JLabel(timerText);
		timerLabel.setForeground(timerColor);
		timerLabel.setFont(new Font("Arial", Font.PLAIN, 11));
		detailsPanel.add(new JLabel("Timer:"));
		detailsPanel.add(timerLabel);

		// Check if stale
		boolean isStale = geOfferTracker.isOfferStale(offer, config.staleOfferThreshold());
		if (isStale)
		{
			PriceData currentPrice = priceApiClient.getPriceData(offer.getItemId());
			int relevantPrice = offer.isBuying() ? currentPrice.getLow() : currentPrice.getHigh();
			addDetailLabel(detailsPanel, "⚠ Current:", QuantityFormatter.quantityToStackSize(relevantPrice) + " gp");
			addDetailLabel(detailsPanel, "Status:", "STALE");
		}
		else
		{
			addDetailLabel(detailsPanel, "Status:", "Active");
		}

		panel.add(detailsPanel, BorderLayout.CENTER);

		// Highlight stale offers
		if (isStale)
		{
			panel.setBackground(new Color(80, 40, 40));
			detailsPanel.setBackground(new Color(80, 40, 40));
		}

		return panel;
	}

	private void updateStatsDisplay()
	{
		statsPanel.removeAll();

		// Session stats header
		JPanel sessionPanel = new JPanel(new GridLayout(4, 2, 5, 5));
		sessionPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		sessionPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

		addDetailLabel(sessionPanel, "Session Profit:", QuantityFormatter.quantityToStackSize(flipHistory.getSessionProfit()) + " gp");
		addDetailLabel(sessionPanel, "Total Profit:", QuantityFormatter.quantityToStackSize(flipHistory.getTotalProfit()) + " gp");
		addDetailLabel(sessionPanel, "Total Flips:", String.valueOf(flipHistory.getTotalFlips()));
		
		JButton resetButton = new JButton("Reset Session");
		resetButton.addActionListener(e -> {
			flipHistory.resetSession();
			updateProfitLabels();
			updateStatsDisplay();
		});
		sessionPanel.add(new JLabel(""));
		sessionPanel.add(resetButton);

		statsPanel.add(sessionPanel);

		// Recent flips
		JLabel recentLabel = new JLabel("Recent Flips");
		recentLabel.setForeground(Color.WHITE);
		recentLabel.setFont(new Font("Arial", Font.BOLD, 14));
		recentLabel.setBorder(new EmptyBorder(10, 10, 5, 10));
		statsPanel.add(recentLabel);

		// Get all flips and sort by timestamp
		List<FlipHistory.CompletedFlip> allFlips = new ArrayList<>();
		flipHistory.getCompletedFlips().values().forEach(allFlips::addAll);
		allFlips.sort((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));

		if (allFlips.isEmpty())
		{
			JLabel noFlipsLabel = new JLabel("No completed flips yet");
			noFlipsLabel.setForeground(Color.LIGHT_GRAY);
			noFlipsLabel.setBorder(new EmptyBorder(10, 10, 10, 10));
			statsPanel.add(noFlipsLabel);
		}
		else
		{
			// Show last 20 flips
			int count = Math.min(20, allFlips.size());
			for (int i = 0; i < count; i++)
			{
				statsPanel.add(createFlipPanel(allFlips.get(i)));
			}
		}

		statsPanel.revalidate();
		statsPanel.repaint();
	}

	private JPanel createFlipPanel(FlipHistory.CompletedFlip flip)
	{
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		panel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		panel.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createMatteBorder(0, 0, 1, 0, ColorScheme.DARK_GRAY_COLOR),
			new EmptyBorder(8, 8, 8, 8)
		));

		// Item name
		JLabel nameLabel = new JLabel(truncateName(flip.getItemName(), 25));
		nameLabel.setForeground(Color.WHITE);
		nameLabel.setFont(new Font("Arial", Font.BOLD, 12));
		panel.add(nameLabel, BorderLayout.NORTH);

		// Details
		JPanel detailsPanel = new JPanel(new GridLayout(4, 2, 5, 2));
		detailsPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);

		addDetailLabel(detailsPanel, "Buy:", QuantityFormatter.quantityToStackSize(flip.getBuyPrice()) + " gp");
		addDetailLabel(detailsPanel, "Sell:", QuantityFormatter.quantityToStackSize(flip.getSellPrice()) + " gp");
		addDetailLabel(detailsPanel, "Qty:", String.valueOf(flip.getQuantity()));
		
		// Profit with color
		JLabel profitKeyLabel = new JLabel("Profit:");
		profitKeyLabel.setForeground(Color.LIGHT_GRAY);
		profitKeyLabel.setFont(new Font("Arial", Font.PLAIN, 11));
		
		JLabel profitValueLabel = new JLabel(QuantityFormatter.quantityToStackSize(flip.getProfit()) + " gp");
		profitValueLabel.setForeground(flip.getProfit() > 0 ? Color.GREEN : Color.RED);
		profitValueLabel.setFont(new Font("Arial", Font.BOLD, 11));
		
		detailsPanel.add(profitKeyLabel);
		detailsPanel.add(profitValueLabel);

		panel.add(detailsPanel, BorderLayout.CENTER);

		return panel;
	}

	public void updateProfitLabels()
	{
		SwingUtilities.invokeLater(() -> {
			sessionProfitLabel.setText("Session: " + QuantityFormatter.quantityToStackSize(flipHistory.getSessionProfit()) + " gp");
			totalProfitLabel.setText("Total: " + QuantityFormatter.quantityToStackSize(flipHistory.getTotalProfit()) + " gp");
		});
	}

	private void addDetailLabel(JPanel panel, String label, String value)
	{
		JLabel keyLabel = new JLabel(label);
		keyLabel.setForeground(Color.LIGHT_GRAY);
		keyLabel.setFont(new Font("Arial", Font.PLAIN, 11));

		JLabel valueLabel = new JLabel(value);
		valueLabel.setForeground(Color.WHITE);
		valueLabel.setFont(new Font("Arial", Font.PLAIN, 11));

		panel.add(keyLabel);
		panel.add(valueLabel);
	}

	private String truncateName(String name, int maxLength)
	{
		if (name.length() <= maxLength)
		{
			return name;
		}
		return name.substring(0, maxLength - 2) + "..";
	}
}
