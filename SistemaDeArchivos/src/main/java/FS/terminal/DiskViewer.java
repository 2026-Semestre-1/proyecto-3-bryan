package FS.terminal;

import FS.principal.*;
import FS.structures.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;

public class DiskViewer extends JFrame {

    private FileSystem fs;
    private JPanel gridPanel;
    private JPanel compactPanel;
    private JLabel statusLabel;
    private Map<Integer, String> blockOwners;
    private javax.swing.Timer refreshTimer;

    private static final int CELL_SIZE = 14;
    private static final int CELLS_PER_ROW = 80;
    private static final int COMPACT_HEIGHT = 50;
    private static final int COMPACT_WIDTH = 900;

    public DiskViewer(FileSystem fs) {
        this.fs = fs;
        setTitle("POLAR — Visor de Disco");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1200, 750);
        setLocationRelativeTo(null);
        initUI();
        refresh();
        startAutoRefresh();
    }

    private void initUI() {
        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        topBar.setBackground(Color.BLACK);

        JLabel title = new JLabel("VISOR DE DISCO");
        title.setForeground(new Color(0, 255, 0));
        title.setFont(new Font("Consolas", Font.BOLD, 16));
        topBar.add(title);

        topBar.add(legendLabel("  Libre", Color.GREEN));
        topBar.add(legendLabel("  Ocupado", Color.RED));
        topBar.add(legendLabel("  Metadatos", new Color(100, 100, 100)));
        topBar.add(legendLabel("  Abierto", Color.ORANGE));

        JButton refreshBtn = new JButton("Refrescar");
        refreshBtn.setBackground(Color.DARK_GRAY);
        refreshBtn.setForeground(Color.WHITE);
        refreshBtn.setFocusPainted(false);
        refreshBtn.addActionListener(e -> refresh());
        topBar.add(refreshBtn);

        add(topBar, BorderLayout.NORTH);

        JPanel mainContent = new JPanel(new BorderLayout());
        mainContent.setBackground(Color.BLACK);

        compactPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                paintCompactBar(g);
            }
        };
        compactPanel.setPreferredSize(new Dimension(COMPACT_WIDTH, COMPACT_HEIGHT + 30));
        compactPanel.setBackground(Color.BLACK);
        mainContent.add(compactPanel, BorderLayout.NORTH);

        gridPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                paintGrid(g);
            }
        };
        gridPanel.setBackground(Color.BLACK);
        gridPanel.setPreferredSize(computeGridSize());

        gridPanel.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                handleTooltip(e);
            }
        });

        JScrollPane scroll = new JScrollPane(gridPanel);
        scroll.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.GRAY), "Zona de Datos (bloques)",
                javax.swing.border.TitledBorder.LEFT, javax.swing.border.TitledBorder.TOP,
                new Font("Consolas", Font.PLAIN, 11), Color.LIGHT_GRAY));
        scroll.getViewport().setBackground(Color.BLACK);
        mainContent.add(scroll, BorderLayout.CENTER);

        add(mainContent, BorderLayout.CENTER);

        statusLabel = new JLabel(" ");
        statusLabel.setForeground(Color.WHITE);
        statusLabel.setBackground(new Color(40, 40, 40));
        statusLabel.setOpaque(true);
        statusLabel.setFont(new Font("Consolas", Font.PLAIN, 12));
        add(statusLabel, BorderLayout.PAGE_END);
    }

    private JLabel legendLabel(String text, Color c) {
        JLabel l = new JLabel("\u25A0 " + text);
        l.setForeground(c);
        l.setFont(new Font("Consolas", Font.PLAIN, 12));
        return l;
    }

    private Dimension computeGridSize() {
        int total = (int) fs.superBlock.getTotalBlocks();
        int rows = (int) Math.ceil((double) total / CELLS_PER_ROW);
        int w = CELLS_PER_ROW * CELL_SIZE + 55;
        int h = Math.max(rows * CELL_SIZE + 15, 300);
        return new Dimension(w, h);
    }

    private void paintCompactBar(Graphics g) {
        int x0 = 20;
        int y0 = 8;
        int h = COMPACT_HEIGHT;
        int metaW = 55;

        Font f = new Font("Consolas", Font.BOLD, 10);
        g.setFont(f);

        Color[] metaColors = {
            new Color(80, 80, 80), new Color(100, 100, 100),
            new Color(70, 70, 70), new Color(110, 110, 110),
            new Color(90, 90, 90), new Color(100, 100, 100),
            new Color(75, 75, 75)
        };
        String[] metaLabels = {"MBR", "SB", "BMP", "OF", "USR", "GRP", "FCB"};
        int x = x0;
        for (int i = 0; i < metaLabels.length; i++) {
            g.setColor(metaColors[i]);
            g.fillRect(x, y0, metaW, h);
            g.setColor(Color.WHITE);
            g.drawString(metaLabels[i], x + (metaW - g.getFontMetrics().stringWidth(metaLabels[i])) / 2, y0 + h / 2 + 3);
            g.setColor(Color.DARK_GRAY);
            g.drawRect(x, y0, metaW, h);
            x += metaW;
        }

        int dataW = COMPACT_WIDTH - x - 20;
        if (dataW > 0) {
            g.setColor(new Color(0, 100, 0));
            g.fillRect(x, y0, dataW, h);
            g.setColor(Color.WHITE);
            String dataLabel = "ZONA DE DATOS";
            g.drawString(dataLabel, x + (dataW - g.getFontMetrics().stringWidth(dataLabel)) / 2, y0 + h / 2 + 3);
            g.setColor(Color.DARK_GRAY);
            g.drawRect(x, y0, dataW, h);
        }

        g.setColor(Color.GRAY);
        g.setFont(new Font("Consolas", Font.PLAIN, 9));
        g.drawString("Metadatos", x0 + 5, y0 + h + 14);
        g.drawString("Zona de Datos (bloques)", x + 5, y0 + h + 14);

        g.setColor(Color.DARK_GRAY);
        g.drawLine(x0, y0 + h + 20, COMPACT_WIDTH - 20, y0 + h + 20);
    }

    private void paintGrid(Graphics g) {
        int total = (int) fs.superBlock.getTotalBlocks();
        int rows = (int) Math.ceil((double) total / CELLS_PER_ROW);

        g.setFont(new Font("Consolas", Font.PLAIN, 9));
        FontMetrics fm = g.getFontMetrics();

        for (int i = 0; i < total; i++) {
            int row = i / CELLS_PER_ROW;
            int col = i % CELLS_PER_ROW;
            int x = col * CELL_SIZE + 50;
            int y = row * CELL_SIZE + 10;

            boolean busy = fs.bitmapBlocks.isBusy(i);
            if (busy) {
                String owner = blockOwners != null ? blockOwners.get(i) : null;
                if (owner != null && owner.startsWith("\u2605")) {
                    g.setColor(Color.ORANGE);
                } else {
                    g.setColor(Color.RED);
                }
            } else {
                g.setColor(Color.GREEN);
            }
            g.fillRect(x, y, CELL_SIZE - 1, CELL_SIZE - 1);
        }

        g.setColor(Color.GRAY);
        for (int r = 0; r < rows; r++) {
            int y = r * CELL_SIZE + 10 + CELL_SIZE / 2 + 3;
            g.drawString(String.format("%04d", r * CELLS_PER_ROW), 2, y);
        }
    }

    private void handleTooltip(MouseEvent e) {
        int mx = e.getX();
        int my = e.getY();
        int total = (int) fs.superBlock.getTotalBlocks();

        int col = (mx - 50) / CELL_SIZE;
        int row = my / CELL_SIZE;
        int idx = row * CELLS_PER_ROW + col;

        if (col < 0 || col >= CELLS_PER_ROW || idx < 0 || idx >= total) {
            gridPanel.setToolTipText(null);
            return;
        }

        boolean busy = fs.bitmapBlocks.isBusy(idx);
        if (!busy) {
            gridPanel.setToolTipText("Bloque " + idx + " \u2014 Libre");
        } else {
            String owner = blockOwners != null ? blockOwners.get(idx) : null;
            gridPanel.setToolTipText("Bloque " + idx + " \u2014 " + (owner != null ? owner : "Desconocido"));
        }
    }

    private void loadBlockOwners() {
        blockOwners = new HashMap<>();
        for (int i = 0; i < 100; i++) {
            try {
                FCB fcb = fs.getFCB(i);
                if (fcb.getName().isEmpty() || fcb.getBlockCount() <= 0) continue;

                StringBuilder sb = new StringBuilder();
                if (fcb.getIsOpen() == 1) sb.append('\u2605');
                if (fcb.getType() == 1) sb.append("[DIR] ");
                else if (fcb.getType() == 2) sb.append("[LNK] ");
                else sb.append("[FILE] ");
                sb.append(buildPath(fcb));

                String label = sb.toString();
                int start = fcb.getStartBlock();
                int end = start + fcb.getBlockCount();
                for (int b = start; b < end; b++) {
                    if (b >= 0) blockOwners.put(b, label);
                }
            } catch (Exception ignored) {
            }
        }
    }

    private String buildPath(FCB fcb) {
        try {
            if (fcb.getParentId() == -1) return "/" + fcb.getName();
            return buildPath(fs.getFCB(fcb.getParentId())) + "/" + fcb.getName();
        } catch (Exception e) {
            return "/" + fcb.getName();
        }
    }

    private void refresh() {
        try {
            long bmpSize = (long) Math.ceil(fs.superBlock.getTotalBlocks() / 8.0);
            byte[] data = fs.disk.read(fs.superBlock.getBitmapBlocksStart(), (int) bmpSize);
            fs.bitmapBlocks = Bitmap.fromBytes(data);
            loadBlockOwners();
            gridPanel.setPreferredSize(computeGridSize());
            gridPanel.revalidate();
            gridPanel.repaint();
            compactPanel.repaint();

            int total = (int) fs.superBlock.getTotalBlocks();
            int used = fs.bitmapBlocks.countUsedBlocks();
            int free = total - used;
            long totalBytes = fs.superBlock.getTotalDiskSize();
            statusLabel.setText(String.format(
                "Disco: %d KB  |  Bloques: %d  |  Usados: %d (%d KB)  |  Libres: %d (%d KB)  |  %.1f%% usado",
                totalBytes / 1024, total, used, used * 512 / 1024,
                free, free * 512 / 1024, (double) used / total * 100));
        } catch (Exception e) {
            statusLabel.setText("Error al refrescar: " + e.getMessage());
        }
    }

    private void startAutoRefresh() {
        refreshTimer = new javax.swing.Timer(2000, e -> {
            if (isVisible()) refresh();
        });
        refreshTimer.start();
    }

    @Override
    public void dispose() {
        if (refreshTimer != null) refreshTimer.stop();
        super.dispose();
    }
}
