/*Diovane Soligo - 02/2017 - Servidor de recebimento de dados da tomada inteligente */
package RoboPhantom;

import DAO.BuscaDadosOnOff;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import modelo.OnOff;
import postgresDB.Conexao;

public class RoboPhantom {
/******************************************************************************/    
    static class ArmazenaDadosPhantom implements Runnable{
        
        Socket cliente;

        public ArmazenaDadosPhantom(Socket cliente) {
            this.cliente = cliente;
        }
       
        @Override
        public void run() {
            try {
                String ClienteIP = cliente.getInetAddress().getHostAddress().toString();//pega o IP do cliente (tomada)
                
                byte[] msg = new byte[1024];//cria buffer
                int tamanho = cliente.getInputStream().read(msg);//armazena o que vem do cliente no buffer msg, e o tamanho na variavel tamanho
                
                String msgString = new String (msg,"UTF-8");
                
                //System.out.println("\nTAMANHO: "+tamanho);//mostra o tamanho da mensagem vinda do cliente
                //System.out.println("\n\nmsgString: "+msgString);//mostra toda mensagem GET vinda do cliente
                //System.out.println("Cliente IP: "+ClienteIP);
                
                if(tamanho>0){
                        String[] msgLink = new String[tamanho]; //cria um array com o tamanho da mensagem do cliente
                        msgLink = msgString.split(" "); //quebra onde tiver espaço
                        msgString = msgLink[1]; //armazena /?ID=01&VOLTS=....
                        //http://localhost:8080/?ID=1@1&VOLTS=219&CORRENTE=0,1123

                        char comp = '?';
                        char comp2= msgString.charAt(1);

                        if(comp == comp2){//se tiver o caracter '?' executa
                               System.out.println("Tomada conectada...");
                               System.out.println("TomadaIP IP: "+ClienteIP+" ...Coletando e armazenando dados..");
                               //System.out.println("Cliente Dados: "+msgLink[1]);
                               
                               String[] dados = new String[3];
                               dados = msgString.split("&");
                               //System.out.println("dados: "+dados[0]);
                               //System.out.println("dados: "+dados[1]);
                               //System.out.println("dados: "+dados[2]);
                               
                               String[] aux = new String[2];
                               String ID,V,A;
                               
                               aux = dados[0].split("=");
                               ID = aux[1];
                               
                               aux = dados[1].split("=");
                               V = aux[1];
                             
                               aux = dados[2].split("=");
                               A = aux[1];
                               
                               System.out.println("Tomada ID: "+ID+"\nVolts: "+V+" volts\nCorrente: "+A+" amperes\n");
                               
                               //armazena dados
                               ArmazenaDadosSGBD armazena = new ArmazenaDadosSGBD(ID, V, A);
                               new Thread(armazena).start();
                               
                               //verifica dispositivo se on ou off
                               Busca b = new Busca(ClienteIP,ID);
                               new Thread(b).start();
                               
                               
                        }else{
                               System.out.println("\n\n");
                        }
                 }  
                cliente.getOutputStream().write("<html><head><title>Phantom</title></head><body></body></html>".getBytes("ISO-8859-1"));//retorna algo ao cliente
                
                cliente.close(); //encerra conexão
                System.out.println("\n");
            } catch (IOException ex) {
                Logger.getLogger(RoboPhantom.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
/******************************************************************************/  
/******************************************************************************/     
    static class Busca implements Runnable {
        String IP,ID;

        private Busca(String ClienteIP, String ID) {
            this.ID = ID;
            this.IP = ClienteIP;
        }
        
        @Override
        public void run() {
            BuscaDadosOnOff X = new BuscaDadosOnOff();
            
            ArrayList<OnOff> Y = new ArrayList<OnOff>();
            
            Y = (ArrayList<OnOff>) X.OnOff(ID,IP);
            
            int tamanho = 0;
            while(tamanho < Y.size()){
                System.out.println("IP: "+Y.get(tamanho).getIp_disp()+" com: "+Y.get(tamanho).getEstado());
                Executa exec = new Executa(Y.get(tamanho).getIp_disp(),Y.get(tamanho).getEstado());
                new Thread(exec).start();
                tamanho++;
            }
            System.out.println("...ENCERRA THREAD Busca...");
            
        }
    }
/******************************************************************************/
/******************************************************************************/    
   static class Executa implements Runnable{
       
       String IP;
       String estado;
       
       public Executa (String IP,String estado){
       this.IP = IP;
       this.estado = estado;
       }
       
        @Override
        public void run() {
            String stringURL = "http://"+IP+":1000/?acao=00"+estado+"";
            System.out.println("IP DE CONEXAO: "+stringURL);
        String resposta = "";
        try {
            URL url = new URL(stringURL);
            URLConnection connection = url.openConnection();
            
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(
                    connection.getInputStream()));
            
            String inputLine;
            StringBuffer sb = new StringBuffer();
            
            while ((inputLine = in.readLine()) != null) sb.append(inputLine);
            resposta = sb.toString();
            
            System.out.println("--> "+resposta);
            
            in.close();
            
            System.out.println("...ENCERRA THREAD Executa...");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        }
        
   }    
/******************************************************************************/  
/******************************************************************************/    
    static class ArmazenaDadosSGBD implements Runnable{

        String ID,V,A;
        
        public ArmazenaDadosSGBD(String ID,String V,String A){
            
            this.ID = ID;
            this.V = V;
            this.A = A;
        }
        
        @Override
        public void run() {
            try {
                Connection c = null;
                
                
                try{
                    PreparedStatement stmt = null;
                    c = Conexao.getConexao();
                    
                    int volts = Integer.parseInt(V);
                    double C = Double.parseDouble(A);
                    int id = Integer.parseInt(ID);
                    double W = volts*C;
                    String data = "current_date";
                    String hora = "current_time";
                    String data_hora = "current_timestamp(0)";
                    
                    String sql = "insert into dados(id_disp,voltagem,corrente,potencia,data,hora,data_hora) "
                            + "values ("+ID+","+V+","+C+","+W+","+data+","+hora+","+data_hora+");";
                    stmt = c.prepareStatement(sql);
                    
                    stmt.execute();
                    
                    stmt.close();

                    c.close();
                    
                }catch(Exception e){
                    e.printStackTrace();
                }
                
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
/******************************************************************************/   
/******************************************************************************/    
    public static void main(String[] args) throws IOException, InterruptedException {
        ServerSocket servidor = new ServerSocket(8080);
        
        while(true){
            Socket cliente = servidor.accept();
            ArmazenaDadosPhantom Phanton = new ArmazenaDadosPhantom(cliente);
            new Thread(Phanton).start();
        }
    }
/******************************************************************************/    
}
