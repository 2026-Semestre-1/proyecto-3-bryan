package FS.structures;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;


public class Mbr {
    public String fsFileName;
    public long volumeSize; 
    public long volumeStart;
    
    public Mbr (String name, long VolumeSize, long VolumeStart){
        this.fsFileName = name;
        this.volumeSize = VolumeSize;
        this.volumeStart = VolumeStart;
    }
    
    public byte[] toBytes(){

        byte[] fileNameBytes = fsFileName.getBytes();

        ByteBuffer buffer = ByteBuffer.allocate(
                Integer.BYTES + fileNameBytes.length + Long.BYTES + Long.BYTES
        );


        buffer.putInt(fileNameBytes.length);
        buffer.put(fileNameBytes);

        buffer.putLong(volumeSize);

        buffer.putLong(volumeStart);


        return buffer.array();

    }
    
     public static Mbr fromBytesToLong(byte[] data){
         ByteBuffer buffer = ByteBuffer.wrap(data);
         
         int nameLength = buffer.getInt();
         byte[] fileNameBytes = new byte[nameLength];     
         buffer.get(fileNameBytes);
         String fsFileName = new String(fileNameBytes);
         
         long volumeSize = buffer.getLong();
         long volumeStart = buffer.getLong();
         
         return new Mbr(fsFileName, volumeSize, volumeStart);
         
        
    }   
        
}
    
    

