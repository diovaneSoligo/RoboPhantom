/*Diovane Soligo - 02/2017 - Servidor de recebimento de dados da tomada inteligente */
package postgresDB;

import java.sql.Connection;
import java.sql.DriverManager;

public class Conexao {
    public static void main(String args[]){
        Conexao.getConexao();
        System.out.println("Conexão Aberta");
    }

    public static Connection getConexao() {
       Connection c = null;
       //System.out.println("Conectando com o BD...");
       
       try{
           Class.forName("org.postgresql.Driver");
           
           String url ="jdbc:postgresql://localhost:5433/dadosTomadaIoT"; //se necessario alterar database
           String user ="postgres";
           String password ="1234";
           
           c = DriverManager.getConnection(url, user, password);
           //System.out.println("BD Conectado...");
       }catch(Exception e){
           e.printStackTrace();
       }

       return c;
    }
}