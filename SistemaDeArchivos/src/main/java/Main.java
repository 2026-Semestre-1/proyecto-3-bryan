
import FS.principal.*;
import FS.terminal.Terminal;
import FS.terminal.HackerTerminal;
import java.io.File;
import java.util.Scanner;
import javax.swing.SwingUtilities;


public class Main{ 
    public static void main(String[] args) throws java.io.IOException {
        FileSystem fs = new FileSystem();
        Scanner scan = new Scanner(System.in);

        if (args.length == 0) {
            while (true) {
                System.out.println("\n=== POLAR File System ===");
                System.out.println("1. Nueva terminal (sin formato)");
                System.out.println("2. Cargar disco existente");
                System.out.println("3. Salir");
                System.out.print("Seleccione: ");
                String opt = scan.nextLine().trim();
                
                if (opt.equals("1")) {
                    break;
                    
                } else if (opt.equals("2")) {
                    File dir = new File(".");
                    File[] discos = dir.listFiles((d, n) -> n.endsWith(".fs"));
                    if (discos == null || discos.length == 0) {
                        System.out.println("No hay discos existentes.");
                        continue;
                    }
                    System.out.println("Discos disponibles:");
                    for (int i = 0; i < discos.length; i++) {
                        System.out.println((i + 1) + ". " + discos[i].getName());
                    }
                    System.out.print("Seleccione (0 para cancelar): ");
                    String sel = scan.nextLine().trim();
                    int idx;
                    try {
                        idx = Integer.parseInt(sel) - 1;
                    } catch (NumberFormatException e) {
                        continue;
                    }
                    if (idx < 0 || idx >= discos.length) continue;
                    String fileName = discos[idx].getName();
                    boolean ok = fs.loadDisk(fileName);
                    if (!ok) {
                        System.out.println("Error al cargar disco.");
                        continue;
                    }
                    System.out.println("Disco cargado: " + fileName);
                    break;
                    
                } else if (opt.equals("3")) {
                    System.out.println("Adios.");
                    return;
                }
            }
            
        } else {
            String fileName = args[0];
            if (new File(fileName).exists()) {
                boolean ok = fs.loadDisk(fileName);
                if (!ok) {
                    System.out.println("Error al cargar '" + fileName + "'");
                    return;
                }
                System.out.println("Disco cargado: " + fileName);
            } else {
                System.out.println("'" + fileName + "' no existe.");
                return;
            }
        }

        Terminal terminal = new Terminal(fs);
        SwingUtilities.invokeLater(() -> {
            HackerTerminal gui = new HackerTerminal(terminal);
            gui.setVisible(true);
        });
        Object lock = new Object();
        synchronized (lock) {
            try { lock.wait(); } catch (InterruptedException e) {}
        }
    }
}