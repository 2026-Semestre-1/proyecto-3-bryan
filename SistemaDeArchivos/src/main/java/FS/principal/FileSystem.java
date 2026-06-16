/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package FS.principal;
import FS.structures.*;

/**
 *
 * @author bryan
 */
public class FileSystem {
    public Disk disk;
    public Mbr mbr;
    public SuperBlock superBlock;
    public Bitmap bitmapBlocks;
    public Bitmap bitmapOpenFiles;
    
    public FileSystem(){
        
    }
    
    
    
    public User getUser(int index){
        long offset = superBlock.getUsersStart() + (index * 90);
        byte[] data = disk.read(offset, 90);
        return User.fromBytes(data);
    }
    
    public Group getGroup(int index){
        long offset = superBlock.getUsersStart() + (index * 55);
        byte[] data = disk.read(offset, 55);
        return Group.fromBytes(data);
    }    

    public FCB getFCB(int index){
        long offset = superBlock.getFcbStart() + (index * 63);
        byte[] data = disk.read(offset, 63);
        return FCB.fromBytes(data);
    }    
    
    public void createDisk(long file, String type ,String fileName, String rootPassword){
        long sizeBytes = 0;
        type = type.toLowerCase();
        if (type.equals("mb")){
            sizeBytes = file * 1024 * 1024;
        } else {
            if (type.equals("kb")) {
                sizeBytes = file * 1024;
            }
        }
        
        Mbr mbr = new Mbr("miFS", sizeBytes, 0);
        long blockSize = 512; // Lo pongo así por si lo cambio
        SuperBlock superBlock = new SuperBlock(sizeBytes, blockSize);

        long sizeMBR = 200;
        long sizeSuperBlock = 80;
        long sizeBitmapOpenFiles = 10; // 80 archivos abiertos
        long sizeUsuarios = 900;        // 90 bytes × 10 usuarios
        long sizeGrupos = 275;          // 55 bytes × 5 grupos
        long sizeFCB = 6300;            // 63 bytes × 100 FCBs
        
        

        long calculatedData = sizeMBR + sizeSuperBlock + sizeBitmapOpenFiles + sizeUsuarios + sizeGrupos + sizeFCB;
        long remainingSpace = sizeBytes - calculatedData;

        long estimatedBlocks = remainingSpace / blockSize;
        long sizeBitmapBlocks = (long) Math.ceil(estimatedBlocks / 8.0);

        long dataSpace = remainingSpace - sizeBitmapBlocks;
        long totalBlocks = dataSpace / blockSize;        
        
        
        long offsetMBR = 0;
        long offsetSuperBlock = offsetMBR + sizeMBR;
        long offsetBitmapBloques = offsetSuperBlock + sizeSuperBlock;
        long offsetBitmapOpenFiles = offsetBitmapBloques + sizeBitmapBlocks;
        long offsetUsuarios = offsetBitmapOpenFiles + sizeBitmapOpenFiles;
        long offsetGrupos = offsetUsuarios + sizeUsuarios;
        long offsetFCB = offsetGrupos + sizeGrupos;
        long offsetDataZone = offsetFCB + sizeFCB;
        
        superBlock.setTotalBlocks(totalBlocks);
        superBlock.setBitmapBlocksStart(offsetBitmapBloques);
        superBlock.setBitmapOpenFilesStart(offsetBitmapOpenFiles);
        superBlock.setUsersStart(offsetUsuarios);
        superBlock.setGroupsStart(offsetGrupos);
        superBlock.setFcbStart(offsetFCB);
        superBlock.setDataZoneStart(offsetDataZone);
        
        
        Disk disk = new Disk(fileName);
        disk.write(0, mbr.toBytes());
        disk.write(offsetSuperBlock, superBlock.toBytes());
        
        // Creamos los bitmaps para los bloques y los archivos abiertos
        // Los guardamos de una vez
        Bitmap bitmapBlocks = new Bitmap((int) totalBlocks);
        Bitmap bitmapOpenFiles = new Bitmap(80);
        disk.write(offsetBitmapBloques, bitmapBlocks.toBytes());
        disk.write(offsetBitmapOpenFiles, bitmapOpenFiles.toBytes());
        
        disk.write(offsetUsuarios, new byte[(int) sizeUsuarios]);
        disk.write(offsetGrupos, new byte[(int) sizeGrupos]);
        disk.write(offsetFCB, new byte[(int) sizeFCB]);
        
        // Creamos al grupo root y le asignamos el indice 0 para root
        Group rootGroup = new Group("root", new int[]{0});
        disk.write(offsetGrupos, rootGroup.toBytes());
        
        // Creación del FCB
        int rootBlock = bitmapBlocks.findFreeBit();
        bitmapBlocks.markBusy(rootBlock);
        
        FCB rootFCB = new FCB(
        "/",
        (byte) 1,
        0,
        0,
        FCB.grantPerm(7,0),
        0,
        rootBlock,
        1,
        System.currentTimeMillis(),
        System.currentTimeMillis(),
        (byte) 0,
        -1
        );
        
        disk.write(offsetFCB, rootFCB.toBytes());
        disk.write(offsetBitmapBloques, bitmapBlocks.toBytes());   
        
        int userRootBlock = bitmapBlocks.findFreeBit();      
        bitmapBlocks.markBusy(userRootBlock);        
        FCB userFCB = new FCB(
        "user",
        (byte) 1,
        0,
        0,
        FCB.grantPerm(7,1),
        0,
        userRootBlock,
        1,
        System.currentTimeMillis(),
        System.currentTimeMillis(),
        (byte) 0,
        0
        );        
        disk.write(offsetFCB + (1 * 63), userFCB.toBytes());
        

        int homeRootBlock = bitmapBlocks.findFreeBit();
        bitmapBlocks.markBusy(homeRootBlock);

        FCB homeRootFCB = new FCB(
            "root",
            (byte) 1,
            0,
            0,
            FCB.grantPerm(7,0),
            0,
            homeRootBlock,
            1,
            System.currentTimeMillis(),
            System.currentTimeMillis(),
            (byte) 0,
            1
        );
        disk.write(offsetFCB + (2 * 63), homeRootFCB.toBytes());    
        disk.write(offsetBitmapBloques, bitmapBlocks.toBytes());
        
        // Creamos usuario root
        User rootUser = new User("root", rootPassword, "root", 0, 2);
        disk.write(offsetUsuarios, rootUser.toBytes());
        

        DirectoryEntry entryUser = new DirectoryEntry("user",1);
        disk.write(offsetDataZone + (0 * blockSize), entryUser.toBytes());
        rootFCB.setSizeUsed(1);
        disk.write(offsetFCB, rootFCB.toBytes());
        
        DirectoryEntry entryRoot = new DirectoryEntry("root",2);       
        disk.write(offsetDataZone + (1 * blockSize), entryRoot.toBytes());
        userFCB.setSizeUsed(1);
        disk.write(offsetFCB + (1 * 63), userFCB.toBytes());        
        this.disk = disk;
        this.superBlock = superBlock;
        this.bitmapBlocks = bitmapBlocks;
        this.bitmapOpenFiles = bitmapOpenFiles;        
        

    }
    
