package com.storephone;

import com.storephone.config.AppContext;
import com.storephone.config.DatabaseConfig;
import com.storephone.view.MainFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        System.setProperty("apple.laf.useScreenMenuBar", "true");
        SwingUtilities.invokeLater(() -> {
            installLookAndFeel();
            DatabaseConfig dbConfig = DatabaseConfig.load();
            AppContext context = new AppContext(dbConfig);
            MainFrame mainFrame = new MainFrame(context);
            mainFrame.setVisible(true);
        });
    }

    private static void installLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception ignored) {
            // Giữ LookAndFeel mặc định nếu có lỗi
        }
    }
}
