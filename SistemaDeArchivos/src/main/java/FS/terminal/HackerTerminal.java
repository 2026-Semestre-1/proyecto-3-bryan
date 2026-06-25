package FS.terminal;

import FS.principal.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import javax.swing.*;
import javax.swing.text.*;

public class HackerTerminal extends JFrame {

    private JTextArea outputArea;
    private AbstractDocument doc;
    private Terminal terminal;
    private Thread terminalThread;
    private BlockingQueue<String> inputQueue;
    private int outputEnd;
    private ByteArrayOutputStream outputBuffer;

    public HackerTerminal(Terminal terminal) {
        this.terminal = terminal;
        this.inputQueue = new LinkedBlockingQueue<>();
        this.outputEnd = 0;
        this.outputBuffer = new ByteArrayOutputStream();
        terminal.setOut(new PrintStream(outputBuffer, true, StandardCharsets.UTF_8));
        terminal.setNewTerminalHandler(fs -> {
            SwingUtilities.invokeLater(() -> {
                HackerTerminal nueva = new HackerTerminal(new Terminal(fs));
                nueva.setVisible(true);
            });
        });
        initUI();
        startTerminal();
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                outputArea.requestFocusInWindow();
            }
        });
    }

    private void initUI() {
        setTitle("POLAR File System — Terminal");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(960, 640);
        setLocationRelativeTo(null);

        outputArea = new JTextArea();
        outputArea.setBackground(Color.BLACK);
        outputArea.setForeground(new Color(0, 255, 0));
        outputArea.setCaretColor(new Color(0, 255, 0));
        outputArea.setFont(new Font("Consolas", Font.PLAIN, 15));
        outputArea.setEditable(true);
        outputArea.setTabSize(4);

        doc = (AbstractDocument) outputArea.getDocument();
        doc.setDocumentFilter(new InputFilter());

        outputArea.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    e.consume();
                    String line = outputArea.getText().substring(outputEnd).trim();
                    outputEnd = doc.getLength();
                    SwingUtilities.invokeLater(() -> {
                        outputArea.append("\n");
                        outputArea.setCaretPosition(doc.getLength());
                    });
                    inputQueue.offer(line);
                } else if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
                    if (outputArea.getCaretPosition() <= outputEnd) {
                        e.consume();
                    }
                } else if (e.getKeyCode() == KeyEvent.VK_LEFT || e.getKeyCode() == KeyEvent.VK_UP
                        || e.getKeyCode() == KeyEvent.VK_HOME) {
                    if (outputArea.getCaretPosition() <= outputEnd) {
                        e.consume();
                    }
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(outputArea);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(0, 100, 0)));
        scrollPane.setBackground(Color.BLACK);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(Color.BLACK);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        add(mainPanel);
    }

    private class InputFilter extends DocumentFilter {
        @Override
        public void insertString(FilterBypass fb, int offset, String text, AttributeSet attr)
                throws BadLocationException {
            if (offset >= outputEnd) {
                super.insertString(fb, offset, text, attr);
            }
        }

        @Override
        public void remove(FilterBypass fb, int offset, int length) throws BadLocationException {
            if (offset >= outputEnd) {
                super.remove(fb, offset, length);
            }
        }

        @Override
        public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attr)
                throws BadLocationException {
            if (offset >= outputEnd) {
                super.replace(fb, offset, length, text, attr);
            }
        }
    }

    private void flushOutput() {
        byte[] pending;
        synchronized (outputBuffer) {
            if (outputBuffer.size() == 0) return;
            pending = outputBuffer.toByteArray();
            outputBuffer.reset();
        }
        String text = new String(pending, StandardCharsets.UTF_8);
        try {
            SwingUtilities.invokeAndWait(() -> {
                outputArea.append(text);
                outputEnd = doc.getLength();
                outputArea.setCaretPosition(outputEnd);
            });
        } catch (Exception ex) {
            System.err.println("flushOutput error: " + ex);
        }
    }

    private void startTerminal() {
        terminalThread = new Thread(() -> {
            InputStream customIn = new InputStream() {
                private ByteArrayInputStream buffer = new ByteArrayInputStream(new byte[0]);

                @Override
                public int read() throws IOException {
                    if (buffer.available() == 0) {
                        flushOutput();
                        try {
                            String line = inputQueue.take();
                            if (line == null) return -1;
                            buffer = new ByteArrayInputStream((line + "\n").getBytes());
                        } catch (InterruptedException e) {
                            return -1;
                        }
                    }
                    return buffer.read();
                }

                @Override
                public int read(byte[] b, int off, int len) throws IOException {
                    if (b == null) throw new NullPointerException();
                    if (off < 0 || len < 0 || len > b.length - off) throw new IndexOutOfBoundsException();
                    if (len == 0) return 0;
                    int c = read();
                    if (c == -1) return -1;
                    b[off] = (byte) c;
                    int i = 1;
                    for (; i < len && buffer.available() > 0; i++) {
                        b[off + i] = (byte) buffer.read();
                    }
                    return i;
                }

                @Override
                public int available() {
                    return buffer.available();
                }
            };
            try {
                terminal.start(new Scanner(customIn));
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    outputArea.append("\n[Terminal finalizado]\n");
                });
            }
        }, "Terminal-Thread");
        terminalThread.setDaemon(true);
        terminalThread.start();
    }

}
