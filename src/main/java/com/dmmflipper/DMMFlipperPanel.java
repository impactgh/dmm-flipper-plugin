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
	private final GEOfferTracker geOfferTracker;
	private final FlipHistory flipHistory;
	private final PriceApiClient priceApiClient;

	private final JPanel offersPanel;
	private final JButton refreshButton;
	private final JLabel sessionProfitLabel;
	private final JLabel totalProfitLabel;

	public DMMFlipperPanel(DMMFlipperPlugin plugin, PriceApiClient priceApiClient, 
						   GEOfferTracker geOfferTracker, FlipHistory flipHistory)
	{
		super(false);
		this.plugin = plugin;
		this.priceApiClient = priceApiClient;
		this.geOfferTracker = geOfferTracker;
		this.flipHistory = flipHistory;

		setLayout(new BorderLayout());
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		// Header
		JPanel headerPanel = new JPanel(new BorderLayout());
		headerPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		headerPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

		JPanel titlePanel = new JPanel(new GridLayout(3, 1));
		titlePanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);

		JLabel titleLabel = new JLabel("DMM Flipper - Active Offers");
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
		refreshButton.addActionListener(e -> updateOfferDisplay());
		headerPanel.add(refreshButton, BorderLayout.EAST);

		add(headerPanel, BorderLayout.NORTH);

		// Main content - just active offers
		JPanel offersPanel = new JPanel();
		offersPanel.setLayout(new BoxLayout(offersPanel, BoxLayout.Y_AXIS));
		offersPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		
		JScrollPane offersScroll = new JScrollPane(offersPanel);
		offersScroll.setBackground(ColorScheme.DARK_GRAY_COLOR);

		add(offersScroll, BorderLayout.CENTER);

		// Store reference for updates
		this.offersPanel = offersPanel;

		// Initial load
		updateOfferDisplay();
		updateProfitLabels();
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
		panel.setBorder(new EmptyBorder(5, 5, 5, 5));

		ItemInfo itemInfo = priceApiClient.getItemInfo(offer.getItemId());
		String itemName = itemInfo != null ? itemInfo.getName() : "Item #" + offer.getItemId();

		JPanel infoPanel = new JPanel(new GridLayout(4, 1));
		infoPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);

		JLabel nameLabel = new JLabel(truncateName(itemName, 25));
		nameLabel.setForeground(Color.WHITE);
		nameLabel.setFont(new Font("Arial", Font.BOLD, 12));

		String type = offer.isBuying() ? "Buying" : "Selling";
		JLabel typeLabel = new JLabel(String.format("%s @ %s gp", 
			type,
			QuantityFormatter.formatNumber(offer.getPrice())));
		typeLabel.setForeground(offer.isBuying() ? Color.CYAN : Color.GREEN);

		JLabel qtyLabel = new JLabel(String.format("Qty: %d / %d", 
			offer.getQuantityFilled(),
			offer.getQuantity()));
		qtyLabel.setForeground(Color.LIGHT_GRAY);

		long ageSeconds = (System.currentTimeMillis() - offer.getTimestamp()) / 1000;
		JLabel ageLabel = new JLabel(String.format("Age: %dm", ageSeconds / 60));
		ageLabel.setForeground(Color.LIGHT_GRAY);
		ageLabel.setFont(new Font("Arial", Font.PLAIN, 10));

		infoPanel.add(nameLabel);
		infoPanel.add(typeLabel);
		infoPanel.add(qtyLabel);
		infoPanel.add(ageLabel);

		panel.add(infoPanel, BorderLayout.CENTER);

		return panel;
	}


	private JPanel createFlipPanel(FlipHistory.CompletedFlip flip)
	{
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		panel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		panel.setBorder(new EmptyBorder(5, 5, 5, 5));

		JPanel infoPanel = new JPanel(new GridLayout(3, 1));
		infoPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);

		JLabel nameLabel = new JLabel(truncateName(flip.getItemName(), 25));
		nameLabel.setForeground(Color.WHITE);
		nameLabel.setFont(new Font("Arial", Font.BOLD, 12));

		JLabel profitLabel = new JLabel(String.format("Profit: %s gp", 
			QuantityFormatter.formatNumber(flip.getProfit())));
		profitLabel.setForeground(flip.getProfit() > 0 ? Color.GREEN : Color.RED);

		JLabel detailLabel = new JLabel(String.format("Buy: %s | Sell: %s | Qty: %d",
			QuantityFormatter.formatNumber(flip.getBuyPrice()),
			QuantityFormatter.formatNumber(flip.getSellPrice()),
			flip.getQuantity()));
		detailLabel.setForeground(Color.LIGHT_GRAY);
		detailLabel.setFont(new Font("Arial", Font.PLAIN, 10));

		infoPanel.add(nameLabel);
		infoPanel.add(profitLabel);
		infoPanel.add(detailLabel);

		panel.add(infoPanel, BorderLayout.CENTER);

		return panel;
	}

	public void updateProfitLabels()
	{
		int sessionProfit = flipHistory.getSessionProfit();
		int totalProfit = flipHistory.getTotalProfit();

		sessionProfitLabel.setText(String.format("Session: %s gp", QuantityFormatter.formatNumber(sessionProfit)));
		totalProfitLabel.setText(String.format("Total: %s gp", QuantityFormatter.formatNumber(totalProfit)));
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
