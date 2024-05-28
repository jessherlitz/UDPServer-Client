import socket
import struct
import sys
import signal

def signal_handler(signum, frame):
    print("\nServer shutting down...")
    sys.exit(0)

def main():
    if len(sys.argv) != 2:
        raise ValueError("Parameter(s): <Port>")

    port = int(sys.argv[1])

    print("\nServer running on port: {}".format(port))

    sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    sock.bind(('', port))

    buffer_size = 1024

    while True:
        try:
            buffer, client_address = sock.recvfrom(buffer_size)

            print("\nRequest: "),
            for byte in buffer:
                print("{:02X}".format(ord(byte))),
            print

            if len(buffer) < 13:
                raise ValueError("Buffer is too small to unpack the required header")

            tml, op_code, operand_one, operand_two, request_id, op_name_length = struct.unpack('!BBiiHB', buffer[:13])
            op_name_bytes = buffer[13:13 + op_name_length]

            if len(op_name_bytes) != op_name_length:
                raise ValueError("Buffer is too small to unpack the operation name")

            op_name = op_name_bytes.decode('utf-16')

            print("\nRequest ID: {}".format(request_id))
            print("Operation: {}".format(op_name))
            print("Operand 1: {}".format(operand_one))
            print("Operand 2: {}".format(operand_two))

            result = 0
            error = False

            if op_code == 0:
                result = operand_one + operand_two
            elif op_code == 1:
                result = operand_one - operand_two
            elif op_code == 2:
                result = operand_one | operand_two
            elif op_code == 3:
                result = operand_one & operand_two
            elif op_code == 4:
                if operand_two == 0:
                    error = True
                else:
                    result = operand_one // operand_two
            elif op_code == 5:
                result = operand_one * operand_two
            else:
                error = True

            if len(buffer) != tml:
                error = True

            error_code = 127 if error else 0

            response_bytes = struct.pack('!BIHB', 8, result, error_code, request_id)
            sock.sendto(response_bytes, client_address)

            print("\nResponse: "),
            for byte in response_bytes:
                print("{:02X}".format(ord(byte))),
            print

            print("\nResponse ID: {}".format(request_id))
            print("Result: {}".format(result))
            print("Error Code: {}".format('Error' if error else 'OK'))

        except struct.error as e:
            print >> sys.stderr, "Struct error occurred: {}".format(e)
        except socket.error as e:
            print >> sys.stderr, "Socket error occurred: {}".format(e)
        except Exception as e:
            print >> sys.stderr, "Unexpected error occurred: {}".format(e)

if __name__ == "__main__":
    signal.signal(signal.SIGINT, signal_handler)
    main()
