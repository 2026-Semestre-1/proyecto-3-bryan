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
        long sizeSuperBlock = 80;
        long sizeMBR = 200;
        long  sizeBitmapOpenFiles = 10; // Serian 80 archivos porque 1 byte son 8 bits
        long sizeUsuarios = 900;
        long sizeGrupos = 550;
        long sizeFCB = 5900;
        long total = sizeSuperBlock + sizeMBR + sizeBitmapOpenFiles + sizeUsuarios + sizeGrupos + sizeFCB;
        long remainingSpace = sizeBytes / total;
        
        
        superBlock.toBytes();
        Disk disk = new Disk(fileName);
        disk.write(0, mbr.toBytes());
        
        
        disk.write(blockSize, superBlock.toBytes());
        
    }
}
