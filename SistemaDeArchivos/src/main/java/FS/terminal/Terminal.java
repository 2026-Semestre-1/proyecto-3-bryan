/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package FS.terminal;
import FS.structures.*;
import FS.principal.*;
import java.util.Scanner;
/**
 *
 * @author bryan
 */
public class Terminal {
    User currentUser;
    FCB currentDirectory;
    FileSystem fs;
    
    public Terminal(User CurrentUser, FCB CurrentDirectory, FileSystem FS){
        this.currentUser = CurrentUser;
        this.currentDirectory = CurrentDirectory;
        this.fs = FS;
    }
    
    public void start(){
        Scanner scanner = new Scanner(System.in);
        while(true){
            System.out.println(currentUser.getUserName() + "@miFS: ");
            
            String line = scanner.nextLine().trim();
            
            if(line.isEmpty()) continue;
            String[] parts = line.split(" ");
            String command = parts[0].toLowerCase();
            
            executeCommand(command, parts);
            
            
        }
    }
    
    public void executeCommand(String command, String[] parts){
        
    }
    
}
