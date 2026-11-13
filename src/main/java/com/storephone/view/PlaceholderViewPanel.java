package com.storephone.view;

import java.awt.BorderLayout;
import java.awt.GridBagLayout;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

public class PlaceholderViewPanel extends JPanel {
    public PlaceholderViewPanel(String screenTitle, String phaseBadge, String description) {
        super(new GridBagLayout());
        setBackground(UiTheme.CONTENT_BG);

        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(UiTheme.CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UiTheme.BORDER),
                BorderFactory.createEmptyBorder(32, 44, 32, 44)
        ));

        JLabel badge = new JLabel(phaseBadge, SwingConstants.CENTER);
        badge.setFont(UiTheme.FONT_SECTION);
        badge.setForeground(UiTheme.PRIMARY);
        badge.setAlignmentX(CENTER_ALIGNMENT);

        JLabel title = new JLabel(screenTitle, SwingConstants.CENTER);
        title.setFont(UiTheme.FONT_TITLE);
        title.setForeground(UiTheme.TEXT_PRIMARY);
        title.setAlignmentX(CENTER_ALIGNMENT);

        JLabel desc = new JLabel("<html><div style='text-align:center;width:420px;'>" + description + "</div></html>", SwingConstants.CENTER);
        desc.setFont(UiTheme.FONT_BODY);
        desc.setForeground(UiTheme.TEXT_MUTED);
        desc.setAlignmentX(CENTER_ALIGNMENT);

        card.add(badge);
        card.add(Box.createVerticalStrut(10));
        card.add(title);
        card.add(Box.createVerticalStrut(12));
        card.add(desc);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(card, BorderLayout.CENTER);
        add(wrapper);
    }
}