    public int freeSlotUsers(){
        for (int i = 0; i < 10; i++ ){
            long offset = superBlock.getUsersStart() + (i * 90);
            byte[] data = disk.read(offset, 90);
            if(data[0] == 0){
                return i;
            }
        }
        return -1;
        
    }
    
    public int freeslotGroups(){
        for (int j = 0; j < 5; j++ ){
            long offset = superBlock.getGroupsStart() + (j * 55);
            byte[] data = disk.read(offset, 55);
            if(data[0] == 0){
                return j;
            }
        }
        return -1;
        
    }
    
    public int freeslotFCB(){
        for (int x = 0; x < 100; x++ ){
            long offset = superBlock.getFcbStart()+ (x * 63);
            byte[] data = disk.read(offset, 63);
            if(data[0] == 0){
                return x;
            }
        }
        return -1;
        
    }  
    
    public void writeUser(User user, int index){
        long offset = superBlock.getUsersStart() + (index * 90);
        disk.write(offset, user.toBytes());
    }  
    
    public void writeGroup(Group group, int index){
        long offset = superBlock.getGroupsStart() + (index * 55);
        disk.write(offset, group.toBytes());
    }      
    
    public void writeFCB(FCB fcb , int index){
        long offset = superBlock.getFcbStart() + (index * 63);
        disk.write(offset, fcb.toBytes());
    }    
    
    public int findUser(String userName){
        for (int i = 0; i < 10; i++ ){
            if (getUser(i).getUserName().equals(userName)) {
                return i;
            }
        }
        return -1;
        
    }    
 
    public int findGroup(String groupName){
        for (int i = 0; i < 5; i++ ){
            if (getGroup(i).getGroupName().equals(groupName)) {
                return i;
            }
        }
        return -1;
        
    }     
    
    public boolean confirmPasswordUser(String password, int index){
        User u = getUser(index);
        System.out.println("índice: " + index);
        System.out.println("a " + u.getPassword());
        System.out.println("b " + password);
        return u.getPassword().equals(password);
    }
    
    public int findFCBID(String name, int parentID){
        for (int i = 0; i < 100; i++){
            if(getFCB(i).getName().equals(name)){
                if(getFCB(i).getParentId() == parentID){
                    return i;
                }
            }
        }
        return -1;
    }
    
    
}
