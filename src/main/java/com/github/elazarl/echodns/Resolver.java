package com.github.elazarl.echodns;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Resolver performs DNS resolution and reverse DNS resolution of command line
 * arguments.
 * Used to test the StaticDnsResolver class.
 *
 * Typical usage:
 * {@code
 * java -Dsun.net.spi.nameservice.provider.1=dns,echodns -jar echodns.jar 1.1.1.1
 * }
 */
public class Resolver {

    public static void main(String[] args) {
        for (String arg : args) {
            try {
                InetAddress address = InetAddress.getByName(arg);
                String hostname = address.getCanonicalHostName();
                if (hostname.equals(address.getHostAddress())) {
                    hostname = address.getHostName() + "[!c]";
                }
                System.out.println(arg + ": " + hostname + "/" + address.getHostAddress());
            } catch (UnknownHostException e) {
                System.out.println("Unknown host: " + arg);
            }
        }
    }
}
