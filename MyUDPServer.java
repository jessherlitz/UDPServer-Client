import java.net.*;  
import java.io.*;
import java.util.Arrays;
import java.nio.charset.StandardCharsets;

public class MyUDPServer {

  public static void main(String[] args) throws Exception {

      if (args.length != 1)     
	        throw new IllegalArgumentException("Parameter(s): <Port>");

      int port = Integer.parseInt(args[0]);   

      System.out.println("\nServer running on port: " + port);

      byte[] buffer = new byte[1024];
      DatagramSocket sock = new DatagramSocket(port);

      while (true) {
        try {
          DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
          sock.receive(packet);
          
          System.out.printf("\nRequest: ");
          for (int i = 0; i < packet.getLength(); i++) {
              System.out.printf("%02X ", buffer[i]);
          }
          System.out.println();

          ByteArrayInputStream payload = 
            new ByteArrayInputStream(packet.getData(), packet.getOffset(), packet.getLength());

          DataInputStream src = new DataInputStream(payload);

          byte tml = src.readByte();
          byte opCode = src.readByte();
          int operandOne = src.readInt();
          int operandTwo = src.readInt();
          short requestId = src.readShort();
          byte opNameLength = src.readByte();
          byte[] opNameBytes = new byte[opNameLength];
          src.readFully(opNameBytes);

          String opName = new String(opNameBytes, StandardCharsets.UTF_16);

          System.out.println("\nRequest ID: " + requestId);
          System.out.println("Operation: " + opName);
          System.out.println("Operand 1: " + operandOne);
          System.out.println("Operand 2: " + operandTwo);

          int result = 0;
          boolean error = false;

          switch (opCode) {
            case 0: result = operandOne + operandTwo; break;
            case 1: result = operandOne - operandTwo; break;
            case 2: result = operandOne | operandTwo; break;
            case 3: result = operandOne & operandTwo; break;
            case 4:
            if (operandTwo == 0) {
                error = true;
            } else {
                result = operandOne / operandTwo;
            }
            break;
            case 5: result = operandOne * operandTwo; break;
            default: error = true; break;
          }

          if (packet.getLength() != tml) {
            error = true;
          }

          byte errorCode = (byte) (error ? 127 : 0);

          ByteArrayOutputStream buf = new ByteArrayOutputStream();
          DataOutputStream out = new DataOutputStream(buf);

          out.writeByte(8);
          out.writeInt(result);
          out.writeByte(errorCode);
          out.writeShort(requestId);
          out.flush();

          byte[] requestBytes = buf.toByteArray();

          DatagramPacket response = 
            new DatagramPacket(requestBytes, requestBytes.length, packet.getAddress(), packet.getPort());

          sock.send(response);

          System.out.printf("\nResponse: ");
          for (byte b : requestBytes) {
            System.out.printf("%02X ", b);
          }
          System.out.println();

          System.out.println("\nResponse ID: " + requestId);
          System.out.println("Result: " + result);
          System.out.println("Error Code: " + (error ? "Error" : "OK"));
      
          } catch (IOException e) {
              System.err.println("I/O error occurred: " + e.getMessage());
          } catch (Exception e) {
              System.err.println("Unexpected error occurred: " + e.getMessage());
          }
      }     
  }
}
