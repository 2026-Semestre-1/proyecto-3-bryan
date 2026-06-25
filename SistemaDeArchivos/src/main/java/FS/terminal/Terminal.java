/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package FS.terminal;
import FS.structures.*;
import FS.principal.*;
import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Scanner;
import java.util.function.Consumer;
/**
 *
 * @author bryan
 */
public class Terminal {
    User currentUser;
    FCB currentDirectory;
    int currentDirectoryId;
    FileSystem fs;
    PrintStream out = System.out;
    
    public Terminal(User CurrentUser, FCB CurrentDirectory, FileSystem FS){
        this.currentUser = CurrentUser;
        this.currentDirectory = CurrentDirectory;
        this.fs = FS;
        this.currentDirectoryId = fs.findFCBID(CurrentDirectory.getName(), CurrentDirectory.getParentId());
    }
    
    public Terminal(FileSystem FS){
        this.fs = FS;
        this.currentUser = fs.getUser(0);
        this.currentDirectory = fs.getFCB(2);
        this.currentDirectoryId = 2;
    }    
    
   
    
    
    public User getCurrentUser() { return currentUser; }
    public void setCurrentUser(User currentUser) { this.currentUser = currentUser; }

    public FCB getCurrentDirectory() { return currentDirectory; }
    public void setCurrentDirectory(FCB currentDirectory) { this.currentDirectory = currentDirectory; }

    public FileSystem getFs() { return fs; }
    public void setFs(FileSystem fs) { this.fs = fs; }
    
    public void setOut(PrintStream out) { this.out = out; }

    private Consumer<FileSystem> newTerminalHandler;

    public void setNewTerminalHandler(Consumer<FileSystem> handler) {
        this.newTerminalHandler = handler;
    }

    public void start() throws IOException{
        start(new Scanner(System.in));
    }

    public void start(Scanner scanner) throws IOException{
        while(true){
            out.print(currentUser.getUserName() + "@miFS: ");
            
            String line = scanner.nextLine().trim();
            
            if(line.isEmpty()) continue;
            currentDirectory = fs.getFCB(currentDirectoryId);
            String[] parts = line.split(" ");
            String command = parts[0].toLowerCase();
            int result = executeCommand(command, parts, scanner);
            if (result == 2){
                break;
            }
        }
    }
    
    public int executeCommand(String command, String[] parts) throws IOException{
        Scanner scan = new Scanner(System.in);
        return executeCommand(command, parts, scan);
    }
    
