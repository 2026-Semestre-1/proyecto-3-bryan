
import FS.principal.*;
import FS.terminal.Terminal;
import FS.terminal.HackerTerminal;
import javax.swing.SwingUtilities;


public class Main{ 
    public static void main(String[] args) throws java.io.IOException {
        FileSystem fs = new FileSystem();

        if(args.length == 0){
            // Crear disco con configuracion predeterminada
            fs.createDisk(3, "mb", "miDiscoDuro.fs", "root");
        }

        Terminal terminal = new Terminal(fs);

        SwingUtilities.invokeLater(() -> {
            HackerTerminal gui = new HackerTerminal(terminal);
            gui.setVisible(true);
        });
    }
}