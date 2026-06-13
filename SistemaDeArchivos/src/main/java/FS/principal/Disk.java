package FS.principal;

import java.io.RandomAccessFile;

public class Disk{
    public RandomAccessFile file;
    
    public Disk(String path) {

        try {

            file = new RandomAccessFile(path, "rw");

        } catch(Exception e){

            e.printStackTrace();

        }

    }


    public void write(long position, byte[] data){

        try {

            file.seek(position);
            file.write(data);

        } catch(Exception e){

            e.printStackTrace();

        }

    }


    public byte[] read(long position, int size){

        byte[] data = new byte[size];

        try {

            file.seek(position);
            file.read(data);

        } catch(Exception e){

            e.printStackTrace();

        }

        return data;

    }
}