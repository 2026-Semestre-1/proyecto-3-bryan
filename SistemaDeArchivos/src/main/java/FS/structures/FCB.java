/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package FS.structures;

import java.nio.ByteBuffer;

/**
 *
 * @author bryan
 */
public class FCB {
    String name;            // 20 bytes
    byte type;               // 1 byte (0=archivo, 1=directorio)
    int ownerId;             // 4 bytes
    int groupId;             // 4 bytes
    byte permissions;        // 1 byte
    int sizeUsed;             // 4 bytes
    int startBlock;          // 4 bytes
    int blockCount;          // 4 bytes
    long creationDate;       // 8 bytes
    long modificationDate;   // 8 bytes
    byte isOpen;             // 1 byte  
    
    
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public byte getType() { return type; }
    public void setType(byte type) { this.type = type; }

    public int getOwnerId() { return ownerId; }
    public void setOwnerId(int ownerId) { this.ownerId = ownerId; }

    public int getGroupId() { return groupId; }
    public void setGroupId(int groupId) { this.groupId = groupId; }

    public byte getPermissions() { return permissions; }
    public void setPermissions(byte permissions) { this.permissions = permissions; }

    public int getSizeUsed() { return sizeUsed; }
    public void setSizeUsed(int sizeUsed) { this.sizeUsed = sizeUsed; }

    public int getStartBlock() { return startBlock; }
    public void setStartBlock(int startBlock) { this.startBlock = startBlock; }

    public int getBlockCount() { return blockCount; }
    public void setBlockCount(int blockCount) { this.blockCount = blockCount; }

    public long getCreationDate() { return creationDate; }
    public void setCreationDate(long creationDate) { this.creationDate = creationDate; }

    public long getModificationDate() { return modificationDate; }
    public void setModificationDate(long modificationDate) { this.modificationDate = modificationDate; }

    public byte getIsOpen() { return isOpen; }
    public void setIsOpen(byte isOpen) { this.isOpen = isOpen; }    
 
    public static byte grantPerm(int owner, int group) {
        return (byte) ((owner << 3) | group);
    }

    public static int getOwnerPerm(byte permisos) {
        return (permisos >> 3) & 0b111;
    }

    public static int getGroupPerm(byte permisos) {
        return permisos & 0b111;
    }    
    
    
    public FCB(String Name, byte Type, int OwnerId, int GroupId, byte Permissions, int SizeUsed,
            int StartBlock, int BlockCount, long CreationDate, long ModificationDate, byte IsOpen){
        this.name = Name;
        this.type = Type;
        this.ownerId = OwnerId;
        this.groupId = GroupId;
        this.permissions = Permissions;
        this.sizeUsed = SizeUsed;
        this.startBlock = StartBlock;
        this.blockCount = BlockCount;
        this.creationDate = CreationDate;
        this.modificationDate = ModificationDate;
        this.isOpen = IsOpen;
    }
    
    public byte[] toBytes(){
        ByteBuffer buffer = ByteBuffer.allocate(20+ 1 + 4 + 4 + 1 + 4 + 4+ 4 + 8 + 8 + 1);
        
        byte[] nameData = name.getBytes();
        byte[] nameBytes = new byte[20];
        System.arraycopy(nameData, 0,nameBytes, 0, Math.min(nameData.length , 20));
        buffer.put(nameBytes);
        
        buffer.put(type);
        
        buffer.putInt(ownerId);
        buffer.putInt(groupId);
        buffer.put(permissions);
        buffer.putInt(sizeUsed);
        buffer.putInt(startBlock);
        buffer.putInt(blockCount);
        buffer.putLong(creationDate);
        buffer.putLong(modificationDate);
        buffer.put(isOpen);
        
        return buffer.array();
    }


    public static FCB fromBytes(byte[] data) {
        ByteBuffer buffer = ByteBuffer.wrap(data);
        
        byte[] nameBytes = new byte[20];
        buffer.get(nameBytes);
        String name = new String(nameBytes).trim();

        byte type = buffer.get();
        int ownerID = buffer.getInt();
        int groupId = buffer.getInt();
        byte permissions = buffer.get();
        int sizeUsed = buffer.getInt();
        int startBlock = buffer.getInt();
        int blockCount = buffer.getInt();
        long creationDate = buffer.getLong();
        long modificationDate = buffer.getLong();
        byte isOpen = buffer.get();
             
        
        FCB fc = new FCB(name, type, ownerID, groupId, permissions, sizeUsed, startBlock, blockCount
        , creationDate, modificationDate, isOpen);
        return fc;
    }    
}
 