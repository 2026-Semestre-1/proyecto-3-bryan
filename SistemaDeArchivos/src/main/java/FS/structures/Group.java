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
public class Group {

    String groupName;
    int[] members;
    
    public Group(String GroupName, int[] Members){
        this.groupName = GroupName;
        this.members = Members;
    }
    

    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }

    public int[] getMembers() { return members; }
    public void setMembers(int[] members) { this.members = members; }

    public byte[] toBytes(){
        ByteBuffer buffer = ByteBuffer.allocate(15 + 10 * 4); // 55 bytes fijos

        byte[] name = groupName.getBytes();
        byte[] nameBytes = new byte[15];
        System.arraycopy(name, 0, nameBytes, 0, Math.min(name.length, 15));
        buffer.put(nameBytes);

        for (int i = 0; i < 10; i++) {
            if (i < members.length) {
                buffer.putInt(members[i]);
            } else {
                buffer.putInt(-1);
            }
        }

        return buffer.array();
    }

    public static Group fromBytes(byte[] data) {
        ByteBuffer buffer = ByteBuffer.wrap(data);
        byte[] nameBytes = new byte[15];
        buffer.get(nameBytes);
        String groupName = new String(nameBytes).trim();

        int[] members = new int[10];
        for (int i = 0; i < 10; i++) {
            members[i] = buffer.getInt();
        }
        return new Group(groupName, members);
    }   
    
}