    public int executeCommand(String command, String[] parts, Scanner scan) throws IOException{
        switch(command.toLowerCase()){
            case "exit":
                out.println("Hola");
                return 2;

            case "newterm":
                if (newTerminalHandler != null) {
                    newTerminalHandler.accept(fs);
                } else {
                    out.println("No hay manejador para nuevas terminales");
                }
                break;
                
            case "useradd":
                out.println("hola1");
                String userName = parts[1].toLowerCase();
                out.println("Nombre de usuario: " + userName);
                out.println("Ingrese su nombre completo por favor: " );
                String fullName = scan.nextLine().trim();
                out.println("Cree una contrasena: " );
                String password = scan.nextLine().trim();    
                out.println("Esta seguro que desea crear la cuenta ingrese (y)/(n): " );
                String confirmation = scan.nextLine().trim();
                if (confirmation.toLowerCase().equals("y")){
                    int slot = fs.freeSlotUsers();
                    int slotG = fs.freeslotGroups();
                    int slotFC = fs.freeslotFCB();
                    if (slot == -1){
                        out.println("Error: No hay espacio para crear más usuarios");
                        return 0;
                    }
                    if (slotG == -1){
                        out.println("Error: No hay espacio para crear más grupos");
                        return 0;                        
                    }
                    if (slotFC == -1){
                        out.println("Error: No hay espacio para crear más FCB'S");
                        return 0;                        
                    }                    
                    Group gr = new Group(userName, new int[]{slot});
                    int newBlock = fs.bitmapBlocks.findFreeBit();
                    fs.bitmapBlocks.markBusy(newBlock);
                    FCB fc = new FCB(userName, (byte)1, slot, slotG, FCB.grantPerm(7,5), 0, 
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
                break;
                
            case "groupadd":
                // FALTA VER LO DE QUE ES UN USUARIO PRIVILEGIADO PARA VER SI HAY OTROS APARTE DE ROOT
                
                out.println("hola5");
                if (parts.length < 2){
                    out.println("Debe ingresar el nombre del grupo");
                    return 0;
                }
                String groupName =  parts[1];
                if (fs.findGroup(groupName) == -1){            
                    int slotGroupAdd = fs.freeslotGroups();
                    if (slotGroupAdd != -1){
                        int creatorIdx = fs.findUser(currentUser.getUserName());
                        Group grup = new Group(groupName, new int[10]);
                        Arrays.fill(grup.getMembers(), -1);
                        if (creatorIdx != -1) {
                            grup.getMembers()[0] = creatorIdx;
                        }
                        fs.writeGroup(grup, slotGroupAdd);  
                        out.println("El grupo fue creado con exito");
                        return 0;
                    } else {
                        out.println("No hay espacio para más grupos");
                    }
                    
                   
                } else {
                    out.println("El grupo ya existe");
                    return 0;
                }
                break;

                

                
            case "passwd":
                if (parts.length == 1){
                    // Si nos cambiamos la contraseña a nosotros mismos
                    out.println("Vamos a cambiar nuestro password");
                    int xx = 0;
                    while(xx < 3){ 
                        out.println("Escriba su password");
                        String p1 = scan.nextLine().trim();
                        if (this.currentUser.getPassword().equals(p1)){
                            int xx2 = 0;
                            while(xx2 < 3) {
                                out.println("Ingrese la nueva contraseña");
                                String p2 = scan.nextLine().trim();
                                out.println("Confirme la nueva contraseña por favor");                           
                                String p3 = scan.nextLine().trim();
                                if (p2.equals(p3)){
                                    this.currentUser.setPassword(p3);
                                    int userIndex = fs.findUser(this.currentUser.getUserName());
                                    fs.writeUser(this.currentUser, userIndex);
                                    out.println("La contrasena fue cambiada con exito");
                                    return 0;                                    
                                }
                                xx2++;
                            }
                        }
                        xx++;
                    }
                } else if (parts.length == 2) {
                    // si quiero cambiar la de otro usuario
                    if(!this.currentUser.getUserName().equals("root")){
                        out.println("Solo root puede cambiar la contraseña de otros usuarios");
                        return 0;
                    }
                    String userToChange = parts[1];
                    out.println("Vamos a cambiar el password");
                    int xx3 = 0;
                    while(xx3 < 3){
                        out.println("Ingrese la contrasena actual del usuario");
                        String p3 = scan.nextLine().trim();
                        int userIndex2 = fs.findUser(this.currentUser.getUserName());
                        if(fs.confirmPasswordUser(p3, userIndex2)) {
                            int xx4 = 0;
                                while(xx4 < 3){
                                out.println("Ahora ingrese la nueva contrasena");
                                String p4 = scan.nextLine().trim();
                                out.println("Vuelva a ingresar la nueva contrasena");
                                String p5 = scan.nextLine().trim();                                
                                if(p4.equals(p5)){
                                    out.println("Se ha cambiado la contrasena");
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
                break;
                
            case "su":
                if(parts.length == 1){
                    // Por si pide un usuario en el que ya esta logueado
                    if(this.currentUser.getUserName().equals("root")){
                        out.println("Ya se encuentra utilizando el usuario root");
                        break;
                    }
                    // CASO SU SOLO
                    out.println("Por favor ingrese la contraseña de root: ");
                    int ff = 0;
                    while(ff < 3){
                        String confirmPassword = scan.nextLine().trim();
                        if(fs.confirmPasswordUser(confirmPassword, 0)){
                            this.currentUser = fs.getUser(0);
                            this.currentDirectory = fs.getFCB(2);
                            this.currentDirectoryId = 2;
                            out.println("Bienvenido root");
                            break;
                        } else {
                            out.println("Contraseña incorrecta");
                            ff++;
                        }
                    }
                    if(ff == 3){
                        out.println("Demasiados intentos fallidos");
                    }
                } else { // Por si quiere usar el mismo usuario
                    String user = parts[1];
                    if(this.currentUser.getUserName().equals(user)){
                        out.println("Ya se encuentra utilizando ese usuario");
                        break;
                    }
                    int res = fs.findUser(user);  // CASO CON SU y nombre a la par
                    if(res != -1){
                        out.println("Por favor ingrese la contraseña: ");
                        int ff = 0;
                        while(ff < 3){
                            String confirmPassword = scan.nextLine().trim();
                            if(fs.confirmPasswordUser(confirmPassword, res)){
                                this.currentUser = fs.getUser(res);
                                this.currentDirectory = fs.getFCB(this.currentUser.getHomeDirId());
                                this.currentDirectoryId = this.currentUser.getHomeDirId();
                                out.println("Bienvenido " + user);
                                break;
                            } else {
                                out.println("Contraseña incorrecta");
                                ff++;
                            }
                        }
                        if(ff == 3){
                            out.println("Demasiados intentos fallidos");
                        }
                    } else {
                        out.println("El usuario no fue encontrado");
                    }
                }
                break;
               
                
                
                
                
            case "whoami":
                
                out.println("Username: " + this.currentUser.getUserName());
                out.println("Full name: " + this.currentUser.getFullName());
                break;
                
            
            case "pwd":
                FCB temp = currentDirectory;
                String pwdPath = "";
                while (temp.getParentId() != -1) {
                    pwdPath = "/" + temp.getName() + pwdPath;
                    temp = fs.getFCB(temp.getParentId());
                }
                pwdPath = pwdPath.isEmpty() ? "/" : pwdPath;
                out.println("Ruta actual: " + pwdPath);
                break;
                
                
                
            case "mkdir":
                out.println("Vamos a hacer directorios");
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
                            out.println("El directorio ya existe actualmente");
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
                            FCB fcb2 = new FCB(directName, (byte)1, user2, grouID2, FCB.grantPerm(7,5), 0, 
                                newBlockFCB, 1, System.currentTimeMillis(), System.currentTimeMillis(), (byte)0, parentID2);
                            fs.writeFCB(fcb2, sloftFreeFCB);
                            fs.disk.write(fs.superBlock.getBitmapBlocksStart(), fs.bitmapBlocks.toBytes());

                            long posDisk = fs.superBlock.dataZoneStart + (currentDirectory.getStartBlock() * 512 ) + (currentDirectory.getSizeUsed() * 24);
                            DirectoryEntry direct2 = new DirectoryEntry(directName, sloftFreeFCB);
                            fs.disk.write(posDisk, direct2.toBytes());
                            currentDirectory.setSizeUsed(currentDirectory.getSizeUsed()+1);
                            fs.writeFCB(currentDirectory, parentID2);    
                            out.println("El directorio fue creado");
                            return 0;
                        } else {
                            out.println("No hay bloques libres");
                            return 0;
                        }
                       
                    } else {
                        out.println("No hay espacio disponibles para el FCB");
                        return 0;
                        
                    }

                    
                } else if(parts.length > 2){
                    out.println("Caso de varios directorios");
                    int cont = 1;
                    while(cont < parts.length){
                        String directoryNames = parts[cont];
                        out.println("Nombre del directorio: " + directoryNames);
                        long data = fs.superBlock.getDataZoneStart();
                        int directionDirec = this.currentDirectory.getStartBlock();
                        long offsetData = data + (directionDirec * 512);
                        for(int p = 0; p < this.currentDirectory.getSizeUsed(); p++ ){
                            byte[] dataEntry = fs.disk.read(offsetData + (p * 24), 24);
                            DirectoryEntry var = DirectoryEntry.fromBytes(dataEntry);
                            if( var.getName().equals(directoryNames)){
                                out.println("El directorio ya existe actualmente");
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
                                FCB fcb2 = new FCB(directoryNames, (byte)1, user2, grouID2, FCB.grantPerm(7,5), 0, 
                                    newBlockFCB, 1, System.currentTimeMillis(), System.currentTimeMillis(), (byte)0, parentID2);
                                fs.writeFCB(fcb2, sloftFreeFCB);
                                fs.disk.write(fs.superBlock.getBitmapBlocksStart(), fs.bitmapBlocks.toBytes());

                                long posDisk = fs.superBlock.dataZoneStart + (currentDirectory.getStartBlock() * 512 ) + (currentDirectory.getSizeUsed() * 24);
                                DirectoryEntry direct2 = new DirectoryEntry(directoryNames, sloftFreeFCB);
                                fs.disk.write(posDisk, direct2.toBytes());
                                currentDirectory.setSizeUsed(currentDirectory.getSizeUsed()+1);
                                fs.writeFCB(currentDirectory, parentID2);  
                                if (cont == (parts.length-1)){
                                    out.println("soy cont: " + cont);
                                    out.println("El directorio fue creado: " + directoryNames);
                                    return 0;
                                }                                    
                                cont++;                            
                                out.println("El directorio fue creado: " + directoryNames);

                                

                                
                                
                            } else {
                                out.println("No hay bloques libres");
                                return 0;
                            }

                        } else {
                            out.println("No hay espacio disponibles para el FCB");
                            return 0;

                        }                        
                    }
                    out.println("No se pudieron crear los directorios");
                    return 0;
                }
                
                
            case "rm":
                if (parts[1].contains("-R")){
                    // Caso en el que solo es recursivo
                    int currentFcbIdRM = fs.findFCBID(currentDirectory.getName(), currentDirectory.getParentId());
                    long offsetRM = fs.superBlock.getDataZoneStart() + (currentDirectory.getStartBlock() * 512);
                    
                    if (parts[2].contains("*")) {
                        // caso en el que es -R y borra aboslutamente todo
                        String regex = parts[2].replace("*", ".*");
                        ArrayList<Integer> matchesRM = new ArrayList<>();
                        for (int i = 0; i < currentDirectory.getSizeUsed(); i++) {
                            byte[] entryData = fs.disk.read(offsetRM + (i * 24), 24);
                            DirectoryEntry entry = DirectoryEntry.fromBytes(entryData);
                            if (entry.getName().matches(regex)) {
                                matchesRM.add(i);
                            }
                        }
                        for (int i = matchesRM.size() - 1; i >= 0; i--) {
                            int idx = matchesRM.get(i);
                            byte[] entryData = fs.disk.read(offsetRM + (idx * 24), 24);
                            DirectoryEntry entry = DirectoryEntry.fromBytes(entryData);
                            rmRecursive(entry.getFcbId());
                            // Borrar entrada
                            for (int j = idx; j < currentDirectory.getSizeUsed() - 1; j++) {
                                long srcPos = offsetRM + ((j + 1) * 24);
                                long dstPos = offsetRM + (j * 24);
                                byte[] nextEntry = fs.disk.read(srcPos, 24);
                                fs.disk.write(dstPos, nextEntry);
                            }
                            currentDirectory.setSizeUsed(currentDirectory.getSizeUsed() - 1);
                            long cleanPos = offsetRM + (currentDirectory.getSizeUsed() * 24);
                            fs.disk.write(cleanPos, new byte[24]);
                        }
                    } else if (parts[2].contains("/")) {
                        // Caso para rutas 
                        int resultId;
                        // Caso absoluta
                        if (parts[2].startsWith("/")) {
                            resultId = findFileByPath(0, parts[2]);
                        } else { // Caso relativa
                            int curId = fs.findFCBID(currentDirectory.getName(), currentDirectory.getParentId());
                            resultId = findFileByPath(curId, parts[2]);
                        }
                        if (resultId == -1) {
                            out.println("No se encontro la ruta");
                            return 0;
                        }
                        rmRecursive(resultId);
                        // Ahora busco quitar al hijo o sea el directorio que borre aribba 
                        String parentPath = parts[2];
                        if (parentPath.endsWith("/")) {
                            parentPath = parentPath.substring(0, parentPath.length() - 1);
                        }
                        // Para eso separo el nombre del directorio y el del archivo 
                        int lastSlash = parentPath.lastIndexOf("/");
                        String targetName = parentPath.substring(lastSlash + 1);
                        String parentDirPath;
                        if (lastSlash <= 0) {
                            parentDirPath = "/";
                        } else {
                            parentDirPath = parentPath.substring(0, lastSlash);
                        }
                        int parentFcbId;
                        if (parentDirPath.startsWith("/")) {
                            parentFcbId = pathHandler(0, parentDirPath); // Busca desde la raiz
                        } else { // buscamos desde el direcotrio actual
                            int curId = fs.findFCBID(currentDirectory.getName(), currentDirectory.getParentId());
                            parentFcbId = pathHandler(curId, parentDirPath);
                        }
                        if (parentFcbId != -1) { // Limpio el archivo hijo sobreescribiendolo con los anteriores
                            FCB parentDir = fs.getFCB(parentFcbId);
                            long parentOffset = fs.superBlock.getDataZoneStart() + (parentDir.getStartBlock() * 512);
                            for (int i = 0; i < parentDir.getSizeUsed(); i++) {
                                byte[] eData = fs.disk.read(parentOffset + (i * 24), 24);
                                DirectoryEntry e = DirectoryEntry.fromBytes(eData);
                                if (e.getName().equals(targetName)) {
                                    for (int j = i; j < parentDir.getSizeUsed() - 1; j++) {
                                        long srcPos = parentOffset + ((j + 1) * 24);
                                        long dstPos = parentOffset + (j * 24);
                                        byte[] nextEntry = fs.disk.read(srcPos, 24);
                                        fs.disk.write(dstPos, nextEntry);
                                    }
                                    parentDir.setSizeUsed(parentDir.getSizeUsed() - 1);
                                    long cleanParentPos = parentOffset + (parentDir.getSizeUsed() * 24);
                                    fs.disk.write(cleanParentPos, new byte[24]);
                                    fs.writeFCB(parentDir, parentFcbId);
                                    if (parentFcbId == currentFcbIdRM) {
                                        currentDirectory.setSizeUsed(parentDir.getSizeUsed());
                                    }
                                    break;
                                }
                            }
                        }
                    } else {
                        // Un solo archivo o directorio
                        String targetName = parts[2];
                        int targetIdx = -1;
                        int targetFcbId = -1;
                        for (int i = 0; i < currentDirectory.getSizeUsed(); i++) {
                            byte[] entryData = fs.disk.read(offsetRM + (i * 24), 24);
                            DirectoryEntry entry = DirectoryEntry.fromBytes(entryData);
                            if (entry.getName().equals(targetName)) {
                                targetIdx = i;
                                targetFcbId = entry.getFcbId();
                                break;
                            }
                        }
                        if (targetFcbId == -1) {
                            out.println("No se encontro: " + targetName);
                            return 0;
                        }
                        rmRecursive(targetFcbId);
                        // Borrar entrada del directorio actual
                        for (int i = targetIdx; i < currentDirectory.getSizeUsed() - 1; i++) {
                            long srcPos = offsetRM + ((i + 1) * 24);
                            long dstPos = offsetRM + (i * 24);
                            byte[] nextEntry = fs.disk.read(srcPos, 24);
                            fs.disk.write(dstPos, nextEntry);
                        }
                        currentDirectory.setSizeUsed(currentDirectory.getSizeUsed() - 1);
                        long cleanPosRM = offsetRM + (currentDirectory.getSizeUsed() * 24);
                        fs.disk.write(cleanPosRM, new byte[24]);
                    }
                    
                    fs.disk.write(fs.superBlock.bitmapBlocksStart, fs.bitmapBlocks.toBytes());
                    fs.writeFCB(currentDirectory, currentFcbIdRM);
                    out.println("Eliminado recursivamente");
                } else{
                    // Caso normal borrar un directorio o txt nada mas 
                    if(parts[1].contains("*")){
                        // caso normal pero para regex
                        // POSIBLE CAMBIO DESPUES PORQUE SIEMPRE HAGO .*
                        // ENTONCES SI ALGUIEN PONE HOLATXT.js lo borraria
                        String regex = parts[1].replace("*", ".*");
                        ArrayList<Integer> matches = new ArrayList<>();
                        long offsetRM = fs.superBlock.getDataZoneStart() + (currentDirectory.getStartBlock() * 512);
                        for (int i = 0; i < currentDirectory.getSizeUsed(); i++) {
                            byte[] entryData = fs.disk.read(offsetRM + (i * 24), 24);
                            DirectoryEntry entry = DirectoryEntry.fromBytes(entryData);
                            if (entry.getName().matches(regex)) {
                                matches.add(i);
                            }
                        }
                        normalRegexRM(matches);
                        
                    } else {
                        // Caso comun y silvestre
                        long dataRM = fs.superBlock.getDataZoneStart();
                        long offsetRM = dataRM + (currentDirectory.getStartBlock() * 512);
                        int fcbIndexRM = -1;
                        int entryIndexRM = -1;
                        for (int i = 0; i < currentDirectory.getSizeUsed(); i++) {
                            byte[] entryData = fs.disk.read(offsetRM + (i * 24), 24);
                            DirectoryEntry entry = DirectoryEntry.fromBytes(entryData);
                            if (entry.getName().equals(parts[1])) {
                                fcbIndexRM = entry.getFcbId();
                                entryIndexRM = i;
                                break;
                            }
                        }
                        if (fcbIndexRM == -1) {
                            out.println("Archivo o directorio no encontrado");
                            return 0;
                        }

                        FCB fcbRM = fs.getFCB(fcbIndexRM);

                        // ver permisos
                        if (!currentUser.getUserName().equals("root")) {
                            int currentIdx = fs.findUser(currentUser.getUserName());
                            if (currentIdx != fcbRM.getOwnerId()) {
                                out.println("No eres root ni dueño");
                                return 0;
                            }
                            int ownerPerm = FCB.getOwnerPerm(fcbRM.getPermissions());
                            if ((ownerPerm & 2) == 0) {
                                out.println("No tienes permiso de escritura");
                                return 0;
                            }
                        }

                        if (fcbRM.getType() == 1 && fcbRM.getSizeUsed() > 0) {
                            out.println("El directorio no esta vacio");
                            return 0;
                        }

                        // quitamos el bloque en donde estaba el fcb 
                        fs.bitmapBlocks.freeBlocks(fcbRM.getStartBlock(), fcbRM.getBlockCount());
                        fs.disk.write(fs.superBlock.bitmapBlocksStart, fs.bitmapBlocks.toBytes());

                        // quitamos al directorio su hijo 
                        int currentFcbIdRM = fs.findFCBID(currentDirectory.getName(), currentDirectory.getParentId());
                        for (int i = entryIndexRM; i < currentDirectory.getSizeUsed() - 1; i++) {
                            long srcPos = offsetRM + ((i + 1) * 24);
                            long dstPos = offsetRM + (i * 24);
                            byte[] nextEntry = fs.disk.read(srcPos, 24);
                            fs.disk.write(dstPos, nextEntry);
                        }
                        currentDirectory.setSizeUsed(currentDirectory.getSizeUsed() - 1);
                        long cleanPosRM = offsetRM + (currentDirectory.getSizeUsed() * 24);
                        fs.disk.write(cleanPosRM, new byte[24]);

                        // Liberamos el campo que teniamos en el fcb
                        fs.disk.write(fs.superBlock.getFcbStart() + (fcbIndexRM * 63), new byte[]{0});

                        // Guardamos el directorio actual para que se vea reflejada la eliminación
                        fs.writeFCB(currentDirectory, currentFcbIdRM);

                        out.println("Eliminado: " + parts[1]);
                    }
                }
                break;
                
            case "mv":
                // buscamos el fcb del archivo
                long dataMV = fs.superBlock.getDataZoneStart();
                int blockDirMV = currentDirectory.getStartBlock();
                long offsetMV = dataMV + (blockDirMV * 512);
                int fcbIndexMV = -1;
                int entryIndexMV = -1;
                for (int i = 0; i < currentDirectory.getSizeUsed(); i++) {
                    byte[] entryData = fs.disk.read(offsetMV + (i * 24), 24);
                    DirectoryEntry entry = DirectoryEntry.fromBytes(entryData);
                    if (entry.getName().equals(parts[1])) {
                        fcbIndexMV = entry.getFcbId();
                        entryIndexMV = i;
                        break;
                    }
                }
                if (fcbIndexMV == -1) {
                    out.println("Archivo no encontrado");
                    return 0;
                }

                // Vemos si es dueño o ususario root 
                FCB fileFCBMV = fs.getFCB(fcbIndexMV);
                if (!currentUser.getUserName().equals("root")) {
                    int currentIdx = fs.findUser(currentUser.getUserName());
                    if (currentIdx != fileFCBMV.getOwnerId()) {
                        out.println("No eres root ni dueño del archivo");
                        return 0;
                    }
                    int ownerPerm = FCB.getOwnerPerm(fileFCBMV.getPermissions());
                    if ((ownerPerm & 2) == 0) {
                        out.println("No tienes permiso de escritura");
                        return 0;
                    }
                }

                if (parts[2].contains("/")) {
                    // Aqui lo movemos de directorio mañnan lo hago
                    int currentFcbId = fs.findFCBID(currentDirectory.getName(), currentDirectory.getParentId());
                    int resultId;
                    if (parts[2].startsWith("/")){
                        resultId = pathHandler(0, parts[2]);
                    } else {
                        resultId = pathHandler(currentFcbId, parts[2]);
                    }
                    // Una vez salga de la función veo si me la dio correcto y si es asi entonces
                    // cambio el archivo a ese directorio
                    if(resultId == -1){
                        out.println("No se encontro el directorio del mv");
                        return 0;
                    }
                    // La misma operación de siempre me situo en el inicio del fcb del directorio
                    FCB finalDir = fs.getFCB(resultId);
                    long dataDest = fs.superBlock.getDataZoneStart();
                    int blockDest = finalDir.getStartBlock();
                    long offsetDest = dataDest + (blockDest * 512);

                    // ponemos el nuevo archivo al final
                    DirectoryEntry newEntry = new DirectoryEntry(parts[1], fcbIndexMV);
                    fs.disk.write(offsetDest + (finalDir.getSizeUsed() * 24), newEntry.toBytes());

                    // aumentar sizeUsed del destino
                    finalDir.setSizeUsed(finalDir.getSizeUsed() + 1);
                    
                    // Sobreescribmos directorio viejo
                    for (int i = entryIndexMV; i < currentDirectory.getSizeUsed() - 1; i++) {
                        long srcPos = offsetMV + ((i + 1) * 24);
                        long dstPos = offsetMV + (i * 24);
                        byte[] nextEntry = fs.disk.read(srcPos, 24);
                        fs.disk.write(dstPos, nextEntry);
                    }

                    currentDirectory.setSizeUsed(currentDirectory.getSizeUsed() - 1);
                    long cleanLastPos = offsetMV + (currentDirectory.getSizeUsed() * 24);
                    fs.disk.write(cleanLastPos, new byte[24]);
                    
                    fileFCBMV.setParentId(resultId);

                    fs.writeFCB(finalDir, resultId);
                    fs.writeFCB(currentDirectory, currentFcbId);
                    fs.writeFCB(fileFCBMV, fcbIndexMV);
                    
                } else {
                    // Veo si no esta el nombre ya
                    for (int i = 0; i < currentDirectory.getSizeUsed(); i++) {
                        byte[] entryData = fs.disk.read(offsetMV + (i * 24), 24);
                        DirectoryEntry entry = DirectoryEntry.fromBytes(entryData);
                        if (entry.getName().equals(parts[2])) {
                            out.println("El nombre ya existe");
                            return 0;
                        }
                    }

                    // Cambiamso en directorio
                    long entryPos = offsetMV + (entryIndexMV * 24);
                    DirectoryEntry renamedEntry = new DirectoryEntry(parts[2], fcbIndexMV);
                    fs.disk.write(entryPos, renamedEntry.toBytes());

                    // Cambiamos en fcb
                    fileFCBMV.setName(parts[2]);
                    fs.writeFCB(fileFCBMV, fcbIndexMV);
                    out.println("Nombre cambiado de " + parts[1] + " a " + parts[2]);
                }
                break;
                
            case "ls":
                currentDirectory = fs.getFCB(currentDirectoryId);
                if (parts.length > 1 && parts[1].equals("-R")) {
                    lsRecursive(currentDirectoryId, buildPath(currentDirectory));
                } else {
                    
                    long dataLs = fs.superBlock.getDataZoneStart();
                    int directionLs = this.currentDirectory.getStartBlock();
                    long offsetLs = dataLs + (directionLs * 512);
                    for(int s = 0; s < this.currentDirectory.getSizeUsed(); s++){
                        byte[] dataEntryLs = fs.disk.read(offsetLs +(s * 24), 24);
                        DirectoryEntry varLs = DirectoryEntry.fromBytes(dataEntryLs);
                        int fcbIDL = varLs.getFcbId();
                        FCB fcLs = fs.getFCB(fcbIDL);
                        if(fcLs.getType() == 1) {
                            out.println("Directorio " + varLs.getName());


                        } else{
                            out.println("Archivo " + varLs.getName());

                        }   

                    }
                }
                
                
                break;
                
            case "cd":
                boolean foundDir = false;
                if(!parts[1].equals("..")){
                    currentDirectory = fs.getFCB(currentDirectoryId);
                    long data = fs.superBlock.getDataZoneStart();
                    int directionDirec = currentDirectory.getStartBlock();
                    long offsetData = data + (directionDirec * 512);
                    for(int p = 0; p < currentDirectory.getSizeUsed(); p++){
                        byte[] dataEntry = fs.disk.read(offsetData + (p * 24), 24);
                        DirectoryEntry var = DirectoryEntry.fromBytes(dataEntry);
                        int fcbCD = var.getFcbId();
                        if(var.getName().equals(parts[1]) && fs.getFCB(fcbCD).getType() == 1){
                            currentDirectoryId = var.getFcbId();
                            currentDirectory = fs.getFCB(currentDirectoryId);
                            foundDir = true;
                            break;
                        }
                    }
                } else {
                    if(currentDirectory.getParentId() != -1){
                        currentDirectoryId = currentDirectory.getParentId();
                        currentDirectory = fs.getFCB(currentDirectoryId);
                        foundDir = true;
                    }
                }
                if (!foundDir) {
                    out.println("No se encontro el directorio");
                }
                break;
            
            case "whereis":
                // Empezamos buscando de raiz SIEMPRE / 
                whereIs(0, "", parts[1]);
                break;
                
                
            case "ln": 
                // Empezamos validando
                if(parts.length > 3){
                    out.println("Comando incorrecto");
                    return 0;
                }
                String originalName = parts[1];
                String destPath = parts[2];
                
                // Encontrar el archivo original
                int originalFcbId;
                if (originalName.contains("/")) {
                    if (originalName.startsWith("/")) {
                        originalFcbId = findFileByPath(0, originalName);
                    } else {
                        int curId = fs.findFCBID(currentDirectory.getName(), currentDirectory.getParentId());
                        originalFcbId = findFileByPath(curId, originalName);
                    }
                } else {
                    // Buscar en el directorio actual
                    long dataLN = fs.superBlock.getDataZoneStart();
                    long offsetLN = dataLN + (currentDirectory.getStartBlock() * 512);
                    originalFcbId = -1;
                    for (int i = 0; i < currentDirectory.getSizeUsed(); i++) {
                        byte[] entryData = fs.disk.read(offsetLN + (i * 24), 24);
                        DirectoryEntry entry = DirectoryEntry.fromBytes(entryData);
                        if (entry.getName().equals(originalName)) {
                            originalFcbId = entry.getFcbId();
                            break;
                        }
                    }
                }
                if (originalFcbId == -1) {
                    out.println("Archivo original no encontrado");
                    return 0;
                }
                
                // Ver permisos de lectura
                FCB fcbOrigin = fs.getFCB(originalFcbId);
                if (!currentUser.getUserName().equals("root")){
                    int ownerPerm = FCB.getOwnerPerm(fcbOrigin.getPermissions());
                    int groupPerm = FCB.getGroupPerm(fcbOrigin.getPermissions());
                    int userIndex = fs.findUser(currentUser.getUserName());
                    int permissions;

                    if (userIndex == fcbOrigin.getOwnerId()) {
                        permissions = ownerPerm;
                    } else if (userBelongsToGroup(userIndex, fcbOrigin.getGroupId())) {
                        permissions = groupPerm;
                    } else {
                        out.println("No eres dueño ni perteneces al grupo del archivo");
                        return 0;
                    }

                    if ((permissions & 4) == 0) {
                        out.println("No tienes permiso de lectura");
                        return 0;
                    }                    
                }
                
                // Parsear destino
                int parentFcbId;
                String linkName;
                if (destPath.contains("/")) {
                    // Separar directorio padre + nombre del enlace
                    String tempDest = destPath;
                    if (tempDest.endsWith("/")) {
                        tempDest = tempDest.substring(0, tempDest.length() - 1);
                    }
                    int lastSlash = tempDest.lastIndexOf("/");
                    linkName = tempDest.substring(lastSlash + 1);
                    String parentDirPath;
                    if (lastSlash <= 0) {
                        parentDirPath = "/";
                    } else {
                        parentDirPath = tempDest.substring(0, lastSlash);
                    }
                    if (parentDirPath.startsWith("/")) {
                        parentFcbId = pathHandler(0, parentDirPath);
                    } else {
                        int curId = fs.findFCBID(currentDirectory.getName(), currentDirectory.getParentId());
                        parentFcbId = pathHandler(curId, parentDirPath);
                    }
                } else {
                    // En el directorio actual
                    linkName = destPath;
                    parentFcbId = fs.findFCBID(currentDirectory.getName(), currentDirectory.getParentId());
                }
                if (parentFcbId == -1) {
                    out.println("Directorio destino no encontrado");
                    return 0;
                }
                
                // Crear el enlace simbolico
                int freeSlotLN = fs.freeslotFCB();
                if (freeSlotLN == -1) {
                    out.println("No hay espacio para mas FCBs");
                    return 0;
                }
                int freeBlockLN = fs.bitmapBlocks.findFreeBit();
                if (freeBlockLN == -1) {
                    out.println("No hay bloques libres");
                    return 0;
                }
                fs.bitmapBlocks.markBusy(freeBlockLN);
                
                // Guardar la ruta original como contenido del enlace
                long dataBlockLN = fs.superBlock.getDataZoneStart() + (freeBlockLN * 512);
                byte[] linkContent = originalName.getBytes();
                fs.disk.write(dataBlockLN, linkContent);
                
                // Crear FCB tipo 2
                int userIdxLN = fs.findUser(currentUser.getUserName());
                int groupIdxLN = currentUser.getGroupId();
                FCB linkFCB = new FCB(linkName, (byte)2, userIdxLN, groupIdxLN, FCB.grantPerm(6,4), 
                    linkContent.length, freeBlockLN, 1, System.currentTimeMillis(), System.currentTimeMillis(), (byte)0, parentFcbId);
                fs.writeFCB(linkFCB, freeSlotLN);
                fs.disk.write(fs.superBlock.getBitmapBlocksStart(), fs.bitmapBlocks.toBytes());
                
                // Agregar entrada al directorio padre
                FCB parentDirLN = fs.getFCB(parentFcbId);
                long parentOffsetLN = fs.superBlock.getDataZoneStart() + (parentDirLN.getStartBlock() * 512);
                DirectoryEntry linkEntry = new DirectoryEntry(linkName, freeSlotLN);
                fs.disk.write(parentOffsetLN + (parentDirLN.getSizeUsed() * 24), linkEntry.toBytes());
                parentDirLN.setSizeUsed(parentDirLN.getSizeUsed() + 1);
                fs.writeFCB(parentDirLN, parentFcbId);
                
                out.println("Enlace creado: " + linkName);
                break;
                
                
        
            case "touch":
                out.println("Vamos a hacer un touch");
                
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
                            out.println("El archivo ya existe actualmente");
                            return 0;
                        } 
                    }

                    //Busco FCB libre
                   
                    int freeHoleFCB = fs.freeslotFCB();
                    if (freeHoleFCB == -1 ){
                        out.println("No queda espacio para más FCBS");
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
                    FCB fcb3 = new FCB(nameFCB3, (byte)0, idUserFCB3, idGroupFCB3, FCB.grantPerm(6,4), 0, newBlockT, 1, System.currentTimeMillis(),
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
                    out.println("Ingrese el nombre del archivo");
                    return 0;
                } else {
                    out.println("Ingrese correctamente el comando");
                    return 0;
                }
                break;
                
            case "cat":
                if (parts.length < 2) {
                    out.println("Ingrese el nombre del archivo");
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
                    out.println("Archivo no encontrado");
                    return 0;
                }
                
                FCB fileFCB2 = fs.getFCB(fcbIndex2);
                
                // Por si tiene enlace simbolo
                if (fileFCB2.getType() == 2) {
                    byte[] linkData2 = fs.readFileData(fileFCB2);
                    String linkPath2 = new String(linkData2).trim();
                    int realId2 = findFileByPath(0, linkPath2);
                    if (realId2 == -1) {
                        out.println("El enlace apunta a un archivo inexistente");
                        return 0;
                    }
                    fileFCB2 = fs.getFCB(realId2);
                }
                
                if (!currentUser.getUserName().equals("root")){
                    int ownerPerm = FCB.getOwnerPerm(fileFCB2.getPermissions());
                    int groupPerm = FCB.getGroupPerm(fileFCB2.getPermissions());
                    int userIndex = fs.findUser(currentUser.getUserName());
                    int permissions;

                    if (userIndex == fileFCB2.getOwnerId()) {
                        permissions = ownerPerm;
                    } else if (userBelongsToGroup(userIndex, fileFCB2.getGroupId())) {
                        permissions = groupPerm;
                    } else {
                        out.println("No eres dueño ni perteneces al grupo del archivo");
                        return 0;
                    }

                    if ((permissions & 4) == 0) {
                        out.println("No tienes permiso de lectura");
                        return 0;
                    }                    
                }
                
                // pasa validaciones y entonces leemos el contenido
                byte[] fileInfo = fs.readFileData(fileFCB2);
                String content = new String(fileInfo, 0, fileFCB2.getSizeUsed());
                out.println(content);
                
                break;
                
            case "less":
                if (parts.length < 2) {
                    out.println("Ingrese el nombre del archivo");
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
                    out.println("Archivo no encontrado");
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
                        out.println(lessLines[i]);
                    }
                    out.println("-- " + (lineIndex + 1) + "/" + lessLines.length + "  q -> salir  Enter -> sig  espacio -> pag  - ->atras");

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
                
                
            case "chown":
                // Revisamos usuario
                if (!this.currentUser.getUserName().equals("root")) {
                    out.println("Para usar el comando debe ser root");
                    return 0;
                }
                
                // Revisamos si va hacer recursivo o no para obtener el usuario y archivo/directorio
                boolean recursiveC = false;
                String userNameChown;
                String fileNameChown;

                if (parts[1].equals("-R")) {
                    recursiveC = true;
                    userNameChown = parts[2];
                    fileNameChown = parts[3];
                } else {
                    userNameChown = parts[1];
                    fileNameChown = parts[2];
                }

                int userIndexC = fs.findUser(userNameChown);
                if (userIndexC == -1) {
                    out.println("Usuario no encontrado");
                    return 0;
                }

                // Buscamos el archivo en el directorio actual
                long dataC = fs.superBlock.getDataZoneStart();
                int blockDirC = currentDirectory.getStartBlock();
                long offsetC = dataC + (blockDirC * 512);

                int fcbIndexC = -1;
                for (int i = 0; i < currentDirectory.getSizeUsed(); i++) {
                    byte[] entryDataC = fs.disk.read(offsetC + (i * 24), 24);
                    DirectoryEntry entryC = DirectoryEntry.fromBytes(entryDataC);
                    if (entryC.getName().equals(fileNameChown)) {
                        fcbIndexC = entryC.getFcbId();
                        break;
                    }
                }
                if (fcbIndexC == -1) {
                    out.println("Archivo no encontrado");
                    return 0;
                }

                
                if (recursiveC) {
                    // Caso recursivo solo le pasamos el indice del fcb y el del usuario
                    FCB dirFCB = fs.getFCB(fcbIndexC);
                    if (dirFCB.getType() != 1) {
                        out.println("-R solo aplica para directorios");
                        return 0;
                    }
                    chownRecursive(fcbIndexC, userIndexC);
                    out.println("Dueño cambiado a " + userNameChown + " (recursivo)");
                } else {
                    // Caso normal se actualiza directamente y escribimos en disco
                    FCB fileFCBC = fs.getFCB(fcbIndexC);
                    fileFCBC.setOwnerId(userIndexC);
                    out.println("Dueño cambiado a " + userNameChown);
                    fs.writeFCB(fileFCBC, fcbIndexC);
                }

                break;
                
            case "chgrp":
                if (parts.length < 3) {
                    out.println("Uso: chgrp [-R] grupo archivo");
                    return 0;
                }

                boolean itsRecursive = false;
                String groupNameG;
                String fileOrDirec;

                if (parts[1].equals("-R")) {
                    if (parts.length < 4) {
                        out.println("Uso: chgrp -R grupo archivo");
                        return 0;
                    }
                    itsRecursive = true;
                    groupNameG = parts[2];
                    fileOrDirec = parts[3];
                } else {
                    groupNameG = parts[1];
                    fileOrDirec = parts[2];
                }

                int groupIndexG = fs.findGroup(groupNameG);
                if (groupIndexG == -1) {
                    out.println("Grupo no encontrado");
                    return 0;
                }
                Group groupC = fs.getGroup(groupIndexG);

                // Buscamos el archivo en el directorio actual
                long dataG = fs.superBlock.getDataZoneStart();
                int blockDirG = currentDirectory.getStartBlock();
                long offsetG = dataG + (blockDirG * 512);
                int fcbIndexG = -1;
                for (int i = 0; i < currentDirectory.getSizeUsed(); i++) {
                    byte[] entryData = fs.disk.read(offsetG + (i * 24), 24);
                    DirectoryEntry entry = DirectoryEntry.fromBytes(entryData);
                    if (entry.getName().equals(fileOrDirec)) {
                        fcbIndexG = entry.getFcbId();
                        break;
                    }
                }

                if (fcbIndexG == -1) {
                    out.println("Archivo no encontrado");
                    return 0;
                }

                FCB fileFCBG = fs.getFCB(fcbIndexG);

                // Vemos si es root y sino los permisos del dueño
                if (!currentUser.getUserName().equals("root")) {
                    int currentUserIdx = fs.findUser(currentUser.getUserName());
                    boolean isTheOwner = (currentUserIdx == fileFCBG.getOwnerId());
                    boolean inGroup = false;
                    for (int m : groupC.getMembers()) {
                        if (m == currentUserIdx) { inGroup = true; break; }
                    }
                    if (!isTheOwner || !inGroup) {
                        out.println("No tienes permisos para cambiar el grupo");
                        return 0;
                    }
                }

                if (itsRecursive) {
                        //Caso recursivo
                        FCB dirFCB = fs.getFCB(fcbIndexG);
                        if (dirFCB.getType() != 1) {
                            out.println("-R solo aplica para directorios");
                            return 0;
                        }
                        chgrpRecursive(fcbIndexG, groupIndexG);
                        out.println("Grupo cambiado a " + groupNameG + " (recursivo)");
                } else {
                    // Caso normal lo actualizo directo
                    fileFCBG.setGroupId(groupIndexG);
                    fs.writeFCB(fileFCBG, fcbIndexG);
                    out.println("Grupo cambiado a " + groupNameG);
                }
                break;
                
            case "chmod": {
                String perms = parts[1];
                int numberPerms = Integer.parseInt(perms);
                int userPerm = numberPerms / 10;
                int groupPermMod = numberPerms % 10;
                String fileNameMod = parts[2];
                
                // Buscamos el archivo 
                long dataMod = fs.superBlock.getDataZoneStart();
                int blockDirMod = currentDirectory.getStartBlock();
                long offsetMod = dataMod + (blockDirMod * 512);
                int fcbIndexMod = -1;
                for (int i = 0; i < currentDirectory.getSizeUsed(); i++) {
                    byte[] entryData = fs.disk.read(offsetMod + (i * 24), 24);
                    DirectoryEntry entry = DirectoryEntry.fromBytes(entryData);
                    if (entry.getName().equals(fileNameMod)) {
                        fcbIndexMod = entry.getFcbId();
                        break;
                    }
                }

                if (fcbIndexMod == -1) {
                    out.println("Archivo no encontrado");
                    return 0;
                }

                FCB fileFCBM = fs.getFCB(fcbIndexMod);

                int ownerModID = fileFCBM.getOwnerId();
                User userMod = fs.getUser(ownerModID);

                // Valido que sea el dueño del archivo o el usuario root 
                if(!currentUser.getUserName().equals(userMod.getUserName()) && !currentUser.getUserName().equals("root")){
                    out.println("No puedes dar permisos del archivo si no eres dueño o root");
                    return 0;
                }
                // Guardo los permisos de usuarios en los 3 bits altos y lo demas en el restatnte
                // asi seteamos los permisos de larchivo
                byte permissions = (byte)((userPerm << 3) | groupPermMod);
                fileFCBM.setPermissions(permissions);
                fs.writeFCB(fileFCBM, fcbIndexMod);
                out.println("Se dieron permisos de usuario: " + userPerm + "y de grupo: " + groupPermMod);
                break;
            }
              

            case "viewfilesopen":
                int totalOpenFiles = fs.bitmapOpenFiles.bitmap.cardinality();
                out.println("Archivos abiertos actualmente: " + totalOpenFiles);
                break;
                
            case "viewfcb": 
                out.println("Información del FCB del: " + parts[1]);
                long dataFCBV = fs.superBlock.getDataZoneStart();
                int blockFCBV = currentDirectory.getStartBlock();
                long offsetFCBV = dataFCBV + (blockFCBV * 512);
                int indexFCBV = -1;
                for (int i = 0; i < currentDirectory.getSizeUsed(); i++){
                    byte[] entryFCBV = fs.disk.read(offsetFCBV + (i * 24),  24);
                    DirectoryEntry entry = DirectoryEntry.fromBytes(entryFCBV);
                    if (entry.getName().equals(parts[1])) {
                        indexFCBV = entry.getFcbId();
                        break;
                    } 
                }
                if (indexFCBV == -1) {
                    out.println("Archivo no encontrado");
                    return 0;
                } 
                FCB fileFCBV = fs.getFCB(indexFCBV);
                User userFileFcb = fs.getUser(fileFCBV.getOwnerId());
                Group groupFCb = fs.getGroup(fileFCBV.getGroupId());
                String groupNameFC = groupFCb.getGroupName();
                String usName = userFileFcb.getUserName();
                String parentName;
                if (fileFCBV.getParentId() == -1) {
                    parentName = "/";
                } else {
                    parentName = fs.getFCB(fileFCBV.getParentId()).getName();
                }
                
                
                int userPerm2 = FCB.getOwnerPerm(fileFCBV.getPermissions());
                int groupPerm2 = FCB.getGroupPerm(fileFCBV.getPermissions());
                String isOpen;
                if (fileFCBV.getIsOpen() == 1){
                    isOpen = "abierto";
                } else {
                    isOpen = "cerrado";                  
                }
                
                out.println("Nombre: " + fileFCBV.getName());
                out.println("Padre: " + parentName);
                if(fileFCBV.getType() == 1){
                    out.println("Tipo de archivo: " + "es un directorio");
                    out.println("Dueño del directorio: " + usName);
                    out.println("Grupo al que pertenece: " + groupNameFC);
                    out.println("Permisos de usuario: " + userPerm2 + "permisos de grupo: " + groupPerm2);
                    out.println("Bytes utilizados: " + fileFCBV.getSizeUsed());
                    out.println("Bloque inicial: " + fileFCBV.getStartBlock());
                    out.println("Cantidad de bloques" + fileFCBV.getBlockCount());
                    out.println("Fecha de creación: " + formatDate(fileFCBV.getCreationDate()));
                    out.println("Fecha de modificacion: " + formatDate(fileFCBV.getModificationDate()));
                    out.println("Directorio padre: " + parentName);                 
                    break;
                }
                out.println("Tipo de archivo: " + "es un archivo");
                out.println("Dueño del archivo: " + usName);
                out.println("Grupo al que pertenece: " + groupNameFC);
                out.println("Permisos de usuario: " + userPerm2 + "permisos de grupo: " + groupPerm2);
                out.println("Tamano utilizado: " + fileFCBV.getSizeUsed());
                out.println("Bloque inicial: " + fileFCBV.getStartBlock());
                out.println("Cantidad de bloques" + fileFCBV.getBlockCount());
                out.println("Fecha de creación: " + formatDate(fileFCBV.getCreationDate()));
                out.println("Fecha de modificacion: " + formatDate(fileFCBV.getModificationDate()));                
                out.println("El archivo se encuentra " + isOpen);
                out.println("Archivo padre: " + parentName);
                break;
                
            case "infofs":
                long totalBytesFS = fs.mbr.volumeSize;
                long metaBytesFS = fs.superBlock.getDataZoneStart();
                long dataUsedBytesFS = fs.bitmapBlocks.countUsedBlocks() * 512;
                long usedBytesFS = metaBytesFS + dataUsedBytesFS;
                long freeBytesFS = totalBytesFS - usedBytesFS;
                out.println("Nombre del FileSystem: " + fs.mbr.fsFileName);
                out.println("Tamaño: " + (totalBytesFS / (1024 * 1024)) + " Mb");
                out.println("Espacio utilizado: " + (usedBytesFS / 1024) + " KB");
                out.println("Disponible: " + (freeBytesFS / 1024) + " KB");
                break;
                

                
            case "note":             
                if (parts.length < 2) {
                    out.println("Ingrese el nombre del archivo");
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
                    out.println("Archivo no encontrado");
                    return 0;
                }

                FCB fileFCB = fs.getFCB(fcbIndex);

                // Resolver enlace simbolico
                if (fileFCB.getType() == 2) {
                    byte[] linkData = fs.readFileData(fileFCB);
                    String linkPath = new String(linkData).trim();
                    int realId = findFileByPath(0, linkPath);
                    if (realId == -1) {
                        out.println("El enlace apunta a un archivo inexistente");
                        return 0;
                    }
                    fileFCB = fs.getFCB(realId);
                }

                // vemos si el usuario tiene permisos
                if (!currentUser.getUserName().equals("root")) {
                    int ownerPerm = FCB.getOwnerPerm(fileFCB.getPermissions());
                    int groupPerm = FCB.getGroupPerm(fileFCB.getPermissions());
                    int userIndex = fs.findUser(currentUser.getUserName());
                    int permissions2;

                    if (userIndex == fileFCB.getOwnerId()) {
                        permissions2 = ownerPerm;
                    } else if (userBelongsToGroup(userIndex, fileFCB.getGroupId())) {
                        permissions2 = groupPerm;
                    } else {
                        out.println("No eres dueño ni perteneces al grupo del archivo");
                        return 0;
                    }

                    if ((permissions2 & 4) == 0) {
                        out.println("No tienes permiso de lectura");
                        return 0;
                    }
                    if ((permissions2 & 2) == 0) {
                        out.println("No tienes permiso de escritura");
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
                    out.println("Archivo guardado");
                } else {
                    out.println("No se guardaron cambios");
                }
                break;
                
                
            
                
            
        }
        
        
        return 0;
    }

    private void chownRecursive(int dirFcbId, int newOwnerId) {
        FCB dirFCB = fs.getFCB(dirFcbId);
        dirFCB.setOwnerId(newOwnerId);
        fs.writeFCB(dirFCB, dirFcbId);

        // Leer entradas del directorio
        byte[] dirData = fs.readFileData(dirFCB);
        int numEntries = dirFCB.getSizeUsed();

        for (int i = 0; i < numEntries; i++) {
            byte[] entryContent = Arrays.copyOfRange(dirData, i * 24, i * 24 + 24);
            DirectoryEntry entry = DirectoryEntry.fromBytes(entryContent);
            String name = entry.getName();
            out.println("Nombre de archivo" + name);


            FCB childFCB = fs.getFCB(entry.getFcbId());
            childFCB.setOwnerId(newOwnerId);
            fs.writeFCB(childFCB, entry.getFcbId());

            // En caso de ser directorio lo volvemos a ejecutar para la 
            // recursividad 
            if (childFCB.getType() == 1) {
                chownRecursive(entry.getFcbId(), newOwnerId);
            }
        }
    }

    private void chgrpRecursive(int dirFcbId, int newGroupId) {
        FCB dirFCB = fs.getFCB(dirFcbId);
        dirFCB.setGroupId(newGroupId);
        fs.writeFCB(dirFCB, dirFcbId);

        // Leer entradas del directorio
        byte[] dirData = fs.readFileData(dirFCB);
        int numEntries = dirFCB.getSizeUsed();

        for (int i = 0; i < numEntries; i++) {
            byte[] entryContent= Arrays.copyOfRange(dirData, i * 24, i * 24 + 24);
            DirectoryEntry entry = DirectoryEntry.fromBytes(entryContent);
            String name = entry.getName();
            out.println("Nombre de archivo" + name);


            FCB childFCB = fs.getFCB(entry.getFcbId());
            childFCB.setGroupId(newGroupId);
            fs.writeFCB(childFCB, entry.getFcbId());

            // En caso de ser directorio lo volvemos a ejecutar para la 
            // recursividad 
            if (childFCB.getType() == 1) {
                chgrpRecursive(entry.getFcbId(), newGroupId);
            }
        }
    }
    
    private void lsRecursive(int dirFCBFF, String path){
        FCB dirFCB = fs.getFCB(dirFCBFF);
        out.println(path + ":");

        byte[] dirData = fs.readFileData(dirFCB);
        int numEntries = dirFCB.getSizeUsed();

        for (int i = 0; i < numEntries; i++) {
            byte[] entryContent= Arrays.copyOfRange(dirData, i * 24, i * 24 + 24);
            DirectoryEntry entryFF2 = DirectoryEntry.fromBytes(entryContent);
            int fcbIDL = entryFF2.getFcbId();
            FCB fcLs = fs.getFCB(fcbIDL);
            if(fcLs.getType() == 1) {
                out.println("Directorio " + entryFF2.getName());
            } else{
                out.println("Archivo " + entryFF2.getName());
            } 

            FCB childFCB = fs.getFCB(entryFF2.getFcbId());
            if (childFCB.getType() == 1) {
                lsRecursive(entryFF2.getFcbId(), path + "/" + entryFF2.getName());
            }
        }
        out.println();
    }
  
    
    private int pathHandler(int DirFCBP, String path){
        String[] segments = path.split("/");
        int currentFcbId = DirFCBP;
        // Aqui vamos descomponiendo la ruta parte por parte para ver si la armamos
        // de poquito a poquito
        for (String seg : segments) {
            if (seg.isEmpty()) continue;
            FCB dirFCB = fs.getFCB(currentFcbId);
            byte[] dirData = fs.readFileData(dirFCB);
            int numEntries = dirFCB.getSizeUsed();
            int found = -1;
            
            for (int i = 0; i < numEntries; i++) {
                byte[] entryContent= Arrays.copyOfRange(dirData, i * 24, i * 24 + 24);
                DirectoryEntry entryFF2 = DirectoryEntry.fromBytes(entryContent);
                FCB child = fs.getFCB(entryFF2.getFcbId());
                if(child.getName().equals(seg) && child.getType() == 1){
                    // Se encuentra una parte del directorio
                    found = entryFF2.getFcbId();
                    break;
                }
            }
            if (found == -1) return -1; 
            currentFcbId = found;
        }
        return currentFcbId;        
    }
    
    
    private int findFileByPath(int startFcbId, String path) {
        String[] segments = path.split("/");
        int currentFcbId = startFcbId;
        for (String seg : segments) {
            if (seg.isEmpty()) continue;
            FCB dirFCB = fs.getFCB(currentFcbId);
            byte[] dirData = fs.readFileData(dirFCB);
            int numEntries = dirFCB.getSizeUsed();
            int found = -1;
            for (int i = 0; i < numEntries; i++) {
                byte[] entryContent = Arrays.copyOfRange(dirData, i * 24, i * 24 + 24);
                DirectoryEntry entry = DirectoryEntry.fromBytes(entryContent);
                FCB child = fs.getFCB(entry.getFcbId());
                if (child.getName().equals(seg)) {
                    found = entry.getFcbId();
                    break;
                }
            }
            if (found == -1) return -1;
            currentFcbId = found;
        }
        return currentFcbId;
    }    
    

    
    private String buildPath(FCB dir) {
        if (dir.getParentId() == -1) return "";
        return buildPath(fs.getFCB(dir.getParentId())) + "/" + dir.getName();
    }

    private boolean userBelongsToGroup(int userIdx, int groupIdx) {
        Group g = fs.getGroup(groupIdx);
        for (int m : g.getMembers()) {
            if (m == userIdx) return true;
        }
        return false;
    }

    private String formatDate(long millis) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        return sdf.format(new Date(millis));
    }

    
    private int normalRM(String dataToFind){
        long data = fs.superBlock.getDataZoneStart();
        int blockDir = currentDirectory.getStartBlock();
        long offset = data + (blockDir * 512);

        int fcbIndex = -1;
        for (int i = 0; i < currentDirectory.getSizeUsed(); i++) {
            byte[] entryData = fs.disk.read(offset + (i * 24), 24);
            DirectoryEntry entry = DirectoryEntry.fromBytes(entryData);
            if (entry.getName().equals(dataToFind)) {
                fcbIndex = entry.getFcbId();
                return fcbIndex;
            }
        }       
        return -1;

    }
    
    
    
    private int normalRegexRM(ArrayList<Integer> matches){
        
        long dataRM = fs.superBlock.getDataZoneStart();
        long offsetRM = dataRM + (currentDirectory.getStartBlock() * 512);
        int currentFcbIdRM = fs.findFCBID(currentDirectory.getName(), currentDirectory.getParentId());
        for (int i = matches.size() - 1; i >= 0; i--) {
            int entryID = matches.get(i);
            long entryRM = offsetRM + (entryID * 24);
            byte[] entryData = fs.disk.read(entryRM, 24);
            DirectoryEntry entry = DirectoryEntry.fromBytes(entryData);
            int fcbId = entry.getFcbId();

            if (fcbId == -1) {
                out.println("Archivo o directorio no encontrado");
                continue;
            }

            FCB fcbRM = fs.getFCB(fcbId);           
           
            if (!currentUser.getUserName().equals("root")) {
                int currentIdx = fs.findUser(currentUser.getUserName());
                if (currentIdx != fcbRM.getOwnerId()) {
                    out.println("No eres root ni dueño, se omite: " + entry.getName());
                    continue;
                }
                int ownerPerm = FCB.getOwnerPerm(fcbRM.getPermissions());
                if ((ownerPerm & 2) == 0) {
                    out.println("No tienes permiso de escritura, se omite: " + entry.getName());
                    continue;
                }
            }

            if (fcbRM.getType() == 1 && fcbRM.getSizeUsed() > 0) {
                out.println("Directorio no vacio, se omite: " + entry.getName());
                continue;
            }

            // Liberamos los bloques del fcb donde tiene su contenido
            fs.bitmapBlocks.freeBlocks(fcbRM.getStartBlock(), fcbRM.getBlockCount());

            // Sobreescribimos las entradas para el padre
            for (int j = entryID; j < currentDirectory.getSizeUsed() - 1; j++) {
                long srcPos = offsetRM + ((j + 1) * 24);
                long dstPos = offsetRM + (j * 24);
                byte[] nextEntry = fs.disk.read(srcPos, 24);
                fs.disk.write(dstPos, nextEntry);
            }
            currentDirectory.setSizeUsed(currentDirectory.getSizeUsed() - 1);
            long cleanPosRM = offsetRM + (currentDirectory.getSizeUsed() * 24);
            fs.disk.write(cleanPosRM, new byte[24]);

            // Quitamos el fcb donde estaba el archivo
            fs.disk.write(fs.superBlock.getFcbStart() + (fcbId * 63), new byte[]{0});

            out.println("Eliminado: " + entry.getName());
        }

        // Todos los cambios hechos en los bloques los guardamos hasta el final
        fs.disk.write(fs.superBlock.bitmapBlocksStart, fs.bitmapBlocks.toBytes());
        fs.writeFCB(currentDirectory, currentFcbIdRM);
        return 0;
    }
    
    private void rmRecursive(int fcbId) {
        FCB fcb = fs.getFCB(fcbId);
        // Si es directorio vamos a ir borrando sus hijos
        if (fcb.getType() == 1) {
            long offsetDir = fs.superBlock.getDataZoneStart() + (fcb.getStartBlock() * 512);
            // Leer sizeUsed antes de modificar
            int totalEntries = fcb.getSizeUsed();
            for (int i = totalEntries - 1; i >= 0; i--) {
                byte[] entryData = fs.disk.read(offsetDir + (i * 24), 24);
                DirectoryEntry entry = DirectoryEntry.fromBytes(entryData);
                rmRecursive(entry.getFcbId());
            }
        }
        // Liberar bloques del archivo o directorio
        fs.bitmapBlocks.freeBlocks(fcb.getStartBlock(), fcb.getBlockCount());
        // Liberar slot del FCB
        fs.disk.write(fs.superBlock.getFcbStart() + (fcbId * 63), new byte[]{0});
    }
    
    
    
    private void whereIs(int dirFcbId, String path, String fileName){
        FCB dirFCB = fs.getFCB(dirFcbId);

        byte[] dirData = fs.readFileData(dirFCB);
        int numEntries = dirFCB.getSizeUsed();

        for (int i = 0; i < numEntries; i++) {
            byte[] entryContent= Arrays.copyOfRange(dirData, i * 24, i * 24 + 24);
            DirectoryEntry entryFF2 = DirectoryEntry.fromBytes(entryContent);
            int fcbIDL = entryFF2.getFcbId();
            FCB fcLs = fs.getFCB(fcbIDL);

            if(fcLs.getName().equals(fileName)){
                out.println(path + "/" + entryFF2.getName());
            }
            
            FCB childFCB = fs.getFCB(entryFF2.getFcbId());
            if (childFCB.getType() == 1) {
                whereIs(entryFF2.getFcbId(), path + "/" + entryFF2.getName(), fileName);
            }
        }
        return;
    }    
    
    
    
}

