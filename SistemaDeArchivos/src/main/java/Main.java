
import java.util.Scanner;
import FS.principal.*;
import FS.structures.*;
import FS.terminal.Terminal;


public class Main{ 
    public static void main(String[] args) {
        FileSystem fs = new FileSystem();
        Scanner scanner = new Scanner(System.in);

        if(args.length == 0){
            // No hay disco, esperar comando format
            System.out.print("$ ");
            String line = scanner.nextLine().trim();
            String[] parts = line.split(" ");

            if(parts[0].equals("format") && parts.length == 3){
                long size = Long.parseLong(parts[1]);
                String type = parts[2];
                System.out.print("Contraseña para root: ");
                String pass = scanner.nextLine();
                fs.createDisk(size, type, "miDiscoDuro.fs", pass);
            } else {
                System.out.println("Debe formatear el disco primero: format <tamaño> <mb|kb>");
                return;
            }
        } else {
            // A fuuro cargar disco

        }

        // Iniciar terminal
        Terminal terminal = new Terminal(fs);
        terminal.start();
    }
}