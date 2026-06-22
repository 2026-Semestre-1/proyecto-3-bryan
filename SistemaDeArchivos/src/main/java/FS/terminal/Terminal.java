/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package FS.terminal;
import FS.structures.*;
import FS.principal.*;
import java.io.IOException;
import java.util.Arrays;
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

    public void start() throws IOException{
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
    
    public int executeCommand(String command, String[] parts) throws IOException{
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
                        newBlock, 1, System.currentTimeMillis(), System.currentTimeMillis(), (byte)0, 1);
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
                
            case "groupadd":
                // FALTA VER LO DE QUE ES UN USUARIO PRIVILEGIADO PARA VER SI HAY OTROS APARTE DE ROOT
                
                System.out.println("hola5");
                if (parts.length < 2){
                    System.out.println("Debe ingresar el nombre del grupo");
                    return 0;
                }
                String groupName =  parts[1];
                if (fs.findGroup(groupName) == -1){            
                    int slotGroupAdd = fs.freeslotGroups();
                    if (slotGroupAdd != -1){
                        Group grup = new Group(groupName, new int[10]);
                        Arrays.fill(grup.getMembers(), -1);
                        fs.writeGroup(grup, slotGroupAdd);  
                        System.out.println("El grupo fue creado con exito");
                        return 0;
                    } else {
                        System.out.println("No hay espacio para más grupos");
                    }
                    
                   
                } else {
                    System.out.println("El grupo ya existe");
                    return 0;
                }

                

                
            case "passwd":
                System.out.println("hola4");
                if (parts.length == 1){
                    System.out.println("Vamos a cambiar nuestro password");
                    int xx = 0;
                    while(xx < 3){ 
                        System.out.println("Escriba su password");
                        String p1 = scan.nextLine().trim();
                        if (this.currentUser.getPassword().equals(p1)){
                            int xx2 = 0;
                            while(xx2 < 3) {
                                System.out.println("Ingrese la nueva contraseña");
                                String p2 = scan.nextLine().trim();
                                System.out.println("Confirme la nueva contraseña por favor");                           
                                String p3 = scan.nextLine().trim();
                                if (p2.equals(p3)){
                                    this.currentUser.setPassword(p3);
                                    int userIndex = fs.findUser(this.currentUser.getUserName());
                                    fs.writeUser(this.currentUser, userIndex);
                                    System.out.println("La contrasena fue cambiada con exito");
                                    return 0;
                                    
                                }
                                xx2++;
                            }
                        }
                        xx++;
                    }
                    
                } else {
                    if(!this.currentUser.getUserName().equals("root")){
                        System.out.println("Solo root puede cambiar la contraseña de otros usuarios");
                        return 0;
                    }
                    // Caso en el que es passwd con username
                    String userToChange = parts[1];
                    System.out.println("Vamos a cambiar el password");
                    int xx3 = 0;
                    while(xx3 < 3){
                        System.out.println("Ingrese la contrasena actual del usuario");
                        String p3 = scan.nextLine().trim();
                        int userIndex2 = fs.findUser(this.currentUser.getUserName());
                        if(fs.confirmPasswordUser(p3, userIndex2)) {
                            int xx4 = 0;
                                while(xx4 < 3){
                                System.out.println("Ahora ingrese la nueva contrasena");
                                String p4 = scan.nextLine().trim();
                                System.out.println("Vuelva a ingresar la nueva contrasena");
                                String p5 = scan.nextLine().trim();                                
                                if(p4.equals(p5)){
                                    System.out.println("Se ha cambiado la contrasena");
                                    int userIndex3 = fs.findUser(userToChange);
                                    User u2 = fs.getUser(userIndex3);
                                    u2.setPassword(p4);
                                    fs.writeUser(u2, userIndex3);
                                    return 0;
                                }
                                xx4++;
                            }
                        }
                        xx3++;
                    }
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
                
            
            case "pwd":
                FCB temp = currentDirectory;
                String pwdPath = "";
                while (temp.getParentId() != -1) {
                    pwdPath = "/" + temp.getName() + pwdPath;
                    temp = fs.getFCB(temp.getParentId());
                }
                pwdPath = pwdPath.isEmpty() ? "/" : pwdPath;
                System.out.println("Ruta actual: " + pwdPath);
                break;
                
                
                
            case "mkdir":
                System.out.println("Vamos a hacer directorios");
                if(parts.length == 2){
                    //Caso en el que solo es un directorio mkdir a

                    String directName = parts[1];
                    long data = fs.superBlock.getDataZoneStart();
                    int directionDirec = this.currentDirectory.getStartBlock();
                    long offsetData = data + (directionDirec * 512);
                    for(int p = 0; p < this.currentDirectory.getSizeUsed(); p++ ){
                        byte[] dataEntry = fs.disk.read(offsetData + (p * 24), 24);
                        DirectoryEntry var = DirectoryEntry.fromBytes(dataEntry);
                        if( var.getName().equals(directName)){
                            System.out.println("El directorio ya existe actualmente");
                            return 0;
                        } 
                    }

                    int sloftFreeFCB = fs.freeslotFCB();                    
                    if (sloftFreeFCB != -1){
                        int newBlockFCB = fs.bitmapBlocks.findFreeBit();
                        if (newBlockFCB != -1){ 
                            fs.bitmapBlocks.markBusy(newBlockFCB);
                            int user2 = fs.findUser(currentUser.getUserName());
                            int grouID2 = this.currentUser.getGroupId();
                            int parentID2 = fs.findFCBID(currentDirectory.getName(), currentDirectory.getParentId());
                            FCB fcb2 = new FCB(directName, (byte)1, user2, grouID2, FCB.grantPerm(7,0), 0, 
                                newBlockFCB, 1, System.currentTimeMillis(), System.currentTimeMillis(), (byte)0, parentID2);
                            fs.writeFCB(fcb2, sloftFreeFCB);
                            fs.disk.write(fs.superBlock.getBitmapBlocksStart(), fs.bitmapBlocks.toBytes());

                            long posDisk = fs.superBlock.dataZoneStart + (currentDirectory.getStartBlock() * 512 ) + (currentDirectory.getSizeUsed() * 24);
                            DirectoryEntry direct2 = new DirectoryEntry(directName, sloftFreeFCB);
                            fs.disk.write(posDisk, direct2.toBytes());
                            currentDirectory.setSizeUsed(currentDirectory.getSizeUsed()+1);
                            fs.writeFCB(currentDirectory, parentID2);    
                            System.out.println("El directorio fue creado");
                            return 0;
                        } else {
                            System.out.println("No hay bloques libres");
                            return 0;
                        }
                       
                    } else {
                        System.out.println("No hay espacio disponibles para el FCB");
                        return 0;
                        
                    }

                    
                } else if(parts.length > 2){
                    System.out.println("Caso de varios directorios");
                    int cont = 1;
                    while(cont < parts.length){
                        String directoryNames = parts[cont];
                        System.out.println("Nombre del directorio: " + directoryNames);
                        long data = fs.superBlock.getDataZoneStart();
                        int directionDirec = this.currentDirectory.getStartBlock();
                        long offsetData = data + (directionDirec * 512);
                        for(int p = 0; p < this.currentDirectory.getSizeUsed(); p++ ){
                            byte[] dataEntry = fs.disk.read(offsetData + (p * 24), 24);
                            DirectoryEntry var = DirectoryEntry.fromBytes(dataEntry);
                            if( var.getName().equals(directoryNames)){
                                System.out.println("El directorio ya existe actualmente");
                                return 0;
                            } 
                        }

                        int sloftFreeFCB = fs.freeslotFCB();                    
                        if (sloftFreeFCB != -1){
                            int newBlockFCB = fs.bitmapBlocks.findFreeBit();
                            if (newBlockFCB != -1){ 
                                fs.bitmapBlocks.markBusy(newBlockFCB);
                                int user2 = fs.findUser(currentUser.getUserName());
                                int grouID2 = this.currentUser.getGroupId();
                                int parentID2 = fs.findFCBID(currentDirectory.getName(), currentDirectory.getParentId());
                                FCB fcb2 = new FCB(directoryNames, (byte)1, user2, grouID2, FCB.grantPerm(7,0), 0, 
                                    newBlockFCB, 1, System.currentTimeMillis(), System.currentTimeMillis(), (byte)0, parentID2);
                                fs.writeFCB(fcb2, sloftFreeFCB);
                                fs.disk.write(fs.superBlock.getBitmapBlocksStart(), fs.bitmapBlocks.toBytes());

                                long posDisk = fs.superBlock.dataZoneStart + (currentDirectory.getStartBlock() * 512 ) + (currentDirectory.getSizeUsed() * 24);
                                DirectoryEntry direct2 = new DirectoryEntry(directoryNames, sloftFreeFCB);
                                fs.disk.write(posDisk, direct2.toBytes());
                                currentDirectory.setSizeUsed(currentDirectory.getSizeUsed()+1);
                                fs.writeFCB(currentDirectory, parentID2);  
                                if (cont == (parts.length-1)){
                                    System.out.println("soy cont: " + cont);
                                    System.out.println("El directorio fue creado: " + directoryNames);
                                    return 0;
                                }                                    
                                cont++;                            
                                System.out.println("El directorio fue creado: " + directoryNames);

                                

                                
                                
                            } else {
                                System.out.println("No hay bloques libres");
                                return 0;
                            }

                        } else {
                            System.out.println("No hay espacio disponibles para el FCB");
                            return 0;

                        }                        
                    }
                    System.out.println("No se pudieron crear los directorios");
                    return 0;
                }
                
            case "cd":
                System.out.println("Vamos a hacer un cd");
                if(!parts[1].equals("..")){
                    long data = fs.superBlock.getDataZoneStart();
                    int directionDirec = this.currentDirectory.getStartBlock();
                    long offsetData = data + (directionDirec * 512);
                    for(int p = 0; p < this.currentDirectory.getSizeUsed(); p++ ){
                        byte[] dataEntry = fs.disk.read(offsetData + (p * 24), 24);
                        DirectoryEntry var = DirectoryEntry.fromBytes(dataEntry);
                        if( var.getName().equals(parts[1])){
                            this.currentDirectory = fs.getFCB(var.getFcbId());
                        }


                    }                    
                } else {
                    if(currentDirectory.getParentId() != -1){
                        currentDirectory = fs.getFCB(currentDirectory.getParentId());
                    }
                }
                
                
            case "touch":
                System.out.println("Vamos a hacer un touch");
                
                if(parts.length == 2){
                    
                    String nameFCB3 = parts[1];
                    
                    // Validacion del nombre
                    long data = fs.superBlock.getDataZoneStart();
                    int directionDirec = this.currentDirectory.getStartBlock();
                    long offsetData = data + (directionDirec * 512);
                    for(int p = 0; p < this.currentDirectory.getSizeUsed(); p++ ){
                        byte[] dataEntry = fs.disk.read(offsetData + (p * 24), 24);
                        DirectoryEntry var = DirectoryEntry.fromBytes(dataEntry);
                        if( var.getName().equals(nameFCB3)){
                            System.out.println("El archivo ya existe actualmente");
                            return 0;
                        } 
                    }

                    //Busco FCB libre
                   
                    int freeHoleFCB = fs.freeslotFCB();
                    if (freeHoleFCB == -1 ){
                        System.out.println("No queda espacio para más FCBS");
                        return 0;
                    }                    
                    
                    // Buscamos un bloque libre de 512
                    int newBlockT = fs.bitmapBlocks.findFreeBit();
                    fs.bitmapBlocks.markBusy(newBlockT);
                    fs.disk.write(fs.superBlock.getBitmapBlocksStart(), fs.bitmapBlocks.toBytes());

                    
                    
                    int idUserFCB3 = fs.findUser(currentUser.getUserName());
                    int idGroupFCB3 = currentUser.getGroupId();
                    int parentID3 = fs.findFCBID(currentDirectory.getName(), currentDirectory.getParentId());
                    // Creo FCB (Solo memo)
                    FCB fcb3 = new FCB(nameFCB3, (byte)0, idUserFCB3, idGroupFCB3, FCB.grantPerm(7,0), 0, newBlockT, 1, System.currentTimeMillis(),
                    System.currentTimeMillis(), (byte)0 ,parentID3);
                    
                    
                    // Lo guardamos en disco
                    fs.writeFCB(fcb3, freeHoleFCB);
                    
                    // Calculo la dirección del directorio papa
                    long directoryDirectFather = fs.superBlock.getDataZoneStart() + (currentDirectory.getStartBlock() * 512) + (currentDirectory.getSizeUsed() * 24);
                    DirectoryEntry directoryEntry3 = new DirectoryEntry(nameFCB3,freeHoleFCB);
                    fs.disk.write(directoryDirectFather, directoryEntry3.toBytes());
                    
                    currentDirectory.setSizeUsed((currentDirectory.getSizeUsed()+1));
                    fs.writeFCB(currentDirectory, parentID3);
                    
                    
                } else if (parts.length == 1){
                    System.out.println("Ingrese el nombre del archivo");
                    return 0;
                } else {
                    System.out.println("Ingrese correctamente el comando");
                    return 0;
                }
                
            case "cat":
                if (parts.length < 2) {
                    System.out.println("Ingrese el nombre del archivo");
                    return 0;
                }
                String fileName2 = parts[1];                
                
                // Busco el archivo y lo trato de leer en el directorio actual
                long data2 = fs.superBlock.getDataZoneStart();
                int blockDir2 = currentDirectory.getStartBlock();
                long offset2 = data2 + (blockDir2 * 512);

                int fcbIndex2 = -1;
                for (int i = 0; i < currentDirectory.getSizeUsed(); i++) {
                    byte[] entryData = fs.disk.read(offset2 + (i * 24), 24);
                    DirectoryEntry entry = DirectoryEntry.fromBytes(entryData);
                    if (entry.getName().equals(fileName2)) {
                        fcbIndex2 = entry.getFcbId();
                        break;
                    }
                } 
                if (fcbIndex2 == -1){
                    System.out.println("Archivo no encontrado");
                    return 0;
                }
                
                FCB fileFCB2 = fs.getFCB(fcbIndex2);
                
                if (!currentUser.getUserName().equals("root")){
                    int ownerPerm = FCB.getOwnerPerm(fileFCB2.getPermissions());
                    int groupPerm = FCB.getGroupPerm(fileFCB2.getPermissions());
                    int userIndex = fs.findUser(currentUser.getUserName());
                    int permissions;

                    if (userIndex == fileFCB2.getOwnerId()) {
                        permissions = ownerPerm;
                    } else if (currentUser.getGroupId() == fileFCB2.getGroupId()) {
                        permissions = groupPerm;
                    } else {
                        System.out.println("No eres dueño ni perteneces al grupo del archivo");
                        return 0;
                    }

                    if ((permissions & 4) == 0) {
                        System.out.println("No tienes permiso de lectura");
                        return 0;
                    }                    
                }
                
                // pasa validaciones y entonces leemos el contenido
                byte[] fileInfo = fs.readFileData(fileFCB2);
                String content = new String(fileInfo, 0, fileFCB2.getSizeUsed());
                System.out.println(content);
                
                break;
                
            case "less":
                if (parts.length < 2) {
                    System.out.println("Ingrese el nombre del archivo");
                    return 0;
                }
                String lessName = parts[1];

                // Buscamos archivo para leerlo
                long dataLess = fs.superBlock.getDataZoneStart();
                int blockLess = currentDirectory.getStartBlock();
                long offsetLess = dataLess + (blockLess * 512);

                int fcbLess = -1;
                for (int i = 0; i < currentDirectory.getSizeUsed(); i++) {
                    byte[] entryData = fs.disk.read(offsetLess + (i * 24), 24);
                    DirectoryEntry entry = DirectoryEntry.fromBytes(entryData);
                    if (entry.getName().equals(lessName)) {
                        fcbLess = entry.getFcbId();
                        break;
                    }
                }
                if (fcbLess == -1) {
                    System.out.println("Archivo no encontrado");
                    return 0;
                }

                // obtenemos el fcb del archivo
                FCB fcbLessFCB = fs.getFCB(fcbLess);
                byte[] lessData = fs.readFileData(fcbLessFCB);
                // Todo el cuerpo del archivo por linea
                String[] lessLines = new String(lessData, 0, fcbLessFCB.getSizeUsed()).split("\n", -1);
                int lineIndex = 0;

                // Simple recorrido para ir linea por linea, hacia atras o con una pag completa que serian 20 lineas
                while (true) {
                    for (int i = lineIndex; i < lessLines.length && i < lineIndex + 20; i++) {
                        System.out.println(lessLines[i]);
                    }
                    System.out.println("-- " + (lineIndex + 1) + "/" + lessLines.length + "  q -> salir  Enter -> sig  espacio -> pag  - ->atras");

                    String input = scan.nextLine();
                    if (input.equals("q")) {
                        break;
                    } else if (input.isEmpty()) {
                        if (lineIndex < lessLines.length - 1) lineIndex++;
                    } else if (input.equals(" ") || input.equals("  ")) {
                        lineIndex = Math.min(lineIndex + 20, lessLines.length - 1);
                    } else if (input.equals("-")) {
                        if (lineIndex > 0) {
                            lineIndex--;
                        }
                    }
                }
                break;
                
            case "note":             
                if (parts.length < 2) {
                    System.out.println("Ingrese el nombre del archivo");
                    return 0;
                }
                String fileName = parts[1];

                // Vemos si el archivo esta en el directorial actual
                long data = fs.superBlock.getDataZoneStart();
                int blockDir = currentDirectory.getStartBlock();
                long offset = data + (blockDir * 512);

                int fcbIndex = -1;
                for (int i = 0; i < currentDirectory.getSizeUsed(); i++) {
                    byte[] entryData = fs.disk.read(offset + (i * 24), 24);
                    DirectoryEntry entry = DirectoryEntry.fromBytes(entryData);
                    if (entry.getName().equals(fileName)) {
                        fcbIndex = entry.getFcbId();
                        break;
                    }
                }

                if (fcbIndex == -1) {
                    System.out.println("Archivo no encontrado");
                    return 0;
                }

                FCB fileFCB = fs.getFCB(fcbIndex);

                // vemos si el usuario tiene permisos
                if (!currentUser.getUserName().equals("root")) {
                    int ownerPerm = FCB.getOwnerPerm(fileFCB.getPermissions());
                    int groupPerm = FCB.getGroupPerm(fileFCB.getPermissions());
                    int userIndex = fs.findUser(currentUser.getUserName());
                    int permissions2;

                    if (userIndex == fileFCB.getOwnerId()) {
                        permissions2 = ownerPerm;
                    } else if (currentUser.getGroupId() == fileFCB.getGroupId()) {
                        permissions2 = groupPerm;
                    } else {
                        System.out.println("No eres dueño ni perteneces al grupo del archivo");
                        return 0;
                    }

                    if ((permissions2 & 4) == 0) {
                        System.out.println("No tienes permiso de lectura");
                        return 0;
                    }
                    if ((permissions2 & 2) == 0) {
                        System.out.println("No tienes permiso de escritura");
                        return 0;
                    }
                }

                // Leemos disco
                byte[] fileData = fs.readFileData(fileFCB);
                String content3 = new String(fileData, 0, fileFCB.getSizeUsed());

                // aqui ya abrimos el swing par verlo graficamente
                NoteEditor editor = new NoteEditor(content3);
                String newContent= editor.openEditor();

                if (!newContent.equals(content3)) {
                    fs.writeFileData(fileFCB, fcbIndex, newContent.getBytes());
                    System.out.println("Archivo guardado");
                } else {
                    System.out.println("No se guardaron cambios");
                }
                break;
                
            
        }
        
        
        return 0;
    }
    
}
