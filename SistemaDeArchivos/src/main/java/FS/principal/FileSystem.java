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
    public String diskFileName;
    
    public FileSystem(){
        
    }
    
    public boolean loadDisk(String fileName){
        try {
            Disk d = new Disk(fileName);
            byte[] mbrData = d.read(0, 200);
            Mbr loadedMbr = Mbr.fromBytesToLong(mbrData);
            byte[] sbData = d.read(200, 80);
            SuperBlock loadedSb = SuperBlock.fromBytes(sbData);
            long bitmapBlocksSize = (long) Math.ceil(loadedSb.getTotalBlocks() / 8.0);
            byte[] bmpData = d.read(loadedSb.getBitmapBlocksStart(), (int) bitmapBlocksSize);
            Bitmap loadedBmp = Bitmap.fromBytes(bmpData);
            byte[] bmpOfData = d.read(loadedSb.getBitmapOpenFilesStart(), 10);
            Bitmap loadedBmpOf = Bitmap.fromBytes(bmpOfData);
            this.disk = d;
            this.mbr = loadedMbr;
            this.superBlock = loadedSb;
            this.bitmapBlocks = loadedBmp;
            this.bitmapOpenFiles = loadedBmpOf;
            return true;
        } catch (Exception e) {
            System.err.println("Error al cargar disco: " + e);
            return false;
        }
    }
    
    
    
    public User getUser(int index){
        long offset = superBlock.getUsersStart() + (index * 90);
        byte[] data = disk.read(offset, 90);
        return User.fromBytes(data);
    }
    
    public Group getGroup(int index){
        long offset = superBlock.getGroupsStart() + (index * 55);
        byte[] data = disk.read(offset, 55);
        return Group.fromBytes(data);
    }    

    public FCB getFCB(int index){
        long offset = superBlock.getFcbStart() + (index * 63);
        byte[] data = disk.read(offset, 63);
        return FCB.fromBytes(data);
    }    
    
    public void createDisk(long file, String type, String fileName, String rootPassword){
        long sizeBytes = 0;
        type = type.toLowerCase();
        if (type.equals("mb")){
            sizeBytes = file * 1024 * 1024;
        } else if (type.equals("kb")) {
            sizeBytes = file * 1024;
        } else {
            sizeBytes = file * 1024 * 1024; // default mb
        }
        
        this.mbr = new Mbr("miFS", sizeBytes, 0);
        long blockSize = 512; // Lo pongo así por si lo cambio
        SuperBlock superBlock = new SuperBlock(sizeBytes, blockSize);

        long sizeMBR = 200;
        long sizeSuperBlock = 80;
        long sizeBitmapOpenFiles = 10; // 80 archivos abiertos
        long sizeUsuarios = 2250;        // 90 bytes × 25 usuarios
        long sizeGrupos = 1375;          // 55 bytes × 25 grupos
        long sizeFCB = 6300;            // 63 bytes × 100 FCBs
        
        

        long calculatedData = sizeMBR + sizeSuperBlock + sizeBitmapOpenFiles + sizeUsuarios + sizeGrupos + sizeFCB;
        long remainingSpace = sizeBytes - calculatedData;
        if (remainingSpace < 512) {
            System.out.println("Error: Disco demasiado pequeño. Mínimo ~1MB.");
            return;
        }

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
        disk.write(0, this.mbr.toBytes());
        disk.write(offsetSuperBlock, superBlock.toBytes());
        
        Bitmap bitmapBlocks = new Bitmap((int) totalBlocks);
        Bitmap bitmapOpenFiles = new Bitmap(80);
        disk.write(offsetBitmapBloques, bitmapBlocks.toBytes());
        disk.write(offsetBitmapOpenFiles, bitmapOpenFiles.toBytes());
        
        disk.write(offsetUsuarios, new byte[(int) sizeUsuarios]);
        disk.write(offsetGrupos, new byte[(int) sizeGrupos]);
        disk.write(offsetFCB, new byte[(int) sizeFCB]);
        
        this.disk = disk;
        this.superBlock = superBlock;
        this.bitmapBlocks = bitmapBlocks;
        this.bitmapOpenFiles = bitmapOpenFiles;
        this.diskFileName = fileName;
    }
    
    public boolean isFormatted() {
        if (superBlock == null || disk == null) return false;
        byte[] data = disk.read(superBlock.getFcbStart(), 63);
        for (byte b : data) {
            if (b != 0) return true;
        }
        return false;
    }
    
    public void formatDisk(String password) {
        long offsetUsuarios = superBlock.getUsersStart();
        long offsetGrupos = superBlock.getGroupsStart();
        long offsetFCB = superBlock.getFcbStart();
        long offsetDataZone = superBlock.getDataZoneStart();
        long blockSize = 512;
        
        // Grupo root
        Group rootGroup = new Group("root", new int[]{0});
        disk.write(offsetGrupos, rootGroup.toBytes());
        
        // FCB raiz 
        int rootBlock = bitmapBlocks.findFreeBit();
        bitmapBlocks.markBusy(rootBlock);
        FCB rootFCB = new FCB("/", (byte) 1, 0, 0, FCB.grantPerm(7,5), 0,
            rootBlock, 1, System.currentTimeMillis(), System.currentTimeMillis(), (byte) 0, -1);
        disk.write(offsetFCB, rootFCB.toBytes());
        disk.write(superBlock.getBitmapBlocksStart(), bitmapBlocks.toBytes());
        
        // FCB /user
        int userRootBlock = bitmapBlocks.findFreeBit();
        bitmapBlocks.markBusy(userRootBlock);
        FCB userFCB = new FCB("user", (byte) 1, 0, 0, FCB.grantPerm(7,1), 0,
            userRootBlock, 1, System.currentTimeMillis(), System.currentTimeMillis(), (byte) 0, 0);
        disk.write(offsetFCB + 63, userFCB.toBytes());
        
        // FCB /user/root
        int homeRootBlock = bitmapBlocks.findFreeBit();
        bitmapBlocks.markBusy(homeRootBlock);
        FCB homeRootFCB = new FCB("root", (byte) 1, 0, 0, FCB.grantPerm(7,5), 0,
            homeRootBlock, 1, System.currentTimeMillis(), System.currentTimeMillis(), (byte) 0, 1);
        disk.write(offsetFCB + (2 * 63), homeRootFCB.toBytes());
        disk.write(superBlock.getBitmapBlocksStart(), bitmapBlocks.toBytes());
        
        // Usuario root
        User rootUser = new User("root", password, "root", 0, 2);
        disk.write(offsetUsuarios, rootUser.toBytes());
        
        // directorio
        DirectoryEntry entryUser = new DirectoryEntry("user", 1);
        disk.write(offsetDataZone + (0 * blockSize), entryUser.toBytes());
        rootFCB.setSizeUsed(1);
        disk.write(offsetFCB, rootFCB.toBytes());
        
        DirectoryEntry entryRoot = new DirectoryEntry("root", 2);
        disk.write(offsetDataZone + (1 * blockSize), entryRoot.toBytes());
        userFCB.setSizeUsed(1);
        disk.write(offsetFCB + 63, userFCB.toBytes());
    }
    
    public int freeSlotUsers(){
        int maxUsers = 25;
        for (int i = 0; i < maxUsers; i++ ){
            long offset = superBlock.getUsersStart() + (i * 90);
            byte[] data = disk.read(offset, 90);
            if(data[0] == 0){
                return i;
            }
        }
        return -1;
        
    }
    
    public int freeslotGroups(){
        int maxGroups = 25;
        for (int j = 0; j < maxGroups; j++ ){
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
        for (int i = 0; i < 25; i++ ){
            if (getUser(i).getUserName().equals(userName)) {
                return i;
            }
        }
        return -1;
        
    }    
 
    public int findGroup(String groupName){
        for (int i = 0; i < 25; i++ ){
            if (getGroup(i).getGroupName().equals(groupName)) {
                return i;
            }
        }
        return -1;
        
    }     
    
    public boolean confirmPasswordUser(String password, int index){
        User u = getUser(index);
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
    
    public synchronized void openFile(int fcbIndex, FCB fcb) {
        bitmapOpenFiles.markBusy(fcbIndex);
        fcb.setIsOpen((byte)1);
        writeFCB(fcb, fcbIndex);
        disk.write(superBlock.getBitmapOpenFilesStart(), bitmapOpenFiles.toBytes());
    }

    public synchronized void closeFile(int fcbIndex) {
        bitmapOpenFiles.markFree(fcbIndex);
        FCB fcb = getFCB(fcbIndex);
        fcb.setIsOpen((byte)0);
        writeFCB(fcb, fcbIndex);
        disk.write(superBlock.getBitmapOpenFilesStart(), bitmapOpenFiles.toBytes());
    }

    public byte[] readFileData(FCB fcb){
        long pos = superBlock.getDataZoneStart() + (fcb.getStartBlock() * 512);
        int size = fcb.getBlockCount() * 512;
        return disk.read(pos, size);
    }
    
    public boolean writeFileData(FCB fcb, int fcbIndex, byte[] data){
        int blocksWeNeeded = (int)Math.ceil(data.length / 512.0);
        
        if(blocksWeNeeded <= fcb.getBlockCount()){
            long pos = superBlock.getDataZoneStart() + (fcb.getStartBlock() * 512);
            disk.write(pos, data);
        } else {
            int oldStart = fcb.getStartBlock();
            int oldCount = fcb.getBlockCount();
            // Liberamos los bloques originales para buscar espacio suficente
            bitmapBlocks.freeBlocks(oldStart, oldCount);
            int firstBlock = bitmapBlocks.findFreeConsecutiveBlocks(blocksWeNeeded);
            if (firstBlock == -1) {
                // Restauramos los bloques originales si no hay espaci
                for (int i = 0; i < oldCount; i++) {
                    bitmapBlocks.markBusy(oldStart + i);
                }
                System.out.println("No hay suficiente espacio contiguo");
                return false;
            }
            for (int i = 0; i < blocksWeNeeded; i++){
                bitmapBlocks.markBusy(firstBlock + i);
            }
            long pos = superBlock.getDataZoneStart() + (firstBlock * 512);
            disk.write(pos, data);
            fcb.setStartBlock(firstBlock);
            fcb.setBlockCount(blocksWeNeeded);
            disk.write(superBlock.getBitmapBlocksStart(), bitmapBlocks.toBytes());
        }
        
        fcb.setSizeUsed(data.length);
        writeFCB(fcb, fcbIndex);
        return true;
    }

    
}
