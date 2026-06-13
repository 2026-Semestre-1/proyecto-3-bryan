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
    
    public void createDisk(long file, String type ,String fileName){
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
        mbr.toBytes();
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
        
    }
}
