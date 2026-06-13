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
public class User {
    String userName;
    String fullName;
    String password;
    int groupId;
    int homeDirId;
    
    public User(String UserName, String Password){
        this.userName = UserName;
        this.password = Password;
    }
    
    public User(String UserName, String Password, String FullName){
        this.userName = UserName;
        this.password = Password;
        this.fullName = FullName;
    }
    
    public User(String UserName, String Password, String FullName, int GroupID, int HomeDirID){
        this.userName = UserName;
        this.password = Password;
        this.fullName = FullName;
        this.groupId = GroupID;
        this.homeDirId = HomeDirID;
    }    
    
    
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public int getGroupId() { return groupId; }
    public void setGroupId(int groupId) { this.groupId = groupId; }

    public int getHomeDirId() { return homeDirId; }
    public void setHomeDirId(int homeDirId) { this.homeDirId = homeDirId; }    
    
    
    public byte[] toBytes(){
        ByteBuffer buffer = ByteBuffer.allocate(20+ 30 + 32 + 4 + 4);
        
        byte[] name = userName.getBytes();
        byte[] nameBytes = new byte[20];
        System.arraycopy(name, 0,nameBytes, 0, Math.min(name.length , 20));
        buffer.put(nameBytes);
        
        byte[] full = fullName.getBytes();
        byte[] fullBytes = new byte[30];
        System.arraycopy(full, 0, fullBytes, 0, Math.min(full.length, 30));
        buffer.put(fullBytes);
        
        byte[] pass = password.getBytes();
        byte[] passBytes = new byte[32];
        System.arraycopy(pass, 0, passBytes, 0, Math.min(pass.length, 32));
        buffer.put(passBytes);
        
        buffer.putInt(groupId);
        buffer.putInt(homeDirId);
        return buffer.array();
        
    }



    public static User fromBytes(byte[] data) {
        ByteBuffer buffer = ByteBuffer.wrap(data);
        
        byte[] nameBytes = new byte[20];
        buffer.get(nameBytes);
        String userName = new String(nameBytes).trim();

        
        byte[] fullBytes = new byte[30];
        buffer.get(fullBytes);
        String fullName = new String(fullBytes).trim();
        
        byte[] passBytes = new byte[32];
        buffer.get(passBytes);
        String password = new String(passBytes).trim();

        int groupId = buffer.getInt();
        int homeDirId = buffer.getInt();      
        
        User us = new User(userName, password, fullName, groupId, homeDirId);
        return us;
    }
    
  
}
