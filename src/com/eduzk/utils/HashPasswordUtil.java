package com.eduzk.utils;

import org.mindrot.jbcrypt.BCrypt;
import java.util.Scanner;

public class HashPasswordUtil {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter password to hash: ");
        String plainPassword = scanner.nextLine();
        String hashedPassword = BCrypt.hashpw(plainPassword, BCrypt.gensalt(12));
        System.out.println("Hashed password: " + hashedPassword);
        scanner.close();
    }
}
