/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package FS.structures;
import java.util.BitSet;
/**
 *
 * @author bryan
 */
public class Bitmap {
    public BitSet bitmap;
    public int size;
    
    public Bitmap(int size){
        this.size = size;
        bitmap = new BitSet(size);
    }
    

    public void markBusy(int index){
        bitmap.set(index);
    }
    
    public void markFree(int index){
        bitmap.set(index, false);
    }
    
    public boolean isBusy(int index){
        return bitmap.get(index) == true;
    }
    
    public int findFreeBit(){
        int i = bitmap.nextClearBit(0);
        return i;
    }
    
    public byte[] toBytes(){
        int bytesSize = (int)Math.ceil(size / 8.0);
        byte[] result = new byte[bytesSize];
        byte[] realData = bitmap.toByteArray();
        
        System.arraycopy(realData, 0, result, 0, realData.length);
        return result;
        
    }
    
    public static Bitmap fromBytes(byte[] data){
        Bitmap b = new Bitmap(data.length * 8);
        b.bitmap = BitSet.valueOf(data);
        return b;
        
    }
    
    public int countUsedBlocks() {
        return bitmap.cardinality();
    }

    public int findFreeConsecutiveBlocks(int needed) {
        int total = bitmap.length(); 
        for (int i = 0; i <= total - needed; i++) {
            boolean free = true;
            for (int j = 0; j < needed; j++) {
                if (bitmap.get(i + j)) {
                    free = false;
                    break;
                }
            }
            if (free) {
                return i; 
            }
        }
        return -1; 
    }
    
    
}
