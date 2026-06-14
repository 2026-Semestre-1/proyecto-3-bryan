/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package FS.terminal;
import FS.structures.*;
import FS.principal.*;
import java.util.Scanner;
/**
 *
 * @author bryan
 */
public class Terminal {
    User currentUser;
    FCB currentDirectory;
    FileSystem fs;
    
    public Terminal(User CurrentUser, FCB CurrentDirectory, FileSystem FS){
        this.currentUser = CurrentUser;
        this.currentDirectory = CurrentDirectory;
        this.fs = FS;
    }
    
    public Terminal(FileSystem FS){
        this.fs = FS;
        this.currentUser = fs.getUser(0);
        this.currentDirectory = fs.getFCB(2);
    }    
    
   
    
    
    public User getCurrentUser() { return currentUser; }
    public void setCurrentUser(User currentUser) { this.currentUser = currentUser; }

    public FCB getCurrentDirectory() { return currentDirectory; }
    public void setCurrentDirectory(FCB currentDirectory) { this.currentDirectory = currentDirectory; }

    public FileSystem getFs() { return fs; }
    public void setFs(FileSystem fs) { this.fs = fs; }

    public void start(){
        Scanner scanner = new Scanner(System.in);
        while(true){
            System.out.println(currentUser.getUserName() + "@miFS: ");
            
            String line = scanner.nextLine().trim();
            
            if(line.isEmpty()) continue;
            String[] parts = line.split(" ");
            String command = parts[0].toLowerCase();
            int result = executeCommand(command, parts);
            if (result == 2){
                break;
            }
            
            
            
            
        }
    }
    
    public int executeCommand(String command, String[] parts){
        Scanner scan = new Scanner(System.in);
        switch(command.toLowerCase()){
            case "exit":
                System.out.println("Hola");
                
                return 2;
                
            case "useradd":
                System.out.println("hola1");
                String userName = parts[1].toLowerCase();
                System.out.println("Nombre de usuario: " + userName);
                System.out.println("Ingrese su nombre completo por favor: " );
                String fullName = scan.nextLine().trim();
                System.out.println("Cree una contrasena: " );
                String password = scan.nextLine().trim();    
                System.out.println("Esta seguro que desea crear la cuenta ingrese (y)/(n): " );
                String confirmation = scan.nextLine().trim();
                if (confirmation.toLowerCase().equals("y")){
                    int slot = fs.freeSlotUsers();
                    int slotG = fs.freeslotGroups();
                    int slotFC = fs.freeslotFCB();
                    if (slot == -1){
                        System.out.println("Error: No hay espacio para crear más usuarios");
                        return 0;
                    }
                    if (slotG == -1){
                        System.out.println("Error: No hay espacio para crear más grupos");
                        return 0;                        
                    }
                    if (slotFC == -1){
                        System.out.println("Error: No hay espacio para crear más FCB'S");
                        return 0;                        
                    }                    
                    Group gr = new Group(userName, new int[]{slot});
                    int newBlock = fs.bitmapBlocks.findFreeBit();
                    fs.bitmapBlocks.markBusy(newBlock);
                    FCB fc = new FCB(userName, (byte)1, slot, slotG, FCB.grantPerm(7,0), 0, 
                        newBlock, 1, System.currentTimeMillis(), System.currentTimeMillis(), (byte)0);
                    User us = new User(userName, password, fullName, slotG, slotFC);
                    
                    // Guardo ahora en disco
                    fs.writeGroup(gr, slotG);
                    fs.writeFCB(fc, slotFC);
                    fs.writeUser(us, slot);
                    fs.disk.write(fs.superBlock.getBitmapBlocksStart(), fs.bitmapBlocks.toBytes());                   
                    FCB userDir = fs.getFCB(1);
                    long blockOffset = fs.superBlock.getDataZoneStart() + (userDir.getStartBlock() * 512);
                    long entryOffset = blockOffset + (userDir.getSizeUsed() * 24);
                    
                    DirectoryEntry newEntryDir = new DirectoryEntry(userName, slotFC);
                    fs.disk.write(entryOffset, newEntryDir.toBytes());
                    
                    userDir.setSizeUsed(userDir.getSizeUsed() + 1);
                    fs.writeFCB(userDir, 1);
                    return 0;

                    
                    
                }
                
            case "su":
                if(parts.length == 1){
                    // Por si pide un usuario en el que ya esta logueado
                    if(this.currentUser.getUserName().equals("root")){
                        System.out.println("Ya se encuentra utilizando el usuario root");
                        break;
                    }
                    // CASO SU SOLO
                    System.out.println("Por favor ingrese la contraseña de root: ");
                    int ff = 0;
                    while(ff < 3){
                        String confirmPassword = scan.nextLine().trim();
                        if(fs.confirmPasswordUser(confirmPassword, 0)){
                            this.currentUser = fs.getUser(0);
                            this.currentDirectory = fs.getFCB(2); 
                            System.out.println("Bienvenido root");
                            break;
                        } else {
                            System.out.println("Contraseña incorrecta");
                            ff++;
                        }
                    }
                    if(ff == 3){
                        System.out.println("Demasiados intentos fallidos");
                    }
                } else { // Por si quiere usar el mismo usuario
                    String user = parts[1];
                    if(this.currentUser.getUserName().equals(user)){
                        System.out.println("Ya se encuentra utilizando ese usuario");
                        break;
                    }
                    int res = fs.findUser(user);  // CASO CON SU y nombre a la par
                    if(res != -1){
                        System.out.println("Por favor ingrese la contraseña: ");
                        int ff = 0;
                        while(ff < 3){
                            String confirmPassword = scan.nextLine().trim();
                            if(fs.confirmPasswordUser(confirmPassword, res)){
                                this.currentUser = fs.getUser(res);
                                this.currentDirectory = fs.getFCB(this.currentUser.getHomeDirId());
                                System.out.println("Bienvenido " + user);
                                break;
                            } else {
                                System.out.println("Contraseña incorrecta");
                                ff++;
                            }
                        }
                        if(ff == 3){
                            System.out.println("Demasiados intentos fallidos");
                        }
                    } else {
                        System.out.println("El usuario no fue encontrado");
                    }
                }
                break;
               
                
                
                
                
            case "whoami":
                
                System.out.println("Username: " + this.currentUser.getUserName());
                System.out.println("Full name: " + this.currentUser.getFullName());
                break;
                
            
                
            
        }
        
        
        return 0;
    }
    
}
