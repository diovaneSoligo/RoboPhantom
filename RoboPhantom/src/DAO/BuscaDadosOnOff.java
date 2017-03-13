/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package DAO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import modelo.OnOff;
import postgresDB.Conexao;

public class BuscaDadosOnOff {
    
        public Object OnOff(String ID,String IP) {
    
        ArrayList<OnOff> dados = new ArrayList<OnOff>();
		try{
			System.out.println("\nVai Buscar Dados");
			
			Connection c = Conexao.getConexao();//Faz conexão com banco
			
			PreparedStatement stmt =  c.prepareStatement("select *from dispositivos where id_disp = "+ID+" and ip_disp = '"+IP+"'");
			
			ResultSet rs = stmt.executeQuery();

			while(rs.next()){
				OnOff l = new OnOff();
				
				l.setUsuario(rs.getString("usuario"));
                                l.setId_disp(rs.getString("id_disp"));
                                l.setIp_disp(rs.getString("ip_disp"));
                                l.setNome(rs.getString("nome"));
                                l.setEstado(rs.getString("estado"));
                               
				dados.add(l);
			}
			System.out.println("tudo OK ... retornando dados\n");
			c.close();
		}catch(Exception e){
			System.out.println("CATCH em Object OnOff em DAO BuscaDadosOnOff ...");
			e.printStackTrace();
		}
		
		return dados;
}
}
