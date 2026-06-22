package FS.terminal;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class NoteEditor {
    private String contenidoOriginal;
    private String resultado;
    private boolean guardado;
    
    public NoteEditor(String contenidoInicial) {
        this.contenidoOriginal = contenidoInicial != null ? contenidoInicial : "";
        this.resultado = null;
        this.guardado = false;
    }
    
    public String openEditor() {
        JFrame frame = new JFrame("Note - Editor");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setSize(600, 400);
        frame.setLocationRelativeTo(null);
        
        JTextArea textArea = new JTextArea(contenidoOriginal);
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        JScrollPane scroll = new JScrollPane(textArea);
        
        JButton btnGuardar = new JButton("Guardar");
        JButton btnCancelar = new JButton("Cancelar");
        
        btnGuardar.addActionListener(e -> {
            resultado = textArea.getText();
            guardado = true;
            frame.dispose();
        });
        
        btnCancelar.addActionListener(e -> {
            resultado = contenidoOriginal;
            guardado = false;
            frame.dispose();
        });
        
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                resultado = contenidoOriginal;
                guardado = false;
            }
        });
        
        JPanel panelBotones = new JPanel();
        panelBotones.add(btnGuardar);
        panelBotones.add(btnCancelar);
        
        frame.add(scroll, BorderLayout.CENTER);
        frame.add(panelBotones, BorderLayout.SOUTH);
        
        frame.setVisible(true);
        
        while (frame.isDisplayable()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                break;
            }
        }
        
        return resultado;
    }
}