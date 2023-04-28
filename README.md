# SegurancaConfiabilidade2023_Proj1
2022/2023
Trabalho 1 – 1ª fase

Grupo SegC-002

56320 - Renato Custódio
57100 - Bruno Soares
41972 - Marcio Moreira

1. Compilacaoo do trabalho e Execução
	1.1 Consola
		1.1.1. Compilar o Servidor: "javac ./server_side/TintolMarketServer.java";
		1.1.2. Compilar o Cliente: "javac ./client_side/TintolMarket.java";

		1.1.3. Criar um ficheiro manifest (caso não exista um) especificando o ponto de entrada da aplicação, para cada um dos métodos main do projeto. o ficheiro deve ser chamado MANIFEST.MF, e deve ser colocado no mesmo diretório dos ficheiros .class. O manifest deve ter o seguinte aspeto:
		"Main-Class: com.example.YourMainClass"
		1.1.4. Executar jar cfm YourJarName.jar MANIFEST.MF *.class
		
		1.1.5. Executar o Servidor: "java ./server_side/TintolMarketServer <port> <password-cifra> <keystore> <password-keystore>";
		1.1.6. Executar o Cliente: "java ./client_side/TintolMarket <serverAddress> <truststore> <keystore> <password-keystore> <userID>";
		OU "java ./client_side/TintolMarket 127.0.0.1:<port> <username>";
		
		Correr o JAR
		Servidor:  java -jar TintolMarketServer.jar <port> <password-cifra> <keystore> <password-keystore>
		Cliente: java -jar TintolMarket.jar <serverAddress> <truststore> <keystore> <password-keystore> <userID>


2. Limitacoes
	O trabalho foi desenvolvido e testado em SO's Windows e macOS, nao tendo havido qualquer problema relativamente 
	 as "/" utilizado para os caminhos de directorias.
	

3. Padrão
   A password de todas as keystores é "123456" por motivos de conveniência para a equipa de desenvolvimento, e para os docentes.
