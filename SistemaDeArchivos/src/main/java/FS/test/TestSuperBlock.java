package FS.test;

import FS.structures.SuperBlock;

public class TestSuperBlock {

    public static void main(String[] args) {

        // Creamos y suponemos solo es para ver si devuevlo l omismo
        SuperBlock sb1 = new SuperBlock(1000000, 512);

        sb1.setBitmapBlocksStart(1024);
        sb1.setBitmapOpenFilesStart(2048);
        sb1.setUsersStart(4096);
        sb1.setGroupsStart(8192);
        sb1.setFcbStart(12000);
        sb1.setDataZoneStart(50000);
        sb1.setRootReference(12000);


        byte[] data = sb1.toBytes();
        SuperBlock sb2 = SuperBlock.fromBytes(data);


        // Comparamos
        System.out.println("Original:");
        System.out.println(sb1.totalDiskSize);
        System.out.println(sb1.blockSize);
        System.out.println(sb1.totalBlocks);
        System.out.println(sb1.bitmapBlocksStart);
        System.out.println(sb1.bitmapOpenFilesStart);        
        System.out.println(sb1.usersStart);
        System.out.println(sb1.groupsStart);
        System.out.println(sb1.fcbStart);
        System.out.println(sb1.dataZoneStart);
        System.out.println(sb1.rootReference);
        

        System.out.println("\nRecuperado:");
        System.out.println(sb2.totalDiskSize);
        System.out.println(sb2.blockSize);
        System.out.println(sb2.totalBlocks);
        System.out.println(sb2.bitmapBlocksStart);
        System.out.println(sb2.bitmapOpenFilesStart);        
        System.out.println(sb2.usersStart);
        System.out.println(sb2.groupsStart);
        System.out.println(sb2.fcbStart);
        System.out.println(sb2.dataZoneStart);
        System.out.println(sb2.rootReference);


        System.out.println("\nBytes del SuperBlock:");
        System.out.println(data.length);

    }
}
