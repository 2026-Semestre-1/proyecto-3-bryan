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
public class DirectoryEntry {
    String name;
    int fcbId;
    
    public DirectoryEntry(String Name, int FcbID){
        this.name = Name;
        this.fcbId = FcbID;
    }
    
    

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getFcbId() { return fcbId; }
    public void setFcbId(int fcbId) { this.fcbId = fcbId; }

    
    
    public byte[] toBytes(){
        ByteBuffer buffer = ByteBuffer.allocate(20 + 4);
        
        byte[] direcName = name.getBytes();
        byte[] nameBytes = new byte[20];
        System.arraycopy(direcName, 0,nameBytes, 0, Math.min(direcName.length , 20));
        buffer.put(nameBytes);
        buffer.putInt(fcbId);
        return buffer.array();
        
    }

    public static DirectoryEntry fromBytes(byte[] data) {
        ByteBuffer buffer = ByteBuffer.wrap(data);
        
        byte[] nameBytes = new byte[20];
        buffer.get(nameBytes);
        String userName = new String(nameBytes).trim();
        int fcbId = buffer.getInt();
        
        DirectoryEntry direcUs = new DirectoryEntry(userName, fcbId);
        return direcUs;
    }    
    
    
}
