import java.net.*;
import java.io.*;
import java.util.Scanner;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.InputMismatchException;

public class MyUDPClient {

  public static void main(String args[]) throws Exception {

      if (args.length != 2) {
         throw new IllegalArgumentException("Parameter(s): <Server>" +
         " <Port> ");
      }       

      DatagramSocket sock = new DatagramSocket(); 

      InetAddress destAddr = InetAddress.getByName(args[0]); 
      int destPort = Integer.parseInt(args[1]);
      
      Scanner scanner = new Scanner(System.in);
      short requestId = 1;
      ArrayList<Long> dataCollection = new ArrayList<>();

      while (true) {
        try {
            System.out.print("\nEnter OpCode (0 for +, 1 for -, 2 for |, 3 for &, 4 for /, 5 for *): ");
            byte opCode = scanner.nextByte();

            System.out.print("Enter Operand 1: ");
            int operand1 = scanner.nextInt();

            System.out.print("Enter Operand 2: ");
            int operand2 = scanner.nextInt();
            
            scanner.nextLine(); 

            String opName;

            switch (opCode) {
                case 0:
                    opName = "addition";
                    break;
                case 1:
                    opName = "subtraction";
                    break;
                case 2:
                    opName = "or";
                    break;
                case 3:
                    opName = "and";
                    break;
                case 4:
                    opName = "division";
                    break;
                case 5:
                    opName = "multiplication";
                    break;
                default:
                    System.out.println("Invalid OpCode");
                    continue;
            }

            byte[] opNameBytes = opName.getBytes(StandardCharsets.UTF_16);
            byte opNameLength = (byte) opNameBytes.length;
            byte tml = (byte) (13 + opNameLength);

            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(buf);

            out.writeByte(tml);
            out.writeByte(opCode);
            out.writeInt(operand1);
            out.writeInt(operand2);
            out.writeShort(requestId);
            out.writeByte(opNameLength);
            out.write(opNameBytes);
            out.flush();

            byte[] requestBytes = buf.toByteArray();
            
            DatagramPacket message = new DatagramPacket(requestBytes, requestBytes.length, 
                    destAddr, destPort);

            long startTime = System.currentTimeMillis();

            sock.send(message);


            System.out.printf("\nRequest: ");
            for (byte b : requestBytes) {
                System.out.printf("%02X ", b);
            }
            System.out.println();


          byte[] responseBuffer = new byte[1024];

          DatagramPacket responsePacket = new DatagramPacket(responseBuffer, responseBuffer.length);
          sock.receive(responsePacket);

          long endTime = System.currentTimeMillis();
          long rtt = endTime - startTime;

          dataCollection.add(rtt);

          ByteBuffer responseByteBuffer = ByteBuffer.wrap(responseBuffer, 0, responsePacket.getLength());

          System.out.printf("\nResponse: ");
          for (int i = 0; i < responsePacket.getLength(); i++) {
            System.out.printf("%02X ", responseBuffer[i]);
          }
          System.out.println();

          byte responseTML = responseByteBuffer.get();
          int result = responseByteBuffer.getInt();
          byte errorCode = responseByteBuffer.get();
          short responseRequestId = responseByteBuffer.getShort();

          System.out.println("\nResponse ID: " + responseRequestId);
          System.out.println("Result: " + result);
          System.out.println("Error Code: " + (errorCode == 0 ? "OK" : "Error" + errorCode));

          System.out.println("\nRound trip time: " + rtt + " ms");

          long minRTT = dataCollection.stream().min(Long::compare).orElse(0L);
          long maxRTT = dataCollection.stream().max(Long::compare).orElse(0L);
          double averageRTT = dataCollection.stream().mapToLong(Long::longValue).average().orElse(0.0);

          System.out.println("\nMin RTT: " + minRTT + " ms");
          System.out.println("Max RTT: " + maxRTT + " ms");
          System.out.println("Average RTT: " + averageRTT + " ms");

          requestId++;

          System.out.print("\nDo you want to send another request? (yes/no): ");

          String answer = scanner.nextLine();

          if (!answer.equalsIgnoreCase("yes")) {
            break;
          }
        } catch (InputMismatchException e) {
          System.out.println("Invalid input. Please enter valid numeric values.");
          scanner.nextLine(); 
        } catch (Exception e) {
          System.out.println("An error occurred: " + e.getMessage());
        }
      }

       scanner.close();
       sock.close();
  }
}




























