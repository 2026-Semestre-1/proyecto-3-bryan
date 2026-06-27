package FS.structures;

import java.nio.ByteBuffer;


public class SuperBlock{
    public long totalDiskSize;
    public long blockSize;
    public long totalBlocks;
    
    public long bitmapBlocksStart;
    public long bitmapOpenFilesStart;
    
    public long usersStart;
    public long groupsStart;
    
    public long fcbStart;
    
    public long dataZoneStart;
    public long rootReference;
    
    
    public SuperBlock(long TotalDiskSize, long BlockSize){
        this.totalDiskSize = TotalDiskSize;
        this.blockSize = BlockSize;
        this.totalBlocks = TotalDiskSize/BlockSize;
    }
    
    public SuperBlock(long TotalDiskSize, long BlockSize, long TotalBlocks, long BitmapBlocksStart,
        long BitmapOpenFilesStart, long UsersStart, long GroupsStart, long FcbStart,
        long DataZoneStart, long RootReference){
        this.totalDiskSize = TotalDiskSize;
        this.blockSize = BlockSize;
        this.totalBlocks = TotalBlocks;
        this.bitmapBlocksStart = BitmapBlocksStart;
        this.bitmapOpenFilesStart = BitmapOpenFilesStart;
        this.usersStart = UsersStart;
        this.groupsStart = GroupsStart;
        this.fcbStart = FcbStart;
        this.dataZoneStart = DataZoneStart;
        this.rootReference = RootReference;
    }    
 
    public long getTotalDiskSize() { return totalDiskSize; }
    public long getBlockSize() { return blockSize; }
    public long getTotalBlocks() { return totalBlocks; }
    public long getBitmapBlocksStart() { return bitmapBlocksStart; }
    public long getBitmapOpenFilesStart() { return bitmapOpenFilesStart; }
    public long getUsersStart() { return usersStart; }
    public long getGroupsStart() { return groupsStart; }
    public long getFcbStart() { return fcbStart; }
    public long getDataZoneStart() { return dataZoneStart; }
    public long getRootReference() { return rootReference; }

    public void setTotalDiskSize(long disk){
        totalDiskSize = disk;
    }
    
    public void setBlockSize(long block){
        blockSize = block;
    }    
    
    public void setTotalBlocks(long block){
        totalBlocks = block;
    }    
        
    public void setBitmapBlocksStart(long direc){
        bitmapBlocksStart = direc;
    }
    
    public void setBitmapOpenFilesStart(long direc){
        bitmapOpenFilesStart = direc;
    }


    public void setUsersStart(long user){
        usersStart = user;
    }

    public void setGroupsStart(long group){
        groupsStart = group;
    } 
    
    
    public void setFcbStart(long fcb){
        fcbStart = fcb;
    }


    public void setDataZoneStart(long data){
        dataZoneStart = data;
    }   
    
    public void setRootReference(long root){
        rootReference = root;
    }     
    
    
    public byte[] toBytes(){


        ByteBuffer buffer = ByteBuffer.allocate(               
                Long.BYTES + Long.BYTES +  Long.BYTES + Long.BYTES + Long.BYTES + Long.BYTES +
                Long.BYTES + Long.BYTES + Long.BYTES + Long.BYTES + Long.BYTES
        );

        buffer.putLong(totalDiskSize);
        buffer.putLong(blockSize);
        buffer.putLong(totalBlocks);
        buffer.putLong(bitmapBlocksStart); 
        buffer.putLong(bitmapOpenFilesStart);
        buffer.putLong(usersStart);
        buffer.putLong(groupsStart);
        buffer.putLong(fcbStart);
        buffer.putLong(dataZoneStart);
        buffer.putLong(rootReference);


        return buffer.array();

    } 
    
     public static SuperBlock fromBytes(byte[] data){
         ByteBuffer buffer = ByteBuffer.wrap(data);
    
         
         long totalDiskSize = buffer.getLong();
         long blockSize = buffer.getLong();
         long totalBlocks = buffer.getLong();
         long bitmapBlocksStart = buffer.getLong();
         long bitmapOpenFilesStart = buffer.getLong();
         long usersStart = buffer.getLong();
         long groupsStart = buffer.getLong();
         long fcbStart = buffer.getLong();     
         long dataZoneStart = buffer.getLong();
         long rootReference = buffer.getLong();           
         
         return new SuperBlock(totalDiskSize, blockSize, totalBlocks, bitmapBlocksStart, bitmapOpenFilesStart,
            usersStart, groupsStart, fcbStart, dataZoneStart, rootReference);
         
        
    }       
    
    
    
   
}