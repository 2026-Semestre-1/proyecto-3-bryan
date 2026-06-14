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
    
    public FileSystem(){
        
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
        long sizeFCB = 5900;            // 59 bytes × 100 FCBs
        
        

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
        (byte) 0
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
        (byte) 0
        );        
        disk.write(offsetFCB + (1 * 59), userFCB.toBytes());
        

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
            (byte) 0
        );
        disk.write(offsetFCB + (2 * 59), homeRootFCB.toBytes());    
        disk.write(offsetBitmapBloques, bitmapBlocks.toBytes());
        
        // Creamos usuario root
        User rootUser = new User("root", rootPassword, "root", 0, 2);
        disk.write(offsetUsuarios, rootUser.toBytes());
        

        DirectoryEntry entryUser = new DirectoryEntry("user",1);
        DirectoryEntry entryRoot = new DirectoryEntry("root",2);
        disk.write(offsetDataZone + (0 * blockSize), entryUser.toBytes());
        disk.write(offsetDataZone + (1 * blockSize), entryRoot.toBytes());
        

    }
    
    
}
