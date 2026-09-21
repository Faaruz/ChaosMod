package com.chaosmod;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.util.List;
import javax.inject.Inject;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;

/** Sidebar controls for manually testing every registered stream event. */
final class ChaosModPanel extends PluginPanel
{
	private static final int GAP = 6;

	private final ChaosModPlugin plugin;
	private final JPanel eventButtons = new JPanel();

	@Inject
	ChaosModPanel(ChaosModPlugin plugin)
	{
		this.plugin = plugin;
		setLayout(new BorderLayout());
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		JPanel content = new JPanel();
		content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
		content.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		content.setBackground(ColorScheme.DARK_GRAY_COLOR);

		JLabel title = new JLabel("Chaos Mod Event Tester", SwingConstants.CENTER);
		title.setAlignmentX(Component.CENTER_ALIGNMENT);
		title.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
		content.add(title);
		content.add(Box.createVerticalStrut(GAP));

		JLabel help = new JLabel("<html><center>Start any event immediately.<br>"
			+ "The current countdown or event will stop.</center></html>", SwingConstants.CENTER);
		help.setAlignmentX(Component.CENTER_ALIGNMENT);
		help.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
		content.add(help);
		content.add(Box.createVerticalStrut(10));

		eventButtons.setLayout(new BoxLayout(eventButtons, BoxLayout.Y_AXIS));
		eventButtons.setBackground(ColorScheme.DARK_GRAY_COLOR);
		content.add(eventButtons);
		content.add(Box.createVerticalStrut(12));

		JButton stopButton = createButton("Stop active event");
		stopButton.addActionListener(event -> plugin.stopTestEvent());
		content.add(stopButton);

		add(content, BorderLayout.NORTH);
	}

	void rebuildEventButtons()
	{
		eventButtons.removeAll();
		List<String> eventNames = plugin.getAvailableEventNames();
		for (String eventName : eventNames)
		{
			JButton button = createButton(eventName);
			button.addActionListener(event -> plugin.startTestEvent(eventName));
			eventButtons.add(button);
			eventButtons.add(Box.createVerticalStrut(GAP));
		}
		eventButtons.revalidate();
		eventButtons.repaint();
	}

	private static JButton createButton(String text)
	{
		JButton button = new JButton(text);
		button.setAlignmentX(Component.CENTER_ALIGNMENT);
		button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
		button.setFocusable(false);
		return button;
	}
}
