/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package grupo03;

import java.sql.Connection;
import java.sql.DriverManager;
import javax.swing.JOptionPane;

/**
 *
 * @author Administrador
 */
public class Conexion {
    public Connection conectar(){
        Connection cn=null;
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            cn=DriverManager.getConnection("jdbc:mysql://localhost:3307/fisei_asistencia","root","");
            // JOptionPane.showMessageDialog(null, "conectado");
        }catch(Exception ex){
            JOptionPane.showMessageDialog(null, "Error conexion: " + ex);
        }
         return cn;   
    }
    
}
